package com.shopsphere.order.integration;

import com.shopsphere.common.web.ApiException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the rule that decides what counts as a dependency failure. */
public class DependencyFailurePredicateTest {


    private final DependencyFailurePredicate predicate = new DependencyFailurePredicate();

    /** 5xx means the dependency is in trouble. */
    @Test
    void serverErrors_areFailures() {
        assertThat(predicate.test(FeignErrors.status(500))).isTrue();
        assertThat(predicate.test(FeignErrors.status(503))).isTrue();
    }

    /** Connection refused, timeouts and I/O errors mean the dependency is unreachable. */
    @Test
    void connectionProblems_areFailures() {
        assertThat(predicate.test(FeignErrors.connectionRefused())).isTrue();
        assertThat(predicate.test(new IOException("reset"))).isTrue();
        assertThat(predicate.test(new TimeoutException("slow"))).isTrue();
    }

    /** 4xx answers prove the dependency works; they must never open a circuit. */
    @Test
    void clientErrors_areNotFailures() {
        for (int status : new int[]{400, 401, 403, 404, 409}) {
            assertThat(predicate.test(FeignErrors.status(status))).as("status " + status).isFalse();
        }
    }

    /** Our own business errors and unrelated bugs are not dependency failures either. */
    @Test
    void applicationErrors_areNotFailures() {
        assertThat(predicate.test(ApiException.unprocessable("nope"))).isFalse();
        assertThat(predicate.test(new IllegalStateException("bug"))).isFalse();
    }

}
