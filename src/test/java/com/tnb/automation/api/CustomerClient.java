package com.tnb.automation.api;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Calls the customer APIs. These need a Bearer token from a completed login. */
public class CustomerClient {

    private static final String ME = "/api/v1/customers/me";

    public Response me(String accessToken) {
        return given()
                .spec(RequestSpecs.base())
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(ME);
    }

    /** Same call with no Authorization header, for security tests (TC-AUTH-10). */
    public Response meWithoutToken() {
        return given()
                .spec(RequestSpecs.base())
                .when()
                .get(ME);
    }
}