package com.tnb.automation.tests.auth;

import com.tnb.automation.api.AuthClient;
import com.tnb.automation.api.TestSupportClient;
import com.tnb.automation.db.DbClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/** Failed-attempt counter and account lock (TNB-3). */
public class AccountLockApiTest {

    private static final String CUSTOMER_ID = "TNB100002";
    private static final String MOBILE = "9000000002";
    private static final String CORRECT_PASSWORD = "Test@1234";
    private static final String WRONG_PASSWORD = "Wrong@123";
    private static final String LOCK_MESSAGE =
            "Your login is locked. Use 'Unlock account' or visit your branch.";

    private final AuthClient auth = new AuthClient();
    private final TestSupportClient testSupport = new TestSupportClient();

    @BeforeMethod
    public void resetTestCustomer() {
        DbClient.resetCustomer(CUSTOMER_ID);
    }

    @Test(groups = "regression",
            description = "TC-AUTH-03: One wrong password is counted but does not lock (TNB-3 Rule 1)")
    public void oneWrongPasswordIsCounted() {
        auth.login(CUSTOMER_ID, WRONG_PASSWORD)
                .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_CREDENTIALS"));

        DbClient.CustomerState state = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(state.failedAttempts(), 1, "failed_attempts after 1 wrong password");
        Assert.assertFalse(state.locked(), "1 wrong password must not lock the account");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-04: 3 consecutive wrong passwords lock the account (TNB-3 AC1)")
    public void threeWrongPasswordsLockTheAccount() {
        // Attempts 1 and 2: rejected but not locked
        auth.login(CUSTOMER_ID, WRONG_PASSWORD)
                .then().statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));
        auth.login(CUSTOMER_ID, WRONG_PASSWORD)
                .then().statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));

        // Attempt 3: the boundary, account locks
        auth.login(CUSTOMER_ID, WRONG_PASSWORD)
                .then()
                .statusCode(423)
                .body("code", equalTo("ACCOUNT_LOCKED"))
                .body("message", equalTo(LOCK_MESSAGE));

        // Database layer
        DbClient.CustomerState state = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(state.failedAttempts(), 3, "failed_attempts after 3 wrong passwords");
        Assert.assertTrue(state.locked(), "account must be locked after 3 wrong passwords");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-05: Locked account stays locked with the correct password (TNB-3 AC2)")
    public void lockedAccountRejectsCorrectPassword() {
        // ARRANGE: lock the account with 3 wrong passwords
        for (int i = 0; i < 3; i++) {
            auth.login(CUSTOMER_ID, WRONG_PASSWORD);
        }

        // ACT + ASSERT (API): the correct password is still refused
        auth.login(CUSTOMER_ID, CORRECT_PASSWORD)
                .then()
                .statusCode(423)
                .body("code", equalTo("ACCOUNT_LOCKED"))
                .body("message", equalTo(LOCK_MESSAGE));

        // ASSERT (DB): still locked, counter unchanged
        DbClient.CustomerState state = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(state.failedAttempts(), 3, "correct password must not change the counter");
        Assert.assertTrue(state.locked(), "correct password must not unlock the account");
    }

    @Test(groups = "regression",
            description = "TC-AUTH-06: Successful login resets the failed-attempt counter (TNB-3 AC4)")
    public void successfulLoginResetsTheCounter() {
        // ARRANGE: 2 wrong passwords, one short of the lock
        auth.login(CUSTOMER_ID, WRONG_PASSWORD);
        auth.login(CUSTOMER_ID, WRONG_PASSWORD);

        // ACT: full successful login (password + OTP)
        String challengeId = auth.login(CUSTOMER_ID, CORRECT_PASSWORD)
                .then()
                .statusCode(200)
                .extract()
                .path("challengeId");
        String otp = testSupport.latestOtp(MOBILE);
        auth.verifyOtp(challengeId, otp)
                .then()
                .statusCode(200);

        // ASSERT (DB): counter is back to 0 (this was the TC-AUTH-06 bug)
        DbClient.CustomerState afterLogin = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(afterLogin.failedAttempts(), 0, "successful login must reset failed_attempts");

        // ASSERT (API + DB): one new wrong password counts from 1 again, no lock
        auth.login(CUSTOMER_ID, WRONG_PASSWORD)
                .then()
                .statusCode(401)
                .body("code", equalTo("INVALID_CREDENTIALS"));

        DbClient.CustomerState afterTypo = DbClient.customerState(CUSTOMER_ID);
        Assert.assertEquals(afterTypo.failedAttempts(), 1, "counter must restart from 0 after a good login");
        Assert.assertFalse(afterTypo.locked(), "one typo after a good login must not lock the account");
    }
}
