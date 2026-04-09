package com.elibrary.borrowing_service.web;

import com.elibrary.borrowing_service.application.BorrowingService;
import com.elibrary.borrowing_service.application.dto.BorrowRecordDTO;
import com.elibrary.borrowing_service.domain.model.BorrowRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer integration test, which boots the MVC slice with BorrowingController,
 * mocks BorrowingService, and verifies HTTP routing, status codes, JSON validation,
 * and exception-handler mapping.
 */
@WebMvcTest(BorrowingController.class)
@TestPropertySource(properties = {
    "spring.cloud.discovery.enabled=false",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
})
class BorrowingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowingService borrowingService;

    private BorrowRecordDTO testDTO() {
        BorrowRecord record = BorrowRecord.create(1L, 100L);
        ReflectionTestUtils.setField(record, "recordId", 42L);
        return BorrowRecordDTO.from(record);
    }

    // POST /api/borrows

    @Test
    void borrowBook_returns201WithDto() throws Exception {
        BorrowRecordDTO dto = testDTO();
        when(borrowingService.borrowBook(1L, 100L)).thenReturn(dto);

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"bookId\":100}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recordId").value(42))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.bookId").value(100))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void borrowBook_returns400WhenBodyMissesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void borrowBook_returns422WhenBusinessRuleViolated() throws Exception {
        when(borrowingService.borrowBook(anyLong(), anyLong()))
            .thenThrow(new IllegalStateException("User has reached the maximum of 5 concurrent borrows."));

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"bookId\":100}"))
            .andExpect(status().is(422))
            .andExpect(content().string("User has reached the maximum of 5 concurrent borrows."));
    }

    @Test
    void borrowBook_returns404WhenUserNotFound() throws Exception {
        when(borrowingService.borrowBook(anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("User 999 does not exist or is not active."));

        mockMvc.perform(post("/api/borrows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":999,\"bookId\":100}"))
            .andExpect(status().isNotFound())
            .andExpect(content().string("User 999 does not exist or is not active."));
    }

    // PUT /api/borrows/{id}/return

    @Test
    void returnBook_returns200() throws Exception {
        BorrowRecordDTO dto = testDTO();
        when(borrowingService.returnBook(42L)).thenReturn(dto);

        mockMvc.perform(put("/api/borrows/42/return"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recordId").value(42));
    }

    @Test
    void returnBook_returns404WhenRecordMissing() throws Exception {
        when(borrowingService.returnBook(999L))
            .thenThrow(new IllegalArgumentException("BorrowRecord not found: 999"));

        mockMvc.perform(put("/api/borrows/999/return"))
            .andExpect(status().isNotFound());
    }

    // PUT /api/borrows/{id}/renew

    @Test
    void renewBook_returns200() throws Exception {
        BorrowRecordDTO dto = testDTO();
        when(borrowingService.renewBook(42L)).thenReturn(dto);

        mockMvc.perform(put("/api/borrows/42/renew"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recordId").value(42));
    }

    @Test
    void renewBook_returns422WhenOverdue() throws Exception {
        when(borrowingService.renewBook(42L))
            .thenThrow(new IllegalStateException("A book cannot be renewed when its status is OVERDUE."));

        mockMvc.perform(put("/api/borrows/42/renew"))
            .andExpect(status().is(422));
    }

    // GET /api/borrows/user/{userId}

    @Test
    void getBorrowsByUser_returnsList() throws Exception {
        when(borrowingService.getBorrowsByUser(1L)).thenReturn(List.of(testDTO()));

        mockMvc.perform(get("/api/borrows/user/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].recordId").value(42));
    }

    // GET /api/borrows/{recordId}

    @Test
    void getBorrowRecord_returnsDto() throws Exception {
        when(borrowingService.getBorrowRecord(42L)).thenReturn(testDTO());

        mockMvc.perform(get("/api/borrows/42"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bookId").value(100));
    }

    // GET /api/borrows/book/{bookId}/active

    @Test
    void getActiveBorrowsByBook_returnsList() throws Exception {
        when(borrowingService.getActiveBorrowsByBook(100L)).thenReturn(List.of(testDTO()));

        mockMvc.perform(get("/api/borrows/book/100/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
