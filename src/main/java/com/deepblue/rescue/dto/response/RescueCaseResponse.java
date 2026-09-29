package com.deepblue.rescue.dto.response;

import com.deepblue.rescue.domain.RescueStatus;

import java.time.LocalDateTime;

public record RescueCaseResponse(
        Long id,

        String caseCode,

        LocalDateTime rescueDate,

        String rescueLocation,

        RescueStatus status,

        String centerCode,

        String animalCode
){
}

