package com.deepblue.rescue.domain;

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
}
