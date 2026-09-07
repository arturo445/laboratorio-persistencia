package com.deepblue.rescue.domain;

<<<<<<< HEAD
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rescue_centers")
public class RescueCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @OneToMany(mappedBy = "rescueCenter")
    private List<RescueCase> rescueCases = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<RescueCase> getRescueCases() {
        return rescueCases;
    }

    public void setRescueCases(List<RescueCase> rescueCases) {
        this.rescueCases = rescueCases;
    }

    public void addCase(RescueCase rescueCase) {
        this.rescueCases.add(rescueCase);
        rescueCase.setRescueCenter(this);
=======
import java.util.ArrayList;
import java.util.List;

public class RescueCenter {
    @Entity
    @Table(name = "rescueCenter")
    public class RescueCenter {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column (nullable = false, unique = true, length = 50)
        private String code;

        @Column (nullable = false, length = 150)
        private String name;

        @Column (nullable = false, length = 100)
        private String city;

        @OneToMany(mappedBy = "rescueCenter")
        private List<RescueCase>  rescueCases = new ArrayList<>();

        protected RescueCenter (){
        }

        public RescueCenter(String code, String name, String city){
            this.code = code;
            this.name = name;
            this.city = city;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getCity() { return city; }
        public List<RescueCase> getRescueCases() { return List.copyOf(rescueCases) }

        public void addCase(RescueCase rescueCase) {
            rescueCases.add(rescueCase);
            rescueCase.setRescueCenter(this);
        }
>>>>>>> 4d56d7ac7b1dfb835bdd31edb07a955108f7f239
    }
}
