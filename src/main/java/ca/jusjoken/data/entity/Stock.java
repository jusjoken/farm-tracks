package ca.jusjoken.data.entity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.streams.DownloadHandler;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.AvatarDiv;
import ca.jusjoken.component.Badge;
import ca.jusjoken.component.Layout;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.service.LocalDateCsvConverter;
import ca.jusjoken.data.service.LocalDateCsvConverterDDMMYYYY;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.utility.AgeBetween;
import ca.jusjoken.utility.BadgeVariant;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    private Boolean breeder = false;
    @ManyToOne
    private StockType stockType;  //rabbit, goat, pig, cow etc.

    @Transient
    @CsvBindByName(column = "Sex")
    private String sexText;  //fro import only - convert to Gender

    private Gender sex = Gender.NA;

    @CsvBindByName(column = "Prefix")
    private String prefix = "";

    @CsvBindByName(column = "Name")
    private String name = "";
    @CsvBindByName(column = "Tattoo")
    private String tattoo = "";
    /*
    @CsvBindByName(column = "Cage")
    @Transient
    private String cage = "";
    */

    @Transient
    @CsvBindByName(column = "Father")
    private String fatherName;  //name of the father for import only
    @Transient
    @CsvBindByName(column = "Mother")
    private String motherName;  //name of the mother for import

    private Integer fatherId;  //id of the father
    private Integer motherId;  //id of the mother
    
    private String fatherExtName = null; //only used when the parent is not in the database
    private String motherExtName = null; //only used when the parent is not in the database

    @CsvBindByName(column = "Color")
    private String color = ""; 
    @CsvBindByName(column = "Breed")
    private String breed = "";

    @Transient
    @CsvBindByName(column = "Weight")
    private String weightText;  //weight used for import only - needs converting

    private Integer weight;  //weight in Oz (convert to lbs/oz for display)
    
    private LocalDateTime weightDate;

    @CsvCustomBindByName(column = "DoB", converter = LocalDateCsvConverter.class)
    private LocalDate doB;

    @CsvCustomBindByName(column = "Acquired", converter = LocalDateCsvConverter.class)
    private LocalDate acquired;

    @CsvBindByName(column = "Reg #")
    private String regNo = "";
    @CsvBindByName(column = "Champ #")
    private String champNo = "";
    @CsvBindByName(column = "Legs")
    private String legs = "";
    @CsvBindByName(column = "Genotype")
    private String genotype = "";  //TODO: needs a string builder for genotypes

    //TODO:: removed as the service now retreives these counts
    //private Integer littersCount;   // make a getter and return a count
    //private Integer kitsCount;   // make a getter and return a count

    @CsvBindByName(column = "Notes")
    private String notes = "";

    //TODO: needs to be a table of status and statusDate
    @CsvBindByName(column = "Status")
    private String status = ""; 
    @CsvCustomBindByName(column = "Status Date", converter = LocalDateCsvConverterDDMMYYYY.class)
    private LocalDateTime statusDate;

    @Transient
    private Boolean active;

    @CsvBindByName(column = "ProfileImage")
    private String profileImage;
    
    @Transient
    private String defaultImageSource = "rabbit_blank.jpg";

    @Transient
    private final String profileImagePath = System.getenv("PATH_TO_PROFILE_IMAGE");
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "litterId",foreignKey = @jakarta.persistence.ForeignKey(name = "none"))
    private Litter litter;

    public Litter getLitter() {
        return litter;
    }

    public void setLitter(Litter litter) {
        this.litter = litter;
    }

    @ManyToOne
    @JoinColumn(name = "fosterLitterId",foreignKey = @jakarta.persistence.ForeignKey(name = "none"))
    private Litter fosterLitter;

    public Litter getFosterLitter() {
        return fosterLitter;
    }

    public void setFosterLitter(Litter litter) {
        this.fosterLitter = litter;
    }
    
    public String getFosterLitterName(){
        if(fosterLitter==null) return "";
        return fosterLitter.getParentsFormatted();
    }
    
    public void setFosterLitterName(String name){
        //do nothing - only here as binder needs a set
    }
    
    @Transient
    private Boolean needsSaving = Boolean.FALSE;

    //the getter below is accessed by JPA to return the calculated age
    private Long ageInDays;
    
    private Double stockValue;
    
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.id);
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
        final Stock other = (Stock) obj;
        return Objects.equals(this.id, other.id);
    }

    @Transient
    private Boolean temp = Boolean.FALSE;
    
    //used for stock that you do not own but need to track for a pedigree
    private Boolean external = Boolean.FALSE;

    //START Common Audit fields for any entity
    //need to add the below for any entity using these audit fields
    //@EntityListeners(AuditingEntityListener.class)
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    //END Common time stamps for records
    
    public Stock() {
        //this.active = getActive();
        
    }

    public Stock(String name, Boolean breeder, StockType stockType) {
        this.name = name;
        //this.active = active;
        this.breeder = breeder;
        this.stockType = stockType;
        //as only used when creating a temp Stock item set id to -1
        this.id = -1;
        this.temp = Boolean.TRUE;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Gender getSex() {
        return sex;
    }

    public void setSex(Gender sex) {
        this.sex = sex;
    }

    public String getPrefix() {
        if(prefix==null) return "";
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        if(name==null) return "";
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTattoo() {
        if(tattoo==null) return "";
        return tattoo;
    }

    public void setTattoo(String tattoo) {
        this.tattoo = tattoo;
    }

    /*
    public String getCage() {
        if(cage==null) return "";
        return cage;
    }

    public void setCage(String cage) {
        this.cage = cage;
    }
    */

    public String getColor() {
        if(color==null) return "";
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBreed() {
        if(breed==null) return "";
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public Integer getWeight() {
        if(weight==null) return 0;
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public LocalDate getDoB() {
        return doB;
    }

    public LocalDateTime getWeightDate() {
        return weightDate;
    }

    public void setWeightDate(LocalDateTime weightDate) {
        this.weightDate = weightDate;
    }

    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }

    public AgeBetween getAge() {
        LocalDate endDate = LocalDate.now();
        if (getStockStatus().stopsAgeCalculation() && getStatusDate() != null) {
            endDate = getStatusDate().toLocalDate();
        }
        return new AgeBetween(this.doB, endDate);
    }
    
    @Access(AccessType.PROPERTY)
    public Long getAgeInDays() {
        //System.out.println("getAgeInDays: returning:" + getAge().getAgeInDays());
        return getAge().getAgeInDays();
    }

    @Access(AccessType.PROPERTY)
    public void setAgeInDays(Long ageInDays) {
        this.ageInDays = ageInDays;
    }
    
    public LocalDate getAcquired() {
        return acquired;
    }

    public void setAcquired(LocalDate acquired) {
        this.acquired = acquired;
    }

    public String getRegNo() {
        if(regNo==null) return "";
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getChampNo() {
        if(champNo==null) return "";
        return champNo;
    }

    public void setChampNo(String champNo) {
        this.champNo = champNo;
    }

    public String getLegs() {
        if(legs==null) return "";
        return legs;
    }

    public void setLegs(String legs) {
        this.legs = legs;
    }

    public String getGenotype() {
        if(genotype==null) return "";
        return genotype;
    }

    public void setGenotype(String genotype) {
        this.genotype = genotype;
    }

    @SuppressWarnings("SingleCharRegex")
    public String getNotes() {
        if(this.notes==null) return "";
        return notes.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\"", "");
    }
    
    public Boolean hasNotes(){
        return !getNotes().isEmpty();
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Access(AccessType.PROPERTY)
    public String getStatus() {
        /*
        if(status==null || status.isEmpty()){
            System.out.println("Stock.getStatus: status blank converted to active" + " for:" + getDisplayName());
            return "active";
        }
        System.out.println("Stock.getStatus: status:" + status + " for:" + getDisplayName());
        */
        return status;
    }

    @Access(AccessType.PROPERTY)
    public void setStatus(String status) {
        if(status==null || status.isEmpty()){
            this.status = "active";
        }
        this.status = status;
    }

    public StockStatus getStockStatus() {
        return Utility.getInstance().getStockStatus(getStatus());
    }

    public LocalDateTime getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(LocalDateTime statusDate) {
        this.statusDate = statusDate;
    }
    
    @Access(AccessType.PROPERTY)
    public Boolean getActive(){
        if(getStatus()==null) return Boolean.FALSE;
        if(getStatus().equals("active")){
            //System.out.println("Stock.getActive: returning TRUE for Status:" + status + " for:" + getDisplayName());
            return Boolean.TRUE;
        }
        //System.out.println("Stock.getActive: returning FALSE for Status:" + status + " for:" + getDisplayName());
        return Boolean.FALSE;
    }

    @Access(AccessType.PROPERTY)
    public void setActive(Boolean active) {
        //System.out.println("Stock.setActive: setting active to:" + active + " for:" + getDisplayName());
        this.active = active;
    }

    public Boolean getBreeder() {
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
        if(sexText==null) return "";
        return sexText;
    }

    public void setSexText(String sexText) {
        this.sexText = sexText;
    }

    public String getFatherName() {
        if(fatherName==null) return "";
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
        if(motherName==null) return "";
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
        String retVal;
        if (inVal.contains("(")) {
            retVal = inVal.substring(0, inVal.lastIndexOf("("));
        } else {
            retVal = inVal;
        }
        return retVal;
    }

    private String getTattooFromName(String inVal) {
        if (inVal.contains("()")) {
            return "";
        } else {
            return inVal.substring(inVal.lastIndexOf("(") + 1, inVal.lastIndexOf(")"));
        }
    }

    public String getMotherExtName() {
        //if(motherExtName==null) return "";
        return motherExtName;
    }

    public void setMotherExtName(String motherExtName) {
        this.motherExtName = motherExtName;
    }

    public String getFatherExtName() {
        //if(fatherExtName==null) return "";
        return fatherExtName;
    }

    public void setFatherExtName(String fatherExtName) {
        this.fatherExtName = fatherExtName;
    }
    
    public Integer getFatherId() {
        return fatherId;
    }

    public void setFatherId(Integer fatherId) {
        this.fatherId = fatherId;
    }

    public Integer getMotherId() {
        return motherId;
    }

    public void setMotherId(Integer motherId) {
        this.motherId = motherId;
    }

    public String getWeightText() {
        if(weightText==null) return "";
        return weightText;
    }

    public void setWeightText(String weightText) {
        this.weightText = weightText;
    }

    public String getWeightInLbsOz() {
        if (getWeight() > 0) {
            return Utility.getInstance().WeightConverterOzToHTML(getWeight());
        }
        return "<p>" + Utility.EMPTY_VALUE + "</p>";
    }

    public String getWeightInLbsOzAsString() {
        if (getWeight() > 0) {
            return Utility.getInstance().WeightConverterOzToString(getWeight());
        }
        return Utility.EMPTY_VALUE;
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
    
    public String getDisplayNameWithStatus() {
        String displayName;
        if (getName().isEmpty()) {
            displayName = getTattoo();
        }else{
            displayName = getName();
        }
        return displayName + " (" + getStatus() + ")";
    }
    
    public File getProfileFile(){
        File profileFile = new File(profileImagePath, getProfileFileName());
        if(profileFile.exists()){
            //System.out.println("getProfileFile: file found:" + profileFile.toString());
            return profileFile;
        }else{
            profileFile = new File(profileImagePath, getDefaultImageSource());
            if (profileFile.exists()){
                //System.out.println("getProfileFile: default found:" + profileFile.toString());
                return profileFile;
            }else{
                //System.out.println("getProfileFile: default NOT found: default:" + getDefaultImageSource());
                return null;
            }
        }
    }
    
    public File getProfileFileToBeSaved(){
        return new File(profileImagePath, getProfileFileName());
    }

    private String getProfileFileName() {
        return "stock-profile-" + getProfileFileBaseName() + ".jpg";
    }

    private String getProfileFileBaseName() {
        if (getTattoo().isEmpty()) {
            if(getName().isEmpty()){
                if(getId()==null){
                    return getDefaultImageSource();
                }else{
                    return getId().toString();
                }
            }
            return getName();
        }
        return getTattoo();
    }

    /*
    public Boolean getActive() {
        if (getStatus() == null || getStatus().isEmpty() || getStatus().equals("active")) {
            //System.out.println("Stock.getActive: returning TRUE for getStatus:" + getStatus() + " for:" + getDisplayName());
            return Boolean.TRUE;
        }
        //System.out.println("Stock.getActive: returning FALSE for getStatus:" + getStatus() + " for:" + getDisplayName());
        return Boolean.FALSE;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
    */

    public String getDefaultImageSource() {
        defaultImageSource = "farmtracks_blank.jpg";
        if(getStockType()==null) return defaultImageSource;
        return getStockType().getImageFileName();
    }

    public void setDefaultImageSource(String defaultImageSource) {
        this.defaultImageSource = defaultImageSource;
    }
    
    public SvgIcon getGenderIcon(){
        return switch(getSex()){
            case FEMALE -> new SvgIcon(Utility.ICONS.GENDER_FEMALE.getIconSource());
            case MALE -> new SvgIcon(Utility.ICONS.GENDER_MALE.getIconSource());
            default -> null;
        };
    }

    public String getProfileImage() {
        if(profileImage==null || profileImage.isEmpty()){
            return defaultImageSource;
        }
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
    
    public HorizontalLayout getStockHeader(Boolean fullHeader){
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Avatar stockName = new Avatar();
        stockName.setName(getName());
        stockName.setColorIndex(5);
        stockName.addThemeVariants(AvatarVariant.LUMO_LARGE);
        
        stockName.setImageHandler(DownloadHandler.forFile(getProfileFile()));

        VerticalLayout columnAvatars = new VerticalLayout(stockName);

        columnAvatars.setPadding(false);
        columnAvatars.setSpacing(true);
        columnAvatars.setWidth("75px");
        columnAvatars.setAlignItems(FlexComponent.Alignment.CENTER);
        columnAvatars.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout columnInfo = new VerticalLayout();
        columnInfo.setPadding(false);
        columnInfo.setSpacing(false);

        columnInfo.add(getHeader());

        if(fullHeader){
            Span stockWeight = new Span(getWeightInLbsOz());
            stockWeight.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            columnInfo.add(stockWeight);
        }
        String extraInfo = "";
        if(!getTattoo().isEmpty()){
            extraInfo = "(" + getTattoo() + ")";
        }
        if(!getBreed().isEmpty()){
            extraInfo += "(" + getBreed() + ")";
        }
        if(!getColor().isEmpty()){
            extraInfo += "(" + getColor() + ")";
        }
        
        Span xInfo = new Span(extraInfo);
        xInfo.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        columnInfo.add(xInfo);

        row.add(columnAvatars, columnInfo);
        row.getStyle().set("line-height", "var(--lumo-line-height-m)");
        return row;
    }
    
    public Layout getHeader(){
        return getHeader(Boolean.FALSE);
    }

    public Layout getHeader(Boolean forPedigree){
        // Header
        Layout header = new Layout();
        if(forPedigree){
            header.setJustifyContent(Layout.JustifyContent.BETWEEN);
        }
        header.setAlignItems(Layout.AlignItems.CENTER);
        header.setGap(Layout.Gap.SMALL);
        header.setWidthFull();
        header.setFlexWrap(Layout.FlexWrap.WRAP);
        
        Layout nameAndPrefix = new Layout();
        if(forPedigree){
            nameAndPrefix.setFlexDirection(Layout.FlexDirection.COLUMN);
            nameAndPrefix.setAlignItems(Layout.AlignItems.START);
        }else{
            nameAndPrefix.setFlexDirection(Layout.FlexDirection.ROW);
            nameAndPrefix.setAlignItems(Layout.AlignItems.CENTER);
        }

        if(forPedigree){
            Badge badge = new Badge(Utility.EMPTY_VALUE);
            if(!getPrefix().isEmpty()){
                badge.setText(getPrefix());
            }
            badge.addThemeVariants(BadgeVariant.PILL, BadgeVariant.SMALL);
            nameAndPrefix.add(badge);
        }else{
            if(!getPrefix().isEmpty()){
                Badge badge = new Badge(getPrefix());
                badge.addThemeVariants(BadgeVariant.PILL, BadgeVariant.SMALL);
                nameAndPrefix.add(badge);
            }
        }
        Span stockName = new Span(getDisplayName());
        //stockName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);
        stockName.addClassNames("stock-name-responsive");
        
        nameAndPrefix.add(stockName);
        header.add(nameAndPrefix);
        //add icons
        if(getSex().equals(Gender.FEMALE)){
            header.add(showIcon(getStockType().getFemaleName(),Utility.ICONS.GENDER_FEMALE.getIconSource()));
        }else if(getSex().equals(Gender.MALE)){
            header.add(showIcon(getStockType().getMaleName(),Utility.ICONS.GENDER_MALE.getIconSource()));
        }
        if(!forPedigree && getBreeder()){
            header.add(showIcon(getStockType().getBreederName(),Utility.ICONS.TYPE_BREEDER.getIconSource()));
        }
        //System.out.println("getHeader: StockStatus:" + getStatus());
        if(!forPedigree && Utility.getInstance().hasStockStatus(getStatus())){
            StockStatus stockStatus = Utility.getInstance().getStockStatus(getStatus());
            header.add(showIcon(stockStatus.getLongName(),stockStatus.getIcon().getIconSource()));
        }
        return header;
    }

    private Layout showIcon(String text, String icon) {
        Layout layout = new Layout();
        layout.setAlignItems(Layout.AlignItems.CENTER);
        layout.setGap(Layout.Gap.SMALL);

        Icon svgIcon = new Icon(icon);
        svgIcon.setTooltipText(text);
        svgIcon.addClassName("vaadin-icon-responsive");  //set in styles.css
        //svgIcon.addClassNames(LumoUtility.IconSize.SMALL);
        //svgIcon.getStyle().set("--vaadin-icon-visual-size", "1.0rem");
        //svgIcon.getStyle().set("--vaadin-icon-size", "1.0rem");
        layout.add(svgIcon);
        return layout;
    }
    
    public AvatarDiv getAvatar(Boolean largeBorder){
        Avatar avatar = new Avatar();
        AvatarDiv avatarDiv = new AvatarDiv(avatar);
        avatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
        avatar.setImageHandler(DownloadHandler.forFile(getProfileFile()));
        UIUtilities.setBorders(avatar, this, UIUtilities.BorderSize.LARGE);
        return avatarDiv;
    }
    
    @SuppressWarnings("ConvertToStringSwitch")
    public void updateFromImported(Boolean importBreeder, Integer importId, StockType importStockType) {
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
        if(status==null || status.isEmpty()){
            status = "active";
        }
        //create a new status record from status
        active = getActive();
        needsSaving = Boolean.TRUE;
    }

    public Boolean isTemp() {
        return temp;
    }

    public Boolean getExternal() {
        if(external==null) return Boolean.FALSE;
        return external;
    }

    public void setExternal(Boolean external) {
        this.external = external;
    }
    
    public Double getStockValue() {
        return stockValue;
    }

    public void setStockValue(Double stockValue) {
        this.stockValue = stockValue;
    }

    @Override
    public String toString() {
        return "Stock{" + "id=" + id + ", name=" + name + ", tattoo=" + tattoo  + ", breeder=" + breeder + ", sexText=" + sexText + ", sex=" + sex + ", prefix=" + prefix+ ", fatherName=" + fatherName + ", motherName=" + motherName + ", fatherId=" + fatherId + ", motherId=" + motherId + ", fatherExtName=" + fatherExtName + ", motherExtName=" + motherExtName + ", color=" + color + ", breed=" + breed + ", weightText=" + weightText + ", weight=" + weight + ", weightDate=" + weightDate + ", doB=" + doB + ", acquired=" + acquired + ", regNo=" + regNo + ", champNo=" + champNo + ", legs=" + legs + ", genotype=" + genotype + ", notes=" + notes + ", status=" + status + ", statusDate=" + statusDate + ", active=" + active + ", profileImage=" + profileImage + ", defaultImageSource=" + defaultImageSource + ", profileImagePath=" + profileImagePath + ", litter=" + litter + ", fosterLitter=" + fosterLitter + ", needsSaving=" + needsSaving + ", ageInDays=" + ageInDays + ", stockValue=" + stockValue + ", temp=" + temp + ", external=" + external + ", createdDate=" + createdDate + ", lastModifiedDate=" + lastModifiedDate + ", stockType=" + stockType  + '}';
    }


}
