/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;

/**
 *
 * @author birch
 */
@Entity
public class StockType {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    private String name;
    private String nameSingular;
    private String maleName;
    private String femaleName;
    private String breederName;  //name like breeder for a rabbit if breeder is true
    private String nonBreederName;  //name like kit for a rabbit if breeder is false
    private Boolean defaultType;
    private String imageFileName;

    public StockType() {
    }

    public StockType(String name, String nameSingular, String maleName, String femaleName, String breederName, String nonBreederName, Boolean defaultType) {
        this.name = name;
        this.nameSingular = nameSingular;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getNameSingular() {
        return nameSingular;
    }

    public void setNameSingular(String nameSingular) {
        this.nameSingular = nameSingular;
    }

    public String getImageFileName() {
        if(imageFileName==null) return "farmtracks_blank.jpg";
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    @Override
    public String toString() {
        return "StockType{" + "id=" + id + ", name=" + name + ", nameSingular=" + nameSingular + ", maleName=" + maleName + ", femaleName=" + femaleName + ", breederName=" + breederName + ", nonBreederName=" + nonBreederName + ", defaultType=" + defaultType + '}';
    }

}
