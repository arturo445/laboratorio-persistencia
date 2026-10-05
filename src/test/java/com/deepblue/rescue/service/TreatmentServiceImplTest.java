package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    private RescueCenter center;
    private RescueCase rescueCase;
    private Animal animal;
    private Specialist specialist;
    private TreatmentResponse response;

    @BeforeEach
    void setUp() {
        center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        rescueCase = new RescueCase("RES-001", LocalDateTime.of(2026, 8, 20, 0, 0), "Bahía Concha", center, RescueStatus.IN_REHABILITATION);
        animal = new Animal("AN-001", "Green Sea Turtle", "Chelonia mydas", com.deepblue.rescue.domain.AnimalSex.FEMALE, rescueCase);
        specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org");
        response = new TreatmentResponse(1L, "AN-001", "SPEC-001", LocalDateTime.now(), TreatmentType.WOUND_CARE, "Cleaning");
    }

    @Test
    void shouldRegisterTreatmentWhenAllValid() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE, "Cleaning");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);
        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository).save(any(Treatment.class));
        verify(mapper).toResponse(any(Treatment.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenSpecialistInactive() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE, "Cleaning");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        setSpecialistActive(specialist, false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCaseReleased() {
        // Cambiar estado del caso a RELEASED
        rescueCase.changeStatus(RescueStatus.RELEASED);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0), TreatmentType.WOUND_CARE, "Cleaning");

        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001")).thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository, never()).save(any());
    }

    /**
     * Desactiva un Specialist para el test.
     * La entidad no expone un método de desactivación (y el plan prohíbe modificarla),
     * así que se usa reflection pero fallando ruidosamente si el campo cambia.
     */
    private static void setSpecialistActive(Specialist specialist, boolean active) {
        try {
            java.lang.reflect.Field activeField = Specialist.class.getDeclaredField("active");
            activeField.setAccessible(true);
            activeField.setBoolean(specialist, active);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "No se pudo modificar el campo 'active' de Specialist: " + e.getMessage(), e);
        }
    }
}
