package ca.jusjoken.data.entity;

import ca.jusjoken.component.Badge;
import ca.jusjoken.component.Layout;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.service.LocalDateCsvConverter;
import ca.jusjoken.data.service.LocalDateCsvConverterDDMMYYYY;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.utility.AgeBetween;
import ca.jusjoken.utility.BadgeVariant;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import org.hibernate.annotations.Formula;

@Entity
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    private Boolean breeder = false;
    @ManyToOne
    private StockType stockType;  //rabbit, goat, pig, cow etc.

    @Transient
    @CsvBindByName(column = "Sex")
    private String sexText;  //fro import only - convert to Gender

    private Gender sex;

    @CsvBindByName(column = "Prefix")
    private String prefix;

    @CsvBindByName(column = "Name")
    private String name;
    @CsvBindByName(column = "Tattoo")
    private String tattoo;
    @CsvBindByName(column = "Cage")
    private String cage;

    @Transient
    @CsvBindByName(column = "Father")
    private String fatherName;  //name of the father for import only
    @Transient
    @CsvBindByName(column = "Mother")
    private String motherName;  //name of the mother for import

    private Long fatherId;  //id of the father
    private Long motherId;  //id of the mother

    @CsvBindByName(column = "Color")
    private String color;  //list of valid colors
    @CsvBindByName(column = "Breed")
    private String breed;

    @Transient
    @CsvBindByName(column = "Weight")
    private String weightText;  //weight used for import only - needs converting

    private Integer weight;  //weight in Oz (convert to lbs/oz for display)

    @CsvCustomBindByName(column = "DoB", converter = LocalDateCsvConverter.class)
    private LocalDate doB;

    @CsvCustomBindByName(column = "Acquired", converter = LocalDateCsvConverter.class)
    private LocalDate acquired;

    @CsvBindByName(column = "Reg #")
    private String regNo;
    @CsvBindByName(column = "Champ #")
    private String champNo;
    @CsvBindByName(column = "Legs")
    private String legs;
    @CsvBindByName(column = "Genotype")
    private String genotype;  //TODO: needs a string builder for genotypes

    //private Integer littersCount;   // make a getter and return a count
    private Integer kitsCount;   // make a getter and return a count

    @CsvBindByName(column = "Category")
    private String category;
    @CsvBindByName(column = "Notes")
    private String notes;

    //TODO: status likely needs to be a child table so a history can be kept
    //change to an enumeration
    @CsvBindByName(column = "Status")
    private String status;  //need to be a list of valid status'
    @CsvCustomBindByName(column = "Status Date", converter = LocalDateCsvConverterDDMMYYYY.class)
    private LocalDate statusDate;

    //@Transient
    private Boolean active;
    
    @Transient
    private String defaultImageSource = "images/rabbit_blank.jpg";

    @ManyToOne
    @JoinColumn(name = "litterId")
    private Litter litter;

    public Litter getLitter() {
        return litter;
    }

    public void setLitter(Litter litter) {
        this.litter = litter;
    }

    //private Long litterForsterId;

    @ManyToOne
    @JoinColumn(name = "fosterLitterId")
    private Litter fosterLitter;

    public Litter getFosterLitter() {
        return fosterLitter;
    }

    public void setFosterLitter(Litter litter) {
        this.fosterLitter = litter;
    }
    
    @Transient
    private Boolean needsSaving = Boolean.FALSE;

    @Formula("(SELECT "
            + "CASE "
            + " WHEN s.status IN ('archived', 'butchered', 'culled', 'died','sold') AND s.status_date IS NOT NULL THEN "
            + " (DATEDIFF(s.status_date, doB)) "
            + " ELSE "
            + " (DATEDIFF(CURRENT_DATE, doB)) "
            + " END "
            + " FROM stock s "
            + " WHERE s.id = id)")
    private Long ageInDays;

    public Long getAgeInDays() {
        return ageInDays;
    }

    public String getAgeInDaysStr() {
        String ageStr = Utility.emptyValue;
        if (this.ageInDays == null) {
            return ageStr;
        }
        if (!this.ageInDays.equals(0L)) {
            ageStr = String.valueOf(ageInDays);
        }
        return ageStr;
    }
    
    @Formula("(SELECT "
            + "CASE "
            + " WHEN s.sex = 'F' THEN "
            + " (SELECT COUNT(*) FROM litter l WHERE s.id = l.mother_id ) "
            + " WHEN s.sex = 'M' THEN "
            + " (SELECT COUNT(*) FROM litter l WHERE s.id = l.father_id ) "
            + " END "
            + " FROM stock s "
            + " WHERE s.id = id)")
    private Long litterCount;

    @Formula("(SELECT "
            + "CASE "
            + " WHEN s.sex = 'F' THEN "
            + " (SELECT COUNT(*) FROM stock st WHERE s.id = st.mother_id ) "
            + " WHEN s.sex = 'M' THEN "
            + " (SELECT COUNT(*) FROM stock st WHERE s.id = st.father_id ) "
            + " END "
            + " FROM stock s "
            + " WHERE s.id = id)")
    private Long kitCount;

    public Long getKitCount() {
        return kitCount;
    }

    public String getKitCountStr() {
        String kitCountStr = Utility.emptyValue;
        if (this.kitCount == null) {
            return kitCountStr;
        }
        if (!this.kitCount.equals(0L)) {
            kitCountStr = String.valueOf(kitCount);
        }
        return kitCountStr;
    }
    
    public Long getLitterCount() {
        return litterCount;
    }
    
    public String getLitterCountStr() {
        String litterCountStr = Utility.emptyValue;
        if (this.litterCount == null) {
            return litterCountStr;
        }
        if (!this.litterCount.equals(0L)) {
            litterCountStr = String.valueOf(litterCount);
        }
        return litterCountStr;
    }

    public Stock() {
        this.active = isActive();
    }

    public Stock(String name, Boolean active, Boolean breeder, StockType stockType) {
        this.name = name;
        this.active = active;
        this.breeder = breeder;
        this.stockType = stockType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Gender getSex() {
        return sex;
    }

    public void setSex(Gender sex) {
        this.sex = sex;
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

    public String getTattoo() {
        return tattoo;
    }

    public void setTattoo(String tattoo) {
        this.tattoo = tattoo;
    }

    public String getCage() {
        return cage;
    }

    public void setCage(String cage) {
        this.cage = cage;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public LocalDate getDoB() {
        return doB;
    }

    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }

    public AgeBetween getAge() {
        LocalDate endDate = LocalDate.now();
        if (getStockStatus().stopsAgeCalculation() && getStatusDate() != null) {
            endDate = getStatusDate();
        }
        return new AgeBetween(this.doB, endDate);
    }

    public LocalDate getAcquired() {
        return acquired;
    }

    public void setAcquired(LocalDate acquired) {
        this.acquired = acquired;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getChampNo() {
        return champNo;
    }

    public void setChampNo(String champNo) {
        this.champNo = champNo;
    }

    public String getLegs() {
        return legs;
    }

    public void setLegs(String legs) {
        this.legs = legs;
    }

    public String getGenotype() {
        return genotype;
    }

    public void setGenotype(String genotype) {
        this.genotype = genotype;
    }

    public Integer getKitsCount() {
        return kitsCount;
    }

    public void setKitsCount(Integer kitsCount) {
        this.kitsCount = kitsCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        if(this.notes==null) return "";
        return notes.replace("[", "").replace("]", "").replace("\"", "");
    }
    
    public Boolean hasNotes(){
        return !getNotes().isEmpty();
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StockStatus getStockStatus() {
        return Utility.getInstance().getStockStatus(getStatus());
    }

    public LocalDate getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(LocalDate statusDate) {
        this.statusDate = statusDate;
    }

    public Boolean isBreeder() {
        return breeder;
    }

    public void setBreeder(Boolean breeder) {
        this.breeder = breeder;
    }

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public String getSexText() {
        return sexText;
    }

    public void setSexText(String sexText) {
        this.sexText = sexText;
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

    private String getNameWithoutTattoo(String inVal) {
        String retVal = "";
        if (inVal.contains("(")) {
            retVal = inVal.substring(0, inVal.indexOf("("));
        } else {
            retVal = inVal;
        }
        return retVal;
    }

    private String getTattooFromName(String inVal) {
        String retVal = "";
        if (inVal.contains("()")) {
            retVal = "";
        } else {
            retVal = inVal.substring(inVal.indexOf("(") + 1, inVal.indexOf(")"));
        }
        return retVal;
    }

    public Long getFatherId() {
        return fatherId;
    }

    public void setFatherId(Long fatherId) {
        this.fatherId = fatherId;
    }

    public Long getMotherId() {
        return motherId;
    }

    public void setMotherId(Long motherId) {
        this.motherId = motherId;
    }

    public String getWeightText() {
        return weightText;
    }

    public void setWeightText(String weightText) {
        this.weightText = weightText;
    }

    public String getWeightInLbsOz() {
        if (getWeight() > 0) {
            return Utility.getInstance().WeightConverterOzToHTML(getWeight());
        }
        return "<p>" + Utility.emptyValue + "</p>";
    }

    public Boolean getNeedsSaving() {
        return needsSaving;
    }

    public void setNeedsSaving(Boolean needsSaving) {
        this.needsSaving = needsSaving;
    }

    public String getDisplayName() {
        if (getName().isEmpty()) {
            return getTattoo();
        }
        return getName();
    }

    public final Boolean isActive() {
        if (getStatus() == null || getStatus().isEmpty()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDefaultImageSource() {
        return defaultImageSource;
    }

    public void setDefaultImageSource(String defaultImageSource) {
        this.defaultImageSource = defaultImageSource;
    }
    
    public SvgIcon getGenderIcon(){
        if(getSex().equals(Utility.Gender.FEMALE)){
            return new SvgIcon(Utility.ICONS.GENDER_FEMALE.getIconSource());
        }else if(getSex().equals(Utility.Gender.MALE)){
            return new SvgIcon(Utility.ICONS.GENDER_MALE.getIconSource());
        }else{
            return null;
        }
    }

    public Layout getHeader(){
        // Header
        Layout header = new Layout();
        header.setAlignItems(Layout.AlignItems.CENTER);
        header.setGap(Layout.Gap.SMALL);
        header.setWidthFull();
        header.setFlexWrap(Layout.FlexWrap.WRAP);
        if(!getPrefix().isEmpty()){
            Badge badge = new Badge(getPrefix());
            badge.addThemeVariants(BadgeVariant.PILL, BadgeVariant.SMALL);
            header.add(badge);
        }
        Span stockName = new Span(getDisplayName());
        stockName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);
        header.add(stockName);
        //add icons
        if(getSex().equals(Gender.FEMALE)){
            header.add(showIcon(getStockType().getFemaleName(),Utility.ICONS.GENDER_FEMALE.getIconSource()));
        }else if(getSex().equals(Gender.MALE)){
            header.add(showIcon(getStockType().getMaleName(),Utility.ICONS.GENDER_MALE.getIconSource()));
        }
        if(isBreeder()){
            header.add(showIcon(getStockType().getBreederName(),Utility.ICONS.TYPE_BREEDER.getIconSource()));
        }
        if(Utility.getInstance().hasStockStatus(getStatus())){
            StockStatus stockStatus = Utility.getInstance().getStockStatus(getStatus());
            header.add(showIcon(stockStatus.getLongName(),stockStatus.getIcon().getIconSource()));
        }
        return header;
    }

    private Layout showIcon(String text, String icon) {
        Layout layout = new Layout();
        layout.setAlignItems(Layout.AlignItems.CENTER);
        layout.setGap(Layout.Gap.SMALL);

        SvgIcon svgIcon = new SvgIcon(icon);
        svgIcon.setTooltipText(text);
        svgIcon.addClassNames(LumoUtility.IconSize.SMALL);
        layout.add(svgIcon);
        return layout;
    }
    
    public void updateFromImported(Boolean importBreeder, Long importId, StockType importStockType) {
        //only allow this update if the id is null as imported records do not have an id
        needsSaving = Boolean.FALSE;
        if (id != null) {
            System.out.println("***Attempted update from non-imported stock record id:" + id + ". Aborted.");
            return;
        }
        //if an id is passed in then this is an update otherwise it's a new record
        if (importId != null) {
            setId(importId);
        }
        setBreeder(importBreeder);

        if (getSexText().equals("buck")) {
            setSex(Utility.Gender.MALE);
        } else if (getSexText().equals("doe")) {
            setSex(Utility.Gender.FEMALE);
        } else {
            setSex(Utility.Gender.NA);
        }

        setStockType(importStockType);

        //convert imported weight to oz
        if (getWeightText().isEmpty()) {
            setWeight(0);
        } else {
            setWeight(Utility.getInstance().WeightConverterStringToOz(getWeightText()));
        }

        //set active based on status
        active = isActive();
        needsSaving = Boolean.TRUE;
    }

    @Override
    public String toString() {
        return "Stock{" + "id=" + id + ", breeder=" + breeder + ", stockType=" + stockType + ", sexText=" + sexText + ", sex=" + sex + ", prefix=" + prefix + ", name=" + name + ", tattoo=" + tattoo + ", cage=" + cage + ", fatherName=" + fatherName + ", motherName=" + motherName + ", fatherId=" + fatherId + ", motherId=" + motherId + ", color=" + color + ", breed=" + breed + ", weightText=" + weightText + ", weight=" + weight + ", doB=" + doB + ", acquired=" + acquired + ", regNo=" + regNo + ", champNo=" + champNo + ", legs=" + legs + ", genotype=" + genotype + ", kitsCount=" + kitsCount + ", category=" + category + ", notes=" + notes + ", status=" + status + ", statusDate=" + statusDate + ", active=" + active + ", litter=" + litter + ", fosterLitter=" + fosterLitter + ", needsSaving=" + needsSaving + ", ageInDays=" + ageInDays + ", litterCount=" + litterCount + ", kitCount=" + kitCount + '}';
    }

    
}
