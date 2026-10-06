package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
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
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    private RescueCenter center;
    private RescueCase rescueCase;
    private Animal animal;
    private AnimalResponse response;

    @BeforeEach
    void setUp() {
        center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        rescueCase = new RescueCase(
                "RES-001", LocalDateTime.of(2026, 8, 20, 0, 0),
                "Bahía Concha", center, RescueStatus.IN_REHABILITATION);
        animal = new Animal(
                "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, rescueCase);
        response = new AnimalResponse(
                1L, "AN-001", "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-001", RescueStatus.IN_REHABILITATION);
    }

    @Test
    void canReceiveTreatment_shouldReturnTrueWhenAnimalInRehabilitation() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        rescueCase.changeStatus(RescueStatus.IN_REHABILITATION);

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
        verify(animalRepository).findByAnimalCode("AN-001");
    }

    @Test
    void canReceiveTreatment_shouldReturnTrueWhenAnimalUnderEvaluation() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        rescueCase.changeStatus(RescueStatus.UNDER_EVALUATION);

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
    }

    @Test
    void canReceiveTreatment_shouldReturnFalseWhenAnimalReleased() {
        when(animalRepository.findByAnimalCode("AN-001")).thenReturn(Optional.of(animal));
        rescueCase.changeStatus(RescueStatus.RELEASED);

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isFalse();
    }

    @Test
    void canReceiveTreatment_shouldThrowExceptionWhenAnimalNotFound() {
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(animalRepository).findByAnimalCode("AN-999");
        verify(mapper, never()).toResponse(any());
    }
}
