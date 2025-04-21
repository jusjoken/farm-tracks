/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;

/**
 *
 * @author birch
 */
@Entity
public class StockType {

    @Id
    private String name;
    private String maleName;
    private String femaleName;
    private String breederName;  //name like breeder for a rabbit if breeder is true
    private String nonBreederName;  //name like kit for a rabbit if breeder is false
    private Boolean defaultType;

    public StockType() {
    }

    public StockType(String name, String maleName, String femaleName, String breederName, String nonBreederName, Boolean defaultType) {
        this.name = name;
        this.maleName = maleName;
        this.femaleName = femaleName;
        this.breederName = breederName;
        this.nonBreederName = nonBreederName;
        this.defaultType = defaultType;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StockType other = (StockType) obj;
        return Objects.equals(this.name, other.name);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaleName() {
        return maleName;
    }

    public void setMaleName(String maleName) {
        this.maleName = maleName;
    }

    public String getFemaleName() {
        return femaleName;
    }

    public void setFemaleName(String femaleName) {
        this.femaleName = femaleName;
    }

    public String getBreederName() {
        return breederName;
    }

    public void setBreederName(String breederName) {
        this.breederName = breederName;
    }
    

    public String getNonBreederName() {
        return nonBreederName;
    }

    public void setNonBreederName(String nonBreederName) {
        this.nonBreederName = nonBreederName;
    }

    public Boolean getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(Boolean defaultType) {
        this.defaultType = defaultType;
    }
    
    
    @Override
    public String toString() {
        return "StockType{" + "name=" + name + ", maleName=" + maleName + ", femaleName=" + femaleName + ", nonBreederName=" + nonBreederName + '}';
    }
    
    
}
