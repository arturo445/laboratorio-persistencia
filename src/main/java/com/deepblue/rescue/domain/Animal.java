package com.deepblue.rescue.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true, length = 50)
    private String animalCode;

    @Column(name = "common_name", nullable = false, length = 150)
    private String commonName;

    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnimalSex sex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", nullable = false, unique = true)
    private RescueCase rescueCase;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "animal")
    private List<Treatment> treatments = new ArrayList<>();

    protected Animal(){
    }

    public Animal(String animalCode, String commonName, String scientificName, AnimalSex sex, RescueCase rescueCase){
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
        this.rescueCase = rescueCase;
    }

    public Long getId() { return id; }
    public String getAnimalCode() { return animalCode; }
    public String getCommonName() { return commonName; }
    public String getScientificName() { return scientificName; }
    public AnimalSex getSex() { return sex; }
    public RescueCase getRescueCase() { return rescueCase; }
    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public List<Treatment> getTreatments() { return List.copyOf(treatments); }

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public void addTreatment(Treatment treatment) {
        treatments.add(treatment);
        treatment.setAnimal(this);
    }

}
