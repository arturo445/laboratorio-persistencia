package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RescueCenterRepository rescueCenterRepository;
    @Autowired
    private RescueCaseRepository rescueCaseRepository;
    @Autowired
    private AnimalRepository animalRepository;
    @Autowired
    private MedicalRecordRepository medicalRecordRepository;
    @Autowired
    private SpecialistRepository specialistRepository;
    @Autowired
    private ExpertiseRepository expertiseRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================================
    // Paso 47 — Test de Flyway
    // =========================================================================
    @Test
    @DisplayName("Flyway debe ejecutar al menos V1 y V2")
    void flywayShouldExecuteMigrations() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );

        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    // =========================================================================
    // Paso 48 — Test de métodos heredados
    // =========================================================================
    @Test
    @DisplayName("Métodos heredados: save, findById, existsById, count")
    void shouldUseInheritedMethods() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean Center");
        center.setCity("Santa Marta");

        rescueCenterRepository.save(center);

        Optional<RescueCenter> found = rescueCenterRepository.findById(center.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DB-CAR");

        assertThat(rescueCenterRepository.existsById(center.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isGreaterThanOrEqualTo(1);
    }

    // =========================================================================
    // Paso 49 — Test relación 1:N
    // =========================================================================
    @Test
    @DisplayName("RescueCenter 1:N RescueCase — dos casos pertenecen al mismo centro")
    void shouldPersistOneToMany() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase();
        case1.setCaseCode("RES-2026-001");
        case1.setRescueDate(LocalDate.of(2026, 8, 1));
        case1.setRescueLocation("Bahía Concha");
        case1.setStatus(RescueStatus.IN_REHABILITATION);
        case1.setRescueCenter(center);
        rescueCaseRepository.save(case1);

        RescueCase case2 = new RescueCase();
        case2.setCaseCode("RES-2026-002");
        case2.setRescueDate(LocalDate.of(2026, 8, 5));
        case2.setRescueLocation("Rodadero");
        case2.setStatus(RescueStatus.ADMITTED);
        case2.setRescueCenter(center);
        rescueCaseRepository.save(case2);

        RescueCase foundCase1 = rescueCaseRepository.findById(case1.getId()).orElseThrow();
        RescueCase foundCase2 = rescueCaseRepository.findById(case2.getId()).orElseThrow();

        assertThat(foundCase1.getRescueCenter().getCode()).isEqualTo("DB-CAR");
        assertThat(foundCase2.getRescueCenter().getCode()).isEqualTo("DB-CAR");
    }

    // =========================================================================
    // Paso 50 — Test RescueCase 1:1 Animal
    // =========================================================================
    @Test
    @DisplayName("RescueCase 1:1 Animal — case.getAnimal y animal.getRescueCase")
    void shouldPersistOneToOneAnimal() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-001");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 1));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        animalRepository.save(animal);
        animalRepository.flush();

        // Refresh from database
        entityManager.refresh(rescueCase);

        RescueCase found = rescueCaseRepository.findById(rescueCase.getId()).orElseThrow();
        Animal foundAnimal = animalRepository.findById(animal.getId()).orElseThrow();

        assertThat(found.getAnimal()).isNotNull();
        assertThat(foundAnimal.getRescueCase()).isNotNull();
        assertThat(foundAnimal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    // =========================================================================
    // Paso 51 — Test Animal 1:1 MedicalRecord
    // =========================================================================
    @Test
    @DisplayName("Animal 1:1 MedicalRecord — cascade genera ambos IDs")
    void shouldPersistMedicalRecordByCascade() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-001");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 1));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Leatherback Turtle");
        animal.setScientificName("Dermochelys coriacea");
        animal.setSex(AnimalSex.MALE);
        animal.setRescueCase(rescueCase);
        animalRepository.save(animal);

        MedicalRecord record = new MedicalRecord();
        record.setAnimal(animal);
        record.setInitialWeight(new BigDecimal("28.40"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Left front flipper injury");
        record.setObservations("Needs monitoring");
        animal.assignMedicalRecord(record);
        animalRepository.flush();

        assertThat(record.getId()).isNotNull();
        assertThat(animal.getMedicalRecord()).isNotNull();
    }

    // =========================================================================
    // Paso 52 — Test N:M
    // =========================================================================
    @Test
    @DisplayName("Specialist N:M Expertise — Elena con Trauma + Rehabilitation")
    void shouldPersistManyToMany() {
        Expertise trauma = findOrCreateExpertise("Trauma");
        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Specialist found = specialistRepository.findById(elena.getId()).orElseThrow();
        assertThat(found.getExpertiseAreas()).hasSize(2);
    }

    // =========================================================================
    // Paso 53 — Test Query Method simple
    // =========================================================================
    @Test
    @DisplayName("Query Method: findByStatus — 2 casos IN_REHABILITATION")
    void shouldFindCasesByStatus() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        createCase(center, "RES-001", RescueStatus.IN_REHABILITATION);
        createCase(center, "RES-002", RescueStatus.READY_FOR_RELEASE);
        createCase(center, "RES-003", RescueStatus.IN_REHABILITATION);

        List<RescueCase> cases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(cases).hasSize(2);
        assertThat(cases.get(0).getCaseCode()).isEqualTo("RES-001");
        assertThat(cases.get(1).getCaseCode()).isEqualTo("RES-003");
    }

    // =========================================================================
    // Paso 54 — Test Query Method navegando relaciones
    // =========================================================================
    @Test
    @DisplayName("Query Method navegación: animales del centro DB-CAR")
    void shouldFindAnimalsByCenterCode() {
        RescueCenter car = new RescueCenter();
        car.setCode("DB-CAR");
        car.setName("DeepBlue Caribbean");
        car.setCity("Santa Marta");
        rescueCenterRepository.save(car);

        RescueCenter pac = new RescueCenter();
        pac.setCode("DB-PAC");
        pac.setName("DeepBlue Pacific");
        pac.setCity("Buenaventura");
        rescueCenterRepository.save(pac);

        createAnimalWithCase(car, "AN-CAR-001", "Green Sea Turtle", "RES-CAR-001");
        createAnimalWithCase(car, "AN-CAR-002", "Hawksbill Turtle", "RES-CAR-002");
        createAnimalWithCase(pac, "AN-PAC-001", "Loggerhead Turtle", "RES-PAC-001");

        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(animals).hasSize(2);
        assertThat(animals).extracting(Animal::getAnimalCode)
                .containsExactlyInAnyOrder("AN-CAR-001", "AN-CAR-002");
    }

    // =========================================================================
    // Paso 55 — Test JPQL de especialistas
    // =========================================================================
    @Test
    @DisplayName("JPQL: especialistas con experiencia en Trauma")
    void shouldFindSpecialistsWithTraumaExpertise() {
        Expertise trauma = findOrCreateExpertise("Trauma");
        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");
        Expertise marineMammals = findOrCreateExpertise("Marine Mammals");

        Specialist elena = createSpecialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Specialist mateo = createSpecialist("SPEC-M", "Mateo", "Ríos", "mateo@deepblue.org");
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);
        specialistRepository.save(mateo);

        List<Specialist> specialists = specialistRepository.findActiveByExpertiseIgnoreCase("Trauma");

        assertThat(specialists).hasSize(1);
        assertThat(specialists.get(0).getFirstName()).isEqualTo("Elena");
    }

    // =========================================================================
    // Paso 56-57 — Test tratamientos y Query Method
    // =========================================================================
    @Test
    @DisplayName("Query Method: tratamientos de un animal ordenados cronológicamente")
    void shouldFindTreatmentsOrderedByDate() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal = createAnimalWithCase(center, "AN-001", "Green Sea Turtle", "RES-001");
        Specialist elena = createSpecialistAndSave("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");

        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 3, 10, 0), TreatmentType.HYDRATION);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 5, 10, 0), TreatmentType.OBSERVATION);

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(treatments).hasSize(3);
        assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(treatments.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);
        assertThat(treatments.get(2).getType()).isEqualTo(TreatmentType.OBSERVATION);
    }

    // =========================================================================
    // Paso 58 — Test JPQL por intervalo
    // =========================================================================
    @Test
    @DisplayName("JPQL: tratamientos entre dos fechas")
    void shouldFindTreatmentsInDateRange() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal = createAnimalWithCase(center, "AN-001", "Green Sea Turtle", "RES-001");
        Specialist elena = createSpecialistAndSave("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");

        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION);

        List<Treatment> treatments = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertThat(treatments).hasSize(1);
        assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.HYDRATION);
    }

    // =========================================================================
    // Paso 59 — Probar UNIQUE
    // =========================================================================
    @Test
    @DisplayName("UNIQUE violation: segundo animal con mismo código lanza excepción")
    void shouldThrowOnDuplicateAnimalCode() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        createAnimalWithCase(center, "AN-100", "Turtle", "RES-001");

        Animal duplicate = new Animal();
        duplicate.setAnimalCode("AN-100");
        duplicate.setCommonName("Another Turtle");
        duplicate.setScientificName("Caretta caretta");
        duplicate.setSex(AnimalSex.MALE);
        duplicate.setRescueCase(createCase(center, "RES-002", RescueStatus.ADMITTED));

        assertThatThrownBy(() -> {
            animalRepository.saveAndFlush(duplicate);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    // =========================================================================
    // Paso 60 — Probar FK
    // =========================================================================
    @Test
    @DisplayName("FK violation: tratamiento sin animal inválido")
    void shouldThrowOnInvalidForeignKey() {
        Specialist specialist = createSpecialistAndSave("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");

        Treatment treatment = new Treatment();
        treatment.setAnimal(null);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(LocalDateTime.now());
        treatment.setType(TreatmentType.OBSERVATION);
        treatment.setDescription("Test");

        assertThatThrownBy(() -> {
            treatmentRepository.saveAndFlush(treatment);
        }).isInstanceOf(Exception.class);
    }

    // =========================================================================
    // Paso 61 — Probar CHECK
    // =========================================================================
    @Test
    @DisplayName("CHECK violation: estado inválido en rescue_cases")
    void shouldThrowOnInvalidStatusCheck() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-CHECK");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 1));
        rescueCase.setRescueLocation("Test");
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);
        rescueCaseRepository.save(rescueCase);

        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                    "UPDATE rescue_cases SET status = 'INVALID_STATUS' WHERE id = ?",
                    rescueCase.getId()
            );
        }).hasMessageContaining("violates check constraint");
    }

    // =========================================================================
    // Paso 62-64 — V3: tracking device
    // =========================================================================
    @Test
    @DisplayName("V3: tracking_device_code debe ser nullable y único")
    void shouldSupportTrackingDevice() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal1 = createAnimalWithCase(center, "AN-GPS-001", "Turtle", "RES-GPS-001");
        animal1.setTrackingDeviceCode("GPS-001");
        animalRepository.save(animal1);

        Animal animal2 = createAnimalWithCase(center, "AN-GPS-002", "Turtle 2", "RES-GPS-002");
        animal2.setTrackingDeviceCode(null);
        animalRepository.save(animal2);

        Animal found1 = animalRepository.findById(animal1.getId()).orElseThrow();
        Animal found2 = animalRepository.findById(animal2.getId()).orElseThrow();

        assertThat(found1.getTrackingDeviceCode()).isEqualTo("GPS-001");
        assertThat(found2.getTrackingDeviceCode()).isNull();
    }

    // =========================================================================
    // Paso 65 — Reto integrador: persistir escenario completo
    // =========================================================================
    @Test
    @DisplayName("Reto integrador: persistir escenario DeepBlue Caribbean completo")
    void shouldPersistCompleteScenario() {
        // Centro
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        // Caso
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 18));
        rescueCase.setRescueLocation("Bahía Concha");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);
        rescueCaseRepository.save(rescueCase);

        // Animal
        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE);
        animal.setRescueCase(rescueCase);
        animalRepository.save(animal);

        // Medical Record
        MedicalRecord record = new MedicalRecord();
        record.setAnimal(animal);
        record.setInitialWeight(new BigDecimal("27.80"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Injury caused by fishing net");
        record.setObservations("Possible plastic ingestion");
        animal.assignMedicalRecord(record);
        animalRepository.flush();

        // Especialista con expertise
        Expertise marineReptiles = findOrCreateExpertise("Marine Reptiles");
        Expertise trauma = findOrCreateExpertise("Trauma");
        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Tratamientos
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 19, 14, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");

        // Verificaciones
        Animal found = animalRepository.findById(animal.getId()).orElseThrow();
        assertThat(found.getRescueCase().getRescueCenter().getCode()).isEqualTo("DB-CAR");
        assertThat(found.getMedicalRecord().getInitialWeight()).isEqualByComparingTo("27.80");
        assertThat(found.getRescueCase().getStatus()).isEqualTo(RescueStatus.IN_REHABILITATION);
    }

    // =========================================================================
    // Paso 65-66 — Consultas del escenario
    // =========================================================================
    @Test
    @DisplayName("Consulta 1: existe caso RES-2026-100")
    void shouldFindCaseByCode() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        createCase(center, "RES-2026-100", RescueStatus.IN_REHABILITATION);

        Optional<RescueCase> found = rescueCaseRepository.findByCaseCode("RES-2026-100");
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("Consulta 2: obtener casos IN_REHABILITATION")
    void shouldFindCasesInRehabilitation() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        createCase(center, "RES-A", RescueStatus.IN_REHABILITATION);
        createCase(center, "RES-B", RescueStatus.READY_FOR_RELEASE);
        createCase(center, "RES-C", RescueStatus.IN_REHABILITATION);

        List<RescueCase> cases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(cases).hasSize(2);
    }

    @Test
    @DisplayName("Consulta 3: animales del centro DB-CAR")
    void shouldFindAnimalsByCenter() {
        RescueCenter car = createCenter("DB-CAR", "Caribbean");
        RescueCenter pac = createCenter("DB-PAC", "Pacific");
        createAnimalWithCase(car, "AN-001", "Turtle", "RES-001");
        createAnimalWithCase(pac, "AN-002", "Seal", "RES-002");

        List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(animals).hasSize(1);
        assertThat(animals.get(0).getAnimalCode()).isEqualTo("AN-001");
    }

    @Test
    @DisplayName("Consulta 4: animales cuyo nombre contenga 'turtle' (case insensitive)")
    void shouldFindAnimalsByNameContaining() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        createAnimalWithCase(center, "AN-001", "Green Sea Turtle", "RES-001");
        createAnimalWithCase(center, "AN-002", "Leatherback Turtle", "RES-002");
        createAnimalWithCase(center, "AN-003", "Dolphin", "RES-003");

        List<Animal> animals = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(animals).hasSize(2);
    }

    @Test
    @DisplayName("Consulta 5: especialistas con experiencia en Trauma")
    void shouldFindSpecialistsByExpertise() {
        Expertise trauma = findOrCreateExpertise("Trauma");
        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");

        Specialist elena = createSpecialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Specialist mateo = createSpecialist("SPEC-M", "Mateo", "Ríos", "mateo@deepblue.org");
        mateo.addExpertise(rehabilitation);
        specialistRepository.save(mateo);

        List<Specialist> specialists = specialistRepository.findActiveByExpertiseIgnoreCase("Trauma");
        assertThat(specialists).hasSize(1);
        assertThat(specialists.get(0).getFirstName()).isEqualTo("Elena");
    }

    @Test
    @DisplayName("Consulta 6: tratamientos de AN-2026-100 ordenados cronológicamente")
    void shouldFindTreatmentsByAnimalOrdered() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal = createAnimalWithCase(center, "AN-001", "Turtle", "RES-001");
        Specialist elena = createSpecialistAndSave("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");

        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 2, 10, 0), TreatmentType.WOUND_CARE);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 5, 10, 0), TreatmentType.HYDRATION);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.OBSERVATION);

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).hasSize(3);
    }

    @Test
    @DisplayName("Consulta 7: tratamientos por especialistas con experiencia en Rehabilitation")
    void shouldFindTreatmentsBySpecialistExpertise() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal = createAnimalWithCase(center, "AN-001", "Turtle", "RES-001");

        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");
        Specialist elena = createSpecialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 2, 10, 0), TreatmentType.WOUND_CARE);

        List<Treatment> treatments = treatmentRepository.findBySpecialistExpertiseAreasNameIgnoreCase("Rehabilitation");
        assertThat(treatments).hasSize(1);
    }

    @Test
    @DisplayName("Consulta 8: tratamientos entre dos fechas")
    void shouldFindTreatmentsBetweenDates() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Animal animal = createAnimalWithCase(center, "AN-001", "Turtle", "RES-001");
        Specialist elena = createSpecialistAndSave("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");

        createTreatment(animal, elena, LocalDateTime.of(2026, 7, 15, 10, 0), TreatmentType.WOUND_CARE);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION);
        createTreatment(animal, elena, LocalDateTime.of(2026, 8, 25, 10, 0), TreatmentType.OBSERVATION);

        List<Treatment> treatments = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 15, 23, 59)
        );

        assertThat(treatments).hasSize(1);
    }

    // =========================================================================
    // Paso 75-76 — Reto sin guía
    // =========================================================================
    @Test
    @DisplayName("Reto: animales en rehabilitación con tratamientos por especialistas de Trauma")
    void shouldFindAnimalsInRehabWithTraumaTreatments() {
        RescueCenter center = createCenter("DB-CAR", "Caribbean");
        Expertise trauma = findOrCreateExpertise("Trauma");
        Expertise rehabilitation = findOrCreateExpertise("Rehabilitation");

        // Caso en rehabilitación
        Animal animal1 = createAnimalWithCase(center, "AN-REHAB", "Green Turtle", "RES-REHAB");

        // Caso cerrado (no debería aparecer)
        Animal animal2 = createAnimalWithCase(center, "AN-CLOSED", "Loggerhead Turtle", "RES-CLOSED");
        RescueCase closedCase = animal2.getRescueCase();
        closedCase.setStatus(RescueStatus.CLOSED);
        rescueCaseRepository.save(closedCase);

        // Especialista con Trauma
        Specialist elena = createSpecialist("SPEC-E", "Elena", "Vargas", "elena@deepblue.org");
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Especialista sin Trauma
        Specialist mateo = createSpecialist("SPEC-M", "Mateo", "Ríos", "mateo@deepblue.org");
        mateo.addExpertise(rehabilitation);
        specialistRepository.save(mateo);

        // Tratamiento de animal1 por Elena (Trauma)
        createTreatment(animal1, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.WOUND_CARE);
        // Tratamiento de animal1 por Mateo (sin Trauma)
        createTreatment(animal1, mateo, LocalDateTime.of(2026, 8, 11, 10, 0), TreatmentType.HYDRATION);
        // Tratamiento de animal2 por Elena (Trauma, pero caso cerrado)
        createTreatment(animal2, elena, LocalDateTime.of(2026, 8, 12, 10, 0), TreatmentType.WOUND_CARE);

        List<Animal> animals = animalRepository
                .findInRehabilitationWithTreatmentsByExpertise(RescueStatus.IN_REHABILITATION, "Trauma");

        assertThat(animals).hasSize(1);
        assertThat(animals.get(0).getAnimalCode()).isEqualTo("AN-REHAB");
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private RescueCenter createCenter(String code, String name) {
        RescueCenter center = new RescueCenter();
        center.setCode(code);
        center.setName("DeepBlue " + name);
        center.setCity(name);
        return rescueCenterRepository.save(center);
    }

    private RescueCase createCase(RescueCenter center, String code, RescueStatus status) {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode(code);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 1));
        rescueCase.setRescueLocation("Location");
        rescueCase.setStatus(status);
        rescueCase.setRescueCenter(center);
        return rescueCaseRepository.save(rescueCase);
    }

    private Animal createAnimalWithCase(RescueCenter center, String animalCode, String commonName, String caseCode) {
        RescueCase rescueCase = createCase(center, caseCode, RescueStatus.IN_REHABILITATION);
        Animal animal = new Animal();
        animal.setAnimalCode(animalCode);
        animal.setCommonName(commonName);
        animal.setScientificName("Scientific name");
        animal.setSex(AnimalSex.MALE);
        animal.setRescueCase(rescueCase);
        return animalRepository.save(animal);
    }

    private Specialist createSpecialistAndSave(String code, String firstName, String lastName, String email) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode(code);
        specialist.setFirstName(firstName);
        specialist.setLastName(lastName);
        specialist.setEmail(email);
        return specialistRepository.save(specialist);
    }

    private Specialist createSpecialist(String code, String firstName, String lastName, String email) {
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode(code);
        specialist.setFirstName(firstName);
        specialist.setLastName(lastName);
        specialist.setEmail(email);
        return specialist;
    }

    private void createTreatment(Animal animal, Specialist specialist, LocalDateTime date, TreatmentType type) {
        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(date);
        treatment.setType(type);
        treatment.setDescription("Treatment description");
        treatmentRepository.save(treatment);
    }

    private void createTreatment(Animal animal, Specialist specialist, LocalDateTime date, TreatmentType type, String description) {
        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(date);
        treatment.setType(type);
        treatment.setDescription(description);
        treatmentRepository.save(treatment);
    }

    private Expertise findOrCreateExpertise(String name) {
        return expertiseRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Expertise expertise = new Expertise();
                    expertise.setName(name);
                    return expertiseRepository.save(expertise);
                });
    }
}
