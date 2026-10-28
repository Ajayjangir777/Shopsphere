package com.shopsphere.user.service;

import com.shopsphere.common.web.ApiException;
import com.shopsphere.user.Address;
import com.shopsphere.user.AddressRepository;
import com.shopsphere.user.Dto.AddressRequest;
import com.shopsphere.user.Dto.AddressResponse;
import com.shopsphere.user.Dto.ProfileRequest;
import com.shopsphere.user.Dto.ProfileResponse;
import com.shopsphere.user.UserProfile;
import com.shopsphere.user.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
/** Business logic for the caller's own profile and addresses. The owner is always passed in from the token. */
@Service
@RequiredArgsConstructor
public class UserService {

    /** Upper bound on saved addresses per user, to limit abuse. */
    private static final int MAX_ADDRESSES = 10;

    private final UserProfileRepository profileRepository;
    private final AddressRepository addressRepository;

    /** Returns the caller's profile, or 404 if they have not created one yet. */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String ownerSub) {
        return profileRepository.findByOwnerSub(ownerSub).map(ProfileResponse::from)
                .orElseThrow(() -> ApiException.notFound("Profile not created yet"));
    }

    /** Creates or updates the caller's profile (an idempotent "upsert"). */
    @Transactional
    public ProfileResponse saveProfile(String ownerSub, ProfileRequest request) {
        UserProfile profile = profileRepository.findByOwnerSub(ownerSub)
                .orElseGet(() -> UserProfile.builder().ownerSub(ownerSub).build());
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        return ProfileResponse.from(profileRepository.save(profile));
    }

    /** Lists the caller's addresses. */
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(String ownerSub) {
        return addressRepository.findByOwnerSubOrderByCreatedAtAsc(ownerSub).stream()
                .map(AddressResponse::from).toList();
    }

    /** Returns one of the caller's addresses; another user's id gives the same 404 as a missing one. */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(String ownerSub, UUID id) {
        return addressRepository.findByIdAndOwnerSub(id, ownerSub).map(AddressResponse::from)
                .orElseThrow(() -> ApiException.notFound("Address not found: " + id));
    }

    /** Saves a new address for the caller, up to the per-user limit. */
    @Transactional
    public AddressResponse addAddress(String ownerSub, AddressRequest r) {
        if (addressRepository.countByOwnerSub(ownerSub) >= MAX_ADDRESSES) {
            throw ApiException.conflict("Address limit reached (" + MAX_ADDRESSES + ")");
        }
        Address saved = addressRepository.save(Address.builder()
                .ownerSub(ownerSub).label(r.label()).recipientName(r.recipientName())
                .line1(r.line1()).line2(r.line2()).city(r.city()).state(r.state())
                .postalCode(r.postalCode()).country(r.country()).phone(r.phone()).build());
        return AddressResponse.from(saved);
    }

    /** Deletes one of the caller's addresses, or 404 if it is not theirs / does not exist. */
    @Transactional
    public void deleteAddress(String ownerSub, UUID id) {
        if (addressRepository.deleteByIdAndOwnerSub(id, ownerSub) == 0) {
            throw ApiException.notFound("Address not found: " + id);
        }
    }
}
