package com.tnb.automation.api;

import io.restassured.response.Response;

import java.util.Map;

import static io.restassured.RestAssured.given;

/** Calls the authentication APIs (TNB-4, TNB-3). Returns the raw response; tests decide what to check. */
public class AuthClient {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String VERIFY_OTP = "/api/v1/auth/verify-otp";
    private static final String RESEND_OTP = "/api/v1/auth/resend-otp";

    public Response login(String customerId, String password) {
        return given()
                .spec(RequestSpecs.base())
                .body(Map.of("customerId", customerId, "password", password))
                .when()
                .post(LOGIN);
    }

    public Response verifyOtp(String challengeId, String otp) {
        return given()
                .spec(RequestSpecs.base())
                .body(Map.of("challengeId", challengeId, "otp", otp))
                .when()
                .post(VERIFY_OTP);
    }

    public Response resendOtp(String challengeId) {
        return given()
                .spec(RequestSpecs.base())
                .body(Map.of("challengeId", challengeId))
                .when()
                .post(RESEND_OTP);
    }
}