package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    private RescueCaseResponse response() {
        return new RescueCaseResponse(
                1L,
                "RES-2026-001",
                LocalDateTime.of(2026, 8, 20, 0, 0),
                "Bahia Concha",
                RescueStatus.IN_REHABILITATION,
                "DB-CAR",
                "AN-2026-001"
        );
    }

    // ----------------------------------------------------------------------
    // TEST 1 — GET RescueCase existente -> 200
    // ----------------------------------------------------------------------
    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        when(service.findByCode("RES-2026-001")).thenReturn(response());

        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$.centerCode").value("DB-CAR"))
                .andExpect(jsonPath("$.animalCode").value("AN-2026-001"));

        verify(service).findByCode("RES-2026-001");
    }

    // ----------------------------------------------------------------------
    // TEST 2 — GET RescueCase inexistente -> 404 + ErrorResponse
    // ----------------------------------------------------------------------
    @Test
    void shouldReturn404WhenCaseDoesNotExist() throws Exception {
        when(service.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));

        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).findByCode("RES-999");
    }

    // ----------------------------------------------------------------------
    // TEST 3 — GET RescueCases por status -> 200
    // ----------------------------------------------------------------------
    @Test
    void shouldReturnCasesByStatus() throws Exception {
        RescueCaseResponse case1 = response();
        RescueCaseResponse case2 = new RescueCaseResponse(
                2L, "RES-2026-002", LocalDateTime.of(2026, 8, 22, 0, 0),
                "Rodadero", RescueStatus.IN_REHABILITATION, "DB-CAR", "AN-2026-002");

        when(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(case1, case2));

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    // ----------------------------------------------------------------------
    // TEST 4 — GET status inválido -> 400
    // ----------------------------------------------------------------------
    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());

        verify(service, never()).findByStatus(any());
    }

    // ----------------------------------------------------------------------
    // TEST 5 — PATCH status válido -> 200
    // ----------------------------------------------------------------------
    @Test
    void shouldChangeRescueCaseStatus() throws Exception {
        RescueCaseResponse changed = new RescueCaseResponse(
                1L, "RES-001", LocalDateTime.of(2026, 8, 20, 0, 0),
                "Bahia Concha", RescueStatus.READY_FOR_RELEASE, "DB-CAR", "AN-2026-001");

        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenReturn(changed);

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "READY_FOR_RELEASE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RELEASE"));

        verify(service).changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class));
    }

    // ----------------------------------------------------------------------
    // TEST 6 — PATCH request inválido -> 400 + details
    // ----------------------------------------------------------------------
    @Test
    void shouldReturn400WhenChangeStatusRequestIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.status").value("Status is required"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    // ----------------------------------------------------------------------
    // TEST 7 — PATCH transición inválida -> 409
    // ----------------------------------------------------------------------
    @Test
    void shouldReturn409WhenTransitionIsInvalid() throws Exception {
        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenThrow(new BusinessRuleException("Invalid status transition"));

        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "READY_FOR_RELEASE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"))
                .andExpect(jsonPath("$.details").isMap());

        verify(service).changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class));
    }
}
