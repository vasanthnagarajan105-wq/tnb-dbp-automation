package com.tnb.automation.tests;

import com.tnb.automation.config.ConfigManager;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class HealthCheckTest {

    @Test(groups = "smoke", description = "Application is up and connected to its database")
    public void applicationIsUp() {
        given()
                .baseUri(ConfigManager.get("base.url"))
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}