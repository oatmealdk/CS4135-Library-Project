package com.elibrary.borrowing_service.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end API scenario across running services.
 *
 * This test is intended for local/demo runs with Docker services up.
 * It will skip itself if required dependencies/data are not available.
 */
class BorrowFlowE2EIntegrationTest {

    private static final String BORROWING_BASE = "http://localhost:8082";
    private static final String BOOK_BASE = "http://localhost:8083";
    private static final String USER_BASE = "http://localhost:8081";

    private static final long USER_ID = Long.parseLong(System.getProperty("e2e.userId", "1"));
    private static final long BOOK_ID_OVERRIDE = Long.parseLong(System.getProperty("e2e.bookId", "-1"));

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void borrow_then_return_updatesBorrowState() throws Exception {
        assumeTrue(isReachable(BORROWING_BASE + "/actuator/health"), "borrowing-service is not reachable");
        assumeTrue(isReachable(BOOK_BASE + "/actuator/health"), "book-service is not reachable");
        assumeTrue(isReachable(USER_BASE + "/actuator/health"), "user-service is not reachable");
        assumeTrue(userExistsAndActive(USER_ID), "test user is missing/inactive");

        long bookId = BOOK_ID_OVERRIDE > 0 ? BOOK_ID_OVERRIDE : resolveBorrowableBookId();
        assumeTrue(bookId > 0, "no borrowable book found for E2E test (set -De2e.bookId=<id> to force one)");

        // 1) Borrow
        JsonNode borrowResult = request(
            "POST",
            BORROWING_BASE + "/api/borrows",
            "{\"userId\":" + USER_ID + ",\"bookId\":" + bookId + "}"
        );
        long recordId = borrowResult.path("recordId").asLong(-1);
        assertTrue(recordId > 0, "recordId should be present");
        assertEquals("ACTIVE", borrowResult.path("status").asText());

        // 2) Borrow list contains ACTIVE record
        JsonNode beforeReturnList = request(
            "GET",
            BORROWING_BASE + "/api/borrows/user/" + USER_ID,
            null
        );
        assertTrue(listContainsRecordWithStatus(beforeReturnList, recordId, "ACTIVE"),
            "Borrow list should contain ACTIVE record");

        // 3) Return
        JsonNode returnResult = request(
            "PUT",
            BORROWING_BASE + "/api/borrows/" + recordId + "/return",
            null
        );
        assertEquals("RETURNED", returnResult.path("status").asText());

        // 4) Borrow list contains RETURNED record
        JsonNode afterReturnList = request(
            "GET",
            BORROWING_BASE + "/api/borrows/user/" + USER_ID,
            null
        );
        assertTrue(listContainsRecordWithStatus(afterReturnList, recordId, "RETURNED"),
            "Borrow list should contain RETURNED record");

        // 5) Availability endpoint still reachable and structured
        JsonNode availability = request(
            "GET",
            BOOK_BASE + "/api/books/" + bookId + "/availability",
            null
        );
        assertEquals(bookId, availability.path("bookId").asLong());
        assertTrue(availability.has("available"));
        assertTrue(availability.has("availableCopies"));
    }

    private boolean isReachable(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() >= 200 && res.statusCode() < 500;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean userExistsAndActive(long userId) {
        JsonNode node = tryRequest("GET", USER_BASE + "/api/users/" + userId + "/exists", null);
        if (node == null) {
            return false;
        }
        return node.path("exists").asBoolean(false) && node.path("isActive").asBoolean(false);
    }

    private long resolveBorrowableBookId() {
        JsonNode page = tryRequest("GET", BOOK_BASE + "/api/books?page=0&size=20", null);
        if (page == null || !page.path("content").isArray()) {
            return -1;
        }
        for (JsonNode item : page.path("content")) {
            long candidateId = item.path("bookId").asLong(-1);
            if (candidateId <= 0) {
                continue;
            }
            JsonNode availability = tryRequest("GET", BOOK_BASE + "/api/books/" + candidateId + "/availability", null);
            if (availability != null && availability.path("available").asBoolean(false)) {
                return candidateId;
            }
        }
        return -1;
    }

    private JsonNode tryRequest(String method, String url, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json");
            if ("GET".equals(method)) {
                builder.GET();
            } else if ("POST".equals(method)) {
                builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else if ("PUT".equals(method)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            } else {
                return null;
            }
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            String payload = response.body() == null || response.body().isBlank() ? "{}" : response.body();
            return json.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean listContainsRecordWithStatus(JsonNode list, long recordId, String status) {
        if (!list.isArray()) {
            return false;
        }
        for (JsonNode item : list) {
            if (item.path("recordId").asLong(-1) == recordId && status.equals(item.path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode request(String method, String url, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json");
        if ("GET".equals(method)) {
            builder.GET();
        } else if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else if ("PUT".equals(method)) {
            builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
            () -> "Request failed: " + method + " " + url + " -> " + response.statusCode() + " " + response.body());
        String payload = response.body() == null || response.body().isBlank() ? "{}" : response.body();
        return json.readTree(payload);
    }
}
