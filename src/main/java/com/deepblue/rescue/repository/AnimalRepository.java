package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByCommonNameContainingIgnoreCase(String name);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String code);

    @Query("""
        select distinct a
        from Animal a
        join a.rescueCase rc
        join a.treatments t
        join t.specialist s
        join s.expertiseAreas e
        where rc.status = :status
          and lower(e.name) = lower(:expertiseName)
    """)
    List<Animal> findInRehabilitationWithTreatmentsByExpertise(RescueStatus status, String expertiseName);
}
