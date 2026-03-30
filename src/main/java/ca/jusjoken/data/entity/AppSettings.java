package ca.jusjoken.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)    
    private Integer id;

    private String farmName = "Breza Homestead & Rabbitry";
    private String farmAddressLine1 = "RR5 Site 3 Box 21";
    private String farmAddressLine2 = "Rimbey Alberta T0C 2J0";
    private String farmEmail = "equidanes@hotmail.ca";
    private String farmPrefix = "Breza's";
    private String defaultLitterPrefix = "BHR";
    private String overrideNextLitterNumber = ""; //if set then this and the Prefix will be used as the next litter name instead of calculating the next litter number
    

    public AppSettings() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getFarmAddressLine1() {
        return farmAddressLine1;
    }

    public void setFarmAddressLine1(String farmAddressLine1) {
        this.farmAddressLine1 = farmAddressLine1;
    }

    public String getFarmAddressLine2() {
        return farmAddressLine2;
    }

    public void setFarmAddressLine2(String farmAddressLine2) {
        this.farmAddressLine2 = farmAddressLine2;
    }

    public String getFarmEmail() {
        return farmEmail;
    }

    public void setFarmEmail(String farmEmail) {
        this.farmEmail = farmEmail;
    }

    public String getFarmPrefix() {
        return farmPrefix;
    }

    public void setFarmPrefix(String farmPrefix) {
        this.farmPrefix = farmPrefix;
    }

    public String getDefaultLitterPrefix() {
        return defaultLitterPrefix;
    }

    public void setDefaultLitterPrefix(String defaultLitterPrefix) {
        this.defaultLitterPrefix = defaultLitterPrefix;
    }

    @Override
    public String toString() {
        return "AppSettings [id=" + id + ", farmName=" + farmName + ", farmAddressLine1=" + farmAddressLine1
                + ", farmAddressLine2=" + farmAddressLine2 + ", farmEmail=" + farmEmail + ", farmPrefix=" + farmPrefix
                + ", defaultLitterPrefix=" + defaultLitterPrefix + "]";
    }

    public String getOverrideNextLitterNumber() {
        return overrideNextLitterNumber;
    }

    public void setOverrideNextLitterNumber(String overrideNextLitterNumber) {
        this.overrideNextLitterNumber = overrideNextLitterNumber;
    }

}
