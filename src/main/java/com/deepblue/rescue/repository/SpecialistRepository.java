package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {
    @Query("""
        select distinct s
        from Specialist s
        join s.expertiseAreas e
        where s.active = true
          and lower(e.name) = lower(:expertiseName)
        order by s.lastName, s.firstName
    """)
    List<Specialist> findActiveByExpertiseIgnoreCase(String expertiseName);

    Optional<Specialist> findByProfessionalCode(String professionalCode);
}
