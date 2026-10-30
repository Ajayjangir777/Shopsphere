package com.shopsphere.common.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CommonExceptionAdviceTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new CommonExceptionAdvice()).build();

    /** An ApiException keeps the status it was created with. */
    @Test
    void apiException_usesItsOwnStatus() throws Exception {
        mockMvc.perform(get("/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("taken"));
    }

    /** Malformed JSON is the client's fault: 400, not 500. */
    @Test
    void malformedJson_gives400() throws Exception {
        mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest());
    }

    /** Wrong HTTP method must stay 405. */
    @Test
    void wrongMethod_gives405() throws Exception {
        mockMvc.perform(post("/conflict")).andExpect(status().isMethodNotAllowed());
    }

    /** Validation errors list the bad fields. */
    @Test
    void validationFailure_listsFieldErrors() throws Exception {
        mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    /** Unexpected bugs give 500 and never leak the internal message. */
    @Test
    void unexpectedError_gives500WithoutDetails() throws Exception {
        mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("secret internal detail"))));
    }

    /** A 503 must tell clients when to retry. */
    @Test
    void serviceUnavailable_includesRetryAfter() throws Exception {
        mockMvc.perform(get("/unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"));
    }

    /** Minimal controller that triggers each case. */
    @RestController
    static class TestController {
        record Body(@NotBlank String name) {}

        @PostMapping("/echo") String echo(@Valid @RequestBody Body body) { return "ok"; }
        @GetMapping("/conflict") void conflict() { throw ApiException.conflict("taken"); }
        @GetMapping("/boom") void boom() { throw new IllegalStateException("secret internal detail"); }
        @GetMapping("/unavailable") void unavailable() { throw ApiException.unavailable("down"); }
    }
}
