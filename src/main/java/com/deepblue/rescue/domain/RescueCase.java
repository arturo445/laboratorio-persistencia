package com.deepblue.rescue.domain;

<<<<<<< HEAD
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true, length = 50)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false, length = 200)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    @OneToOne(mappedBy = "rescueCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Animal animal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public LocalDate getRescueDate() {
        return rescueDate;
    }

    public void setRescueDate(LocalDate rescueDate) {
        this.rescueDate = rescueDate;
    }

    public String getRescueLocation() {
        return rescueLocation;
    }

    public void setRescueLocation(String rescueLocation) {
        this.rescueLocation = rescueLocation;
    }

    public RescueStatus getStatus() {
        return status;
    }

    public void setStatus(RescueStatus status) {
        this.status = status;
    }

    public RescueCenter getRescueCenter() {
        return rescueCenter;
    }

    public void setRescueCenter(RescueCenter rescueCenter) {
        this.rescueCenter = rescueCenter;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    public void assignAnimal(Animal animal) {
        this.animal = animal;
        animal.setRescueCase(this);
    }
=======
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;

import java.util.ArrayList;
import java.util.List;

public class RescueCase {

    @Entity
    @Table(name = "rescueCase")
    public class RescueCase {

        @Id
        @GeneratedValue(strategy = GenereationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true, length = 50)
        private String case_code;

        @Column (nullable = false)
        private LocalDate rescue_date;

        @Column (nullable = false, length = 200)
        private String rescue_location;

        @Enumerated(EnumType.STRING)
        @Column (nullable = false, length = 30)
        private RescueStatus status;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "rescue_center_id", nullable = false)
        private RescueCenter rescueCenter;

        @OneToOne(
                mappedBy = "rescueCase",
                cascade = CascadeType.ALL,
                orphanRemoval = true,
                fetch = FetchType.LAZY
        )
        private Animal animal;

        protected RescueCase(){
        }

        private RescueCase (String case_code, LocalDate rescue_date, String rescue_location, RescueCenter rescue_center){
            this.case_code = case_code;
            this.rescue_date = rescue_date;
            this.rescue_location = rescue_location;
            this.rescue_center = rescue_center;
            this.status = RescueStatus.ACTIVE;
        }

        public void assignAnimal(Animal animal) {
            this.animal = animal;
            animal.setRescueCase(this);
        }
>>>>>>> 4d56d7ac7b1dfb835bdd31edb07a955108f7f239
}
