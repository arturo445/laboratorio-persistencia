package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    private RescueCase rescueCase;
    private RescueCaseResponse response;

    @BeforeEach
    void setUp() {
        rescueCase = new RescueCase(
                "RES-001", LocalDateTime.of(2026, 9, 28, 0, 0), "Bahía de Santa Marta", null, RescueStatus.ADMITTED);

        response = new RescueCaseResponse(
                1L, "RES-001", LocalDateTime.of(2026, 9, 28, 0, 0), "Bahía de Santa Marta",
                RescueStatus.ADMITTED, "CEN-001", "ANI-001");
    }

    @Test
    void shouldFindRescueCaseByCode(){

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);

        verify(repository).findByCaseCode("RES-001");

        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldFindInexistentRescueCaseByCode(){
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999")).isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findByCaseCode("RES-999");
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        ChangeRescueStatusRequest request = new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus("RES-001", request);

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        assertThat(result).isEqualTo(response);

        verify(repository).save(rescueCase);
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenTransitionIsInvalid() {
        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);

        verify(repository).findByCaseCode("RES-001");
        verify(repository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }
}
