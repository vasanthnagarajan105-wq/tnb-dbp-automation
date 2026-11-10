package com.tnb.automation.tests.auth;

import com.tnb.automation.api.AuthClient;
import com.tnb.automation.api.CustomerClient;
import com.tnb.automation.db.DbClient;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;

/** Security checks on the authentication APIs (TNB-4). */
public class AuthSecurityApiTest {

    private static final String KNOWN_CUSTOMER_ID = "TNB100002";
    private static final String UNKNOWN_CUSTOMER_ID = "TNB999999";
    private static final String CORRECT_PASSWORD = "Test@1234";
    private static final String WRONG_PASSWORD = "Wrong@123";

    private final AuthClient auth = new AuthClient();
    private final CustomerClient customers = new CustomerClient();

    @BeforeMethod
    public void resetTestCustomer() {
        DbClient.resetCustomer(KNOWN_CUSTOMER_ID);
    }

    @Test(groups = "regression",
            description = "TC-AUTH-02: Unknown Customer ID gets the same error as a wrong password (TNB-4 Security)")
    public void unknownCustomerAndWrongPasswordLookTheSame() {
        Response unknownId = auth.login(UNKNOWN_CUSTOMER_ID, CORRECT_PASSWORD);
        Response wrongPassword = auth.login(KNOWN_CUSTOMER_ID, WRONG_PASSWORD);

        unknownId.then().statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));
        wrongPassword.then().statusCode(401).body("code", equalTo("INVALID_CREDENTIALS"));

        // The key security point: an attacker cannot tell which Customer IDs exist
        String unknownMessage = unknownId.path("message");
        String wrongPasswordMessage = wrongPassword.path("message");
        Assert.assertEquals(unknownMessage, wrongPasswordMessage,
                "unknown ID and wrong password must return the identical message");
    }

    @Test(groups = {"smoke", "regression"},
            description = "TC-AUTH-10: Protected API rejects a request with no token (TNB-4 Security)")
    public void profileWithoutTokenIsRejected() {
        customers.meWithoutToken()
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", equalTo("Missing or invalid access token."));
    }
}
