#!/usr/bin/env bash
# DEV HELPER: logs in through the Auth Server's login form and prints an ACCESS TOKEN.
# Usage: ./scripts/get-token.sh <username> <password> ["space separated scopes"]
# Requires: curl, openssl, jq. Not for production use.
set -euo pipefail

USERNAME=${1:?username required}
PASSWORD=${2:?password required}
SCOPE=${3:-"openid profile shop.read shop.write"}
AUTH=http://localhost:9000
REDIRECT=http://localhost:3000/callback
JAR=$(mktemp)                      # cookie jar: keeps the login session between calls
trap 'rm -f "$JAR"' EXIT

# PKCE: a random secret (verifier) and its SHA-256 hash (challenge)
VERIFIER=$(openssl rand -base64 48 | tr -d '=+/' | cut -c1-64)
CHALLENGE=$(printf '%s' "$VERIFIER" | openssl dgst -sha256 -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')
ENC_SCOPE=${SCOPE// /%20}

# 1. Start the flow. We are not logged in, so the server redirects to /login and sets a session cookie.
curl -s -H 'Accept: text/html' -c "$JAR" -b "$JAR" -o /dev/null \
  "$AUTH/oauth2/authorize?response_type=code&client_id=shopsphere-web&redirect_uri=$REDIRECT&scope=$ENC_SCOPE&code_challenge=$CHALLENGE&code_challenge_method=S256&state=xyz"

# 2. Read the CSRF token out of the login form.
CSRF=$(curl -s -c "$JAR" -b "$JAR" "$AUTH/login" \
  | grep -o 'name="_csrf"[^>]*value="[^"]*"' | sed 's/.*value="//; s/"$//')

# 3. Submit the credentials; on success the server redirects back to the saved authorize request.
NEXT=$(curl -s -c "$JAR" -b "$JAR" -o /dev/null -w '%{redirect_url}' \
  --data-urlencode "username=$USERNAME" --data-urlencode "password=$PASSWORD" \
  --data-urlencode "_csrf=$CSRF" "$AUTH/login")

# 4. Follow it: the server answers with a redirect to the callback URL containing the code.
CALLBACK=$(curl -s -H 'Accept: text/html' -c "$JAR" -b "$JAR" -o /dev/null -w '%{redirect_url}' "$NEXT")
CODE=$(printf '%s' "$CALLBACK" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
[ -n "$CODE" ] || { echo "Login failed (wrong credentials or unexpected redirect: $NEXT)" >&2; exit 1; }

# 5. Exchange the code plus the PKCE verifier for tokens; print only the access token.
curl -s -X POST "$AUTH/oauth2/token" \
  -d grant_type=authorization_code -d client_id=shopsphere-web \
  --data-urlencode "redirect_uri=$REDIRECT" -d code="$CODE" -d code_verifier="$VERIFIER" \
  | jq -r '.access_token'