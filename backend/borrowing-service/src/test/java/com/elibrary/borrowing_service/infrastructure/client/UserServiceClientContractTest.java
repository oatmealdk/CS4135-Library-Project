package com.elibrary.borrowing_service.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserServiceClientContractTest {

    @Test
    void validateUser_mapsExistsAndIsActivePayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        UserServiceClient client = new UserServiceClient(restTemplate);

        server.expect(requestTo("http://user-service/api/users/10/exists"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                "{\"userId\":10,\"exists\":true,\"isActive\":true}",
                MediaType.APPLICATION_JSON
            ));

        UserServiceClient.UserValidation result = client.validateUser(10L);

        assertEquals(10L, result.userId());
        assertTrue(result.exists());
        assertTrue(result.isActive());
        server.verify();
    }

    @Test
    void getDeskProfile_mapsNameAndEmailPayload() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        UserServiceClient client = new UserServiceClient(restTemplate);

        server.expect(requestTo("http://user-service/api/users/25/desk-profile"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                "{\"name\":\"Alice Reader\",\"email\":\"alice@example.com\"}",
                MediaType.APPLICATION_JSON
            ));

        UserServiceClient.DeskProfile result = client.getDeskProfile(25L);

        assertEquals("Alice Reader", result.name());
        assertEquals("alice@example.com", result.email());
        server.verify();
    }

    @Test
    void validateUser_defaultsToFalseWhenResponseIsEmptyObject() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        UserServiceClient client = new UserServiceClient(restTemplate);

        server.expect(requestTo("http://user-service/api/users/99/exists"))
            .andExpect(method(GET))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        UserServiceClient.UserValidation result = client.validateUser(99L);

        assertEquals(99L, result.userId());
        assertFalse(result.exists());
        assertFalse(result.isActive());
        server.verify();
    }
}
