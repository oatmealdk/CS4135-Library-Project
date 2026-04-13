package com.elibrary.borrowing_service.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BookServiceClientContractTest {

    @Test
    void checkAvailability_mapsBookServiceAvailabilityPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BookServiceClient client = new BookServiceClient(restTemplate);

        server.expect(requestTo("http://book-service/api/books/42/availability"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                "{\"bookId\":42,\"available\":true,\"availableCopies\":3}",
                MediaType.APPLICATION_JSON
            ));

        BookServiceClient.BookAvailability result = client.checkAvailability(42L);

        assertEquals(42L, result.bookId());
        assertTrue(result.available());
        assertEquals(3, result.availableCopies());
        server.verify();
    }

    @Test
    void getBookSummary_mapsBookServiceBookDtoFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        BookServiceClient client = new BookServiceClient(restTemplate);

        server.expect(requestTo("http://book-service/api/books/7"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                "{\"bookId\":7,\"title\":\"Dune\",\"author\":\"Frank Herbert\",\"isbn\":\"978-0441013593\",\"availableCopies\":2}",
                MediaType.APPLICATION_JSON
            ));

        BookServiceClient.BookSummary result = client.getBookSummary(7L);

        assertEquals("Dune", result.title());
        assertEquals("Frank Herbert", result.author());
        assertEquals("978-0441013593", result.isbn());
        server.verify();
    }
}
