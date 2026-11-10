package com.tnb.automation.api;

import io.restassured.response.Response;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/** Test hooks for automation: read what the simulated SMS gateway sent (dev and sit only). */
public class TestSupportClient {

    private static final String SMS = "/api/v1/test-support/sms";
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    public Response latestSms(String mobile) {
        return given()
                .spec(RequestSpecs.base())
                .queryParam("mobile", mobile)
                .when()
                .get(SMS);
    }

    /** Returns the 6-digit OTP from the newest SMS sent to this mobile number. */
    public String latestOtp(String mobile) {
        String message = latestSms(mobile)
                .then()
                .statusCode(200)
                .extract()
                .path("[0].message");

        if (message == null) {
            throw new IllegalStateException("No SMS found for mobile " + mobile);
        }
        Matcher matcher = SIX_DIGITS.matcher(message);
        if (!matcher.find()) {
            throw new IllegalStateException("No 6-digit OTP in SMS: " + message);
        }
        return matcher.group(1);
    }
}