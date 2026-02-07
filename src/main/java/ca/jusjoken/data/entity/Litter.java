/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.service.LocalDateCsvConverter;
import ca.jusjoken.data.service.LocalDateTimeCsvConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 *
 * @author birch
 */
@Entity
public class Litter {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)    
    private Integer id;

    @ManyToOne
    private StockType stockType;  //rabbit, goat, pig, cow etc.

    @CsvBindByName(column = "Prefix")
    private String prefix;

    @CsvBindByName(column = "Name")
    private String name;
    
    @CsvBindByName(column = "Breed")
    private String breed;

    @CsvBindByName(column = "Cage")
    private String cage;

    @Transient
    @CsvBindByName(column = "Father")
    private String fatherName;  //name of the father for import only
    @Transient
    @CsvBindByName(column = "Mother")
    private String motherName;  //name of the mother for import
    
    //private Long fatherId;  //id of the father
    //private Long motherId;  //id of the mother
    
    @ManyToOne
    @JoinColumn(name = "father_id", referencedColumnName = "id", nullable = false,foreignKey = @jakarta.persistence.ForeignKey(name = "none"))
    private Stock father;
    
    @ManyToOne
    @JoinColumn(name = "mother_id", referencedColumnName = "id", nullable = false,foreignKey = @jakarta.persistence.ForeignKey(name = "none"))
    private Stock mother;

    public Stock getFather() {
        return father;
    }

    public void setFather(Stock father) {
        this.father = father;
    }

    public Stock getMother() {
        return mother;
    }

    public void setMother(Stock mother) {
        this.mother = mother;
    }
    
    public String getDisplayName(){
        return getDoB().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " (" + getParentsFormatted() + ") - " + getName();
    }

    
    
    public String getParentsFormatted(){
        String motherNameStr = "Unknown";
        if(getMother()!=null) {
            motherNameStr = getMother().getName();
        }
        String fatherNameStr = "Unknown";
        if(getFather()!=null) {
            fatherNameStr = getFather().getName();
        }
        return motherNameStr + "/" + fatherNameStr;
    }
    
    @CsvCustomBindByName(column = "Bred", converter = LocalDateCsvConverter.class)
    private LocalDate bred;
    
    @CsvCustomBindByName(column = "Born", converter = LocalDateCsvConverter.class)
    private LocalDate doB;
    
    @CsvBindByName(column = "Total Kits")
    private Integer kitsCount;   // make a getter and return a count
    @CsvBindByName(column = "Sold Kits")
    private Integer soldKitsCount;   // make a getter and return a count
    @CsvBindByName(column = "Died Kits")
    private Integer diedKitsCount;   // make a getter and return a count

    @Transient
    @CsvBindByName(column = "Total Weight")
    private String litterWeightText;  //weight used for import only - needs converting
    
    private Integer litterWeight;  //weight in Oz (convert to lbs/oz for display)

    @CsvCustomBindByName(column = "Archived", converter = LocalDateTimeCsvConverter.class)
    private LocalDateTime archived;

    @CsvBindByName(column = "Notes")
    private String notes;

    @Transient
    private Boolean needsSaving = Boolean.FALSE;
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getCage() {
        return cage;
    }

    public void setCage(String cage) {
        this.cage = cage;
    }

   public String getFatherName() {
        return fatherName;
    }

    public String getFatherNameWithoutTattoo() {
        return getNameWithoutTattoo(fatherName);
    }
    
    public String getFatherTattoo() {
        return getTattooFromName(fatherName);
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public String getMotherNameWithoutTattoo() {
        return getNameWithoutTattoo(motherName);
    }
    
    public String getMotherTattoo() {
        return getTattooFromName(motherName);
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    private String getNameWithoutTattoo(String inVal){
        String retVal = "";
        if(inVal.contains("(")){
            retVal = inVal.substring(0, inVal.lastIndexOf("("));
        }else{
            retVal = inVal;
        }
        return retVal;
    }
    
    private String getTattooFromName(String inVal){
        String retVal = "";
        if(inVal.contains("()")){
            retVal = "";
        }else{
            retVal = inVal.substring(inVal.lastIndexOf("(")+1, inVal.lastIndexOf(")"));
        }
        return retVal;
    }

    public LocalDate getBred() {
        return bred;
    }

    public void setBred(LocalDate bred) {
        this.bred = bred;
    }

    public LocalDate getDoB() {
        return doB;
    }

    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }

    public Integer getKitsCount() {
        return kitsCount;
    }

    public void setKitsCount(Integer kitsCount) {
        this.kitsCount = kitsCount;
    }

    public Integer getSoldKitsCount() {
        return soldKitsCount;
    }

    public void setSoldKitsCount(Integer soldKitsCount) {
        this.soldKitsCount = soldKitsCount;
    }

    public Integer getDiedKitsCount() {
        return diedKitsCount;
    }

    public void setDiedKitsCount(Integer diedKitsCount) {
        this.diedKitsCount = diedKitsCount;
    }
    
    public String getSurvivalRate(){
        float survival = (getKitsSurvivedCount() * 100f) / ((getKitsSurvivedCount()*100f) + (getDiedKitsCount()*100f));
        
        DecimalFormat df = new DecimalFormat("0%");
        return df.format((Math.max(0f, survival)));
    }

    public String getLitterWeightText() {
        return litterWeightText;
    }

    public void setLitterWeightText(String litterWeightText) {
        this.litterWeightText = litterWeightText;
    }

    public Integer getLitterWeight() {
        return litterWeight;
    }

    public void setLitterWeight(Integer litterWeight) {
        this.litterWeight = litterWeight;
    }
    
    public String getLitterWeightInLbsOz(){
        return Utility.getInstance().WeightConverterOzToString(getLitterWeight());
    }

    public Integer getLitterAverageWeight(){
        return getLitterWeight() / getKitsSurvivedCount();
    }
    
    public Integer getKitsSurvivedCount(){
        return (getKitsCount() - getDiedKitsCount());
    }
    
    public String getLitterAverageWeightInLbsOz(){
        return Utility.getInstance().WeightConverterOzToString(getLitterAverageWeight());
    }

    public LocalDateTime getArchived() {
        return archived;
    }

    public void setArchived(LocalDateTime archived) {
        this.archived = archived;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getNeedsSaving() {
        return needsSaving;
    }

    public void setNeedsSaving(Boolean needsSaving) {
        this.needsSaving = needsSaving;
    }

    public Boolean getActive() {
        return archived==null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
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
        final Litter other = (Litter) obj;
        return Objects.equals(this.id, other.id);
    }
    
    

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Litter{");
        sb.append("id=").append(id);
        sb.append(", stockType=").append(stockType);
        sb.append(", prefix=").append(prefix);
        sb.append(", name=").append(name);
        sb.append(", breed=").append(breed);
        sb.append(", cage=").append(cage);
        sb.append(", fatherName=").append(fatherName);
        sb.append(", motherName=").append(motherName);
        sb.append(", father=").append(father);
        sb.append(", mother=").append(mother);
        sb.append(", bred=").append(bred);
        sb.append(", doB=").append(doB);
        sb.append(", kitsCount=").append(kitsCount);
        sb.append(", soldKitsCount=").append(soldKitsCount);
        sb.append(", diedKitsCount=").append(diedKitsCount);
        sb.append(", litterWeightText=").append(litterWeightText);
        sb.append(", litterWeight=").append(litterWeight);
        sb.append(", archived=").append(archived);
        sb.append(", notes=").append(notes);
        sb.append(", needsSaving=").append(needsSaving);
        sb.append('}');
        return sb.toString();
    }


}
