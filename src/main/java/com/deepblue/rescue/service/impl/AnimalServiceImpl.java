package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.AnimalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;
    private final AnimalMapper mapper;

    public AnimalServiceImpl(AnimalRepository animalRepository, AnimalMapper mapper) {
        this.animalRepository = animalRepository;
        this.mapper = mapper;
    }

    @Override
    public AnimalResponse findByCode(String animalCode) {
        return animalRepository.findByAnimalCode(animalCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));
    }

    @Override
    public List<AnimalResponse> findAnimalsInRehabilitation() {
        return animalRepository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public boolean canReceiveTreatment(String animalCode) {
        Animal animal = animalRepository.findByAnimalCode(animalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + animalCode));

        RescueStatus status = animal.getRescueCase().getStatus();
        // Same rule as TreatmentServiceImpl.register: only RELEASED/CLOSED animals cannot be treated.
        return status != RescueStatus.RELEASED && status != RescueStatus.CLOSED;
    }
}
