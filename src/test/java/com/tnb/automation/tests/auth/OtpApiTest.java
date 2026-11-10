package com.tnb.automation.tests.auth;

import com.tnb.automation.api.AuthClient;
import com.tnb.automation.api.TestSupportClient;
import com.tnb.automation.db.DbClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/** OTP rules: wrong OTP, resend limit, single use (TNB-4). */
public class OtpApiTest {

    private static final String CUSTOMER_ID = "TNB100002";
    private static final String MOBILE = "9000000002";
    private static final String PASSWORD = "Test@1234";

    private final AuthClient auth = new AuthClient();
    private final TestSupportClient testSupport = new TestSupportClient();

    @BeforeMethod
    public void resetTestCustomer() {
        DbClient.resetCustomer(CUSTOMER_ID);
    }

    /** Step 1 of every OTP test: correct password, returns the challengeId. */
    private String loginWithPassword() {
        return auth.login(CUSTOMER_ID, PASSWORD)
                .then()
                .statusCode(200)
                .extract()
                .path("challengeId");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-07: Wrong OTP is rejected and counted as a failed attempt (TNB-4 AC2)")
    public void wrongOtpIsCountedAsFailedAttempt() {
        String challengeId = loginWithPassword();

        // Pick an OTP that is guaranteed to be wrong
        String realOtp = testSupport.latestOtp(MOBILE);
        String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        auth.verifyOtp(challengeId, wrongOtp)
                .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_OTP"));

        DbClient.CustomerState customer = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(customer.failedAttempts(), 1, "wrong OTP must add 1 to failed_attempts");
        Assert.assertFalse(customer.locked(), "1 wrong OTP must not lock the account");

        DbClient.ChallengeState challenge = DbClient.challengeState(challengeId);
        Assert.assertFalse(challenge.used(), "a wrong OTP must not mark the challenge as used");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-08: OTP can be resent 3 times; the 4th resend is refused (TNB-4 AC4)")
    public void fourthResendIsRefused() {
        String challengeId = loginWithPassword();

        // Resends 1, 2 and 3 are allowed
        for (int i = 1; i <= 3; i++) {
            auth.resendOtp(challengeId)
                    .then()
                    .statusCode(200);
        }

        // Resend 4: the boundary, refused (this was the TC-AUTH-08 off-by-one bug)
        auth.resendOtp(challengeId)
                .then()
                .statusCode(429)
                .body("code", equalTo("RESEND_LIMIT_REACHED"));

        DbClient.ChallengeState challenge = DbClient.challengeState(challengeId);
        Assert.assertEquals(challenge.resendCount(), 3, "resend_count must stop at the limit of 3");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-09: An OTP cannot be used twice (TNB-4 Rule 2)")
    public void otpCannotBeUsedTwice() {
        String challengeId = loginWithPassword();
        String otp = testSupport.latestOtp(MOBILE);

        // First use: logs in
        auth.verifyOtp(challengeId, otp)
                .then()
                .statusCode(200);

        // Second use of the same OTP: refused
        auth.verifyOtp(challengeId, otp)
                .then()
                .statusCode(400)
                .body("code", equalTo("OTP_ALREADY_USED"));

        DbClient.ChallengeState challenge = DbClient.challengeState(challengeId);
        Assert.assertTrue(challenge.used(), "challenge must be marked used after a successful login");
    }
}
