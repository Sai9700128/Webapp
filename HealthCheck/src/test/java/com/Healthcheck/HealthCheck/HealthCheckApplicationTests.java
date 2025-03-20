package com.Healthcheck.HealthCheck;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class HealthCheckApplicationTests {

    @BeforeAll
    public static void setup() {
        // Set up base URI and port for RestAssured to communicate with the application
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080; // Change if running on a different port
    }

    // Health Check Tests

    @Test
    void testHealthCheckSuccess() {
        // Test if the /healthz endpoint is reachable and returns HTTP 200 OK status.
        given()
                .when()
                .get("/healthz")
                .then()
                .statusCode(200); // Should return HTTP 200 for a successful health check
    }

    @Test
    void testInvalidEndpointReturns404() {
        // Test invalid endpoint /invalid-endpoint to ensure it returns HTTP 404 Not
        // Found
        given()
                .when()
                .get("/invalid-endpoint")
                .then()
                .statusCode(404); // Should return HTTP 404 for non-existent endpoint
    }

    @Test
    void testRequestNotAllowed405() {
        // List of HTTP methods to test that should not be allowed on /healthz
        String[] methods = { "POST", "PUT", "DELETE", "PATCH" };

        for (String method : methods) {
            given()
                    .when()
                    .request(method, "/healthz") // Dynamically test methods (POST, PUT, DELETE, PATCH)
                    .then()
                    .statusCode(405); // Should return HTTP 405 Method Not Allowed for all non-GET methods
        }
    }

    @Test
    void testGetWithPayloadReturns400() {
        // Test sending a payload with a GET request, expecting a 400 Bad Request
        given()
                .contentType(ContentType.JSON)
                .body("{\"key\":\"value\"}") // Sending a payload
                .when()
                .get("/healthz")
                .then()
                .statusCode(400); // Should return HTTP 400 for GET requests with a body
    }

    // /v1/file Endpoint Tests

    @Test
    void testPostFileUpload() {
        // Test file upload via POST /v1/file
        String filePath = "path_to_your_test_file"; // Replace with an actual file path for testing

        given()
                .multiPart("file", new java.io.File(filePath))
                .when()
                .post("/v1/file")
                .then()
                .statusCode(200) // Should return HTTP 200 for successful upload
                .body("fileKey", notNullValue()); // Verify that a fileKey is returned in the response
    }

    @Test
    void testGetFile() {
        // Assuming fileKey is returned from a previous POST request
        String fileKey = "exampleFileKey"; // Replace with actual fileKey returned from POST

        given()
                .when()
                .get("/v1/file/{fileKey}", fileKey)
                .then()
                .statusCode(200) // Should return HTTP 200 for a successful file retrieval
                .body("fileKey", equalTo(fileKey)); // Ensure the correct file is retrieved based on fileKey
    }

    @Test
    void testDeleteFile() {
        // Assuming fileKey is returned from a previous POST request
        String fileKey = "exampleFileKey"; // Replace with actual fileKey

        given()
                .when()
                .delete("/v1/file/{fileKey}", fileKey)
                .then()
                .statusCode(204); // Should return HTTP 204 for successful file deletion (no content)
    }

    @Test
    void testDeleteFileNotFound() {
        // Test delete on a non-existent file
        String fileKey = "nonExistentFileKey"; // Replace with a fileKey that does not exist

        given()
                .when()
                .delete("/v1/file/{fileKey}", fileKey)
                .then()
                .statusCode(404); // Should return HTTP 404 for file not found
    }
}
