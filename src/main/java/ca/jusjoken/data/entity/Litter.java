/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.service.LocalDateCsvConverter;
import ca.jusjoken.data.service.LocalDateTimeCsvConverter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    
    public String getParentsFormatted(){
        String motherName = "Unknown";
        if(getMother()!=null) {
            motherName = getMother().getName();
        }
        String fatherName = "Unknown";
        if(getFather()!=null) {
            fatherName = getFather().getName();
        }
        return motherName + "/" + fatherName;
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
            retVal = inVal.substring(0, inVal.indexOf("("));
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
            retVal = inVal.substring(inVal.indexOf("(")+1, inVal.indexOf(")"));
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

    @Override
    public String toString() {
        return "Litter{" + "id=" + id + ", prefix=" + prefix + ", name=" + name + ", breed=" + breed + ", cage=" + cage + ", fatherName=" + fatherName + ", motherName=" + motherName + ", father=" + father + ", mother=" + mother + ", bred=" + bred + ", doB=" + doB + ", kitsCount=" + kitsCount + ", soldKitsCount=" + soldKitsCount + ", diedKitsCount=" + diedKitsCount + ", litterWeightText=" + litterWeightText + ", litterWeight=" + litterWeight + ", archived=" + archived + ", notes=" + notes + ", needsSaving=" + needsSaving + '}';
    }


}
