package com.tnb.automation.tests.auth;

import com.tnb.automation.api.AuthClient;
import com.tnb.automation.db.DbClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/** Failed-attempt counter and account lock (TNB-3). */
public class AccountLockApiTest {

    private static final String CUSTOMER_ID = "TNB100002";
    private static final String WRONG_PASSWORD = "Wrong@123";
    private static final String LOCK_MESSAGE =
            "Your login is locked. Use 'Unlock account' or visit your branch.";

    private final AuthClient auth = new AuthClient();

    @BeforeMethod
    public void resetTestCustomer() {
        DbClient.resetCustomer(CUSTOMER_ID);
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
}