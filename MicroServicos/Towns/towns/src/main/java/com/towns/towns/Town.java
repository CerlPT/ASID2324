package com.towns.towns;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Town {
    @Id
    private int id;
    private String name;
    private String countryName;
    private int popSize;
    
    // Getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    
    public int getPopSize() {
        return popSize;
    }

    public void setPopSize(int popSize) {
        this.popSize = popSize;
    }

}


