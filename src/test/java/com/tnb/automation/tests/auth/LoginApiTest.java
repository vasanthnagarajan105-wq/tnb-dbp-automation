package com.tnb.automation.tests.auth;

import com.tnb.automation.api.AuthClient;
import com.tnb.automation.api.CustomerClient;
import com.tnb.automation.api.TestSupportClient;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

public class LoginApiTest {

    // Synthetic test customer from the dev seed data
    private static final String CUSTOMER_ID = "TNB100001";
    private static final String PASSWORD = "Test@1234";
    private static final String MOBILE = "9000000001";

    private final AuthClient auth = new AuthClient();
    private final TestSupportClient testSupport = new TestSupportClient();
    private final CustomerClient customers = new CustomerClient();

    @Test(groups = {"smoke", "regression"},
            description = "TC-AUTH-01: login with password + OTP, then fetch own profile")
    public void loginWithPasswordAndOtpReturnsProfile() {
        // Step 1: password login sends an OTP
        String challengeId = auth.login(CUSTOMER_ID, PASSWORD)
                .then()
                .statusCode(200)
                .body("maskedMobile", equalTo("******0001"))
                .extract()
                .path("challengeId");

        // Step 2: read the OTP the simulated SMS gateway sent
        String otp = testSupport.latestOtp(MOBILE);

        // Step 3: verify the OTP and receive a token
        String token = auth.verifyOtp(challengeId, otp)
                .then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("customerId", equalTo(CUSTOMER_ID))
                .extract()
                .path("accessToken");

        // Step 4: the token gives access to the customer's own profile
        customers.me(token)
                .then()
                .statusCode(200)
                .body("customerId", equalTo(CUSTOMER_ID))
                .body("fullName", equalTo("Arun Kumar"))
                .body("maskedMobile", equalTo("******0001"));
    }
}