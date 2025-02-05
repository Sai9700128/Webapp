package com.Healthcheck.HealthCheck;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class HealthCheckApplicationTests {

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;  // Change if running on a different port
    }

    @Test
    void contextLoads() {
        // Spring Boot Context Load Test
    }

    @Test
    void testHealthCheckSuccess() {
        given()
                .when()
                .get("/healthz")
                .then()
                .statusCode(200);
    }

    @Test
    void testInvalidEndpointReturns404() {
        given()
                .when()
                .get("/invalid-endpoint")
                .then()
                .statusCode(404);
    }

    @Test
    void testRequestNotAllowed405() {
        
         // List of all methods to test except GET
         String[] methods = {"POST", "PUT", "DELETE", "PATCH"};

         for (String method : methods) {
             given()
                     .when()
                     .request(method , "/healthz")  // Using dynamic method (POST, PUT, DELETE, PATCH, HEAD)
                     .then()
                     .statusCode(405);  // Should return 405 Method Not Allowed
         }
 
    }

    @Test
    void testGetWithPayloadReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"key\":\"value\"}")  // Sending a payload
                .when()
                .get("/healthz")
                .then()
                .statusCode(400);
    }
}

