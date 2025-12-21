/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;

import ca.jusjoken.data.service.ColumnNameComparator;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusComparator;
import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author birch
 */
public class Utility {
    public static Utility instance = null;
    
    public static Utility getInstance() {
        if (Utility.instance == null) {
            Utility.instance = new Utility();
            createStockStatusList();
            createStockColumnNameList();
        }
        return Utility.instance;
    }
    
    private static final Map<String, StockStatus> stockStatusList = new HashMap();
    private static final Map<String, ColumnName> stockColumnNameList = new HashMap();

    public static enum Gender{
        MALE("M"), FEMALE("F"), NA("NA");

        private final String shortName;
        
        private Gender(String shortName) {
            this.shortName = shortName;
        }
        
        public String getShortName(){
            return shortName;
        }
        
        public static Gender fromShortName(String shortName){
            switch (shortName){
                case "M":
                    return Gender.MALE;
                case "F":
                    return Gender.FEMALE;
                case "NA":
                    return Gender.NA;
                default:
                    throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
            }
        }
    }

    public static enum BreederFilter{
        BREEDER(Boolean.TRUE), NONBREEDER(Boolean.FALSE), ALL(null);

        private final Boolean isBreeder;
        
        private BreederFilter(Boolean isBreeder) {
            this.isBreeder = isBreeder;
        }
        public Boolean getIsBreeder(){
            return isBreeder;
        }
        
        public static BreederFilter fromIsBreeder(Boolean isBreeder){
            if(isBreeder==null){
                return BreederFilter.ALL;
            }else if(isBreeder){
                return BreederFilter.BREEDER;
            }else{
                return BreederFilter.NONBREEDER;
            }
        }
        
        public static List<BreederFilter> filterList(){
            List<BreederFilter> list = new ArrayList<>();
            list.add(ALL);
            list.add(BREEDER);
            list.add(NONBREEDER);
            return list;
        }
    }
        
    public static enum ImportType{
        BREEDERS("Breeders"), KITS("Kits"), LITTERS("Litters");

        public final String typeName;
        private ImportType(String s) {
            this.typeName = s;
        }
    }
    
    public static enum TabType{
        NONE("None"), HASDATA("HasData"), COUNT("Count");

        public final String typeName;
        private TabType(String s) {
            this.typeName = s;
        }
    }
    
    public static enum PanelType{
        LITTERS("Litters"), KITS("Kits"), NONE("None");

        public final String typeName;
        private PanelType(String s) {
            this.typeName = s;
        }
    }
    
    public enum ICONS {
        STATUS_ACTIVE(FontAwesome.Solid.CIRCLE_CHECK.create().getIcon()),
        STATUS_INACTIVE(FontAwesome.Solid.TIMES_CIRCLE.create().getIcon()),
        STATUS_ALL(FontAwesome.Solid.GLOBE.create().getIcon()),
        STATUS_CULLED(FontAwesome.Solid.CUT.create().getIcon()),
        STATUS_BUTCHERED(FontAwesome.Solid.UTENSILS.create().getIcon()),
        STATUS_DIED(FontAwesome.Solid.HEART_BROKEN.create().getIcon()),
        STATUS_SOLD(FontAwesome.Solid.DOLLAR_SIGN.create().getIcon()),
        STATUS_SOLD_W_DEPOSIT(FontAwesome.Solid.HAND_HOLDING_USD.create().getIcon()),
        STATUS_ARCHIVED(FontAwesome.Solid.ARCHIVE.create().getIcon()),
        ACTION_DELETE(FontAwesome.Solid.TRASH.create().getIcon()),
        ACTION_MARK_FOR_SALE(FontAwesome.Solid.MONEY_BILL.create().getIcon()),
        ACTION_CAGE_CARD(FontAwesome.Solid.LIST.create().getIcon()),
        ACTION_BIRTH(FontAwesome.Solid.BIRTHDAY_CAKE.create().getIcon()),
        ACTION_EDIT(FontAwesome.Solid.PENCIL_ALT.create().getIcon()),
        GENDER_FEMALE(FontAwesome.Solid.VENUS.create().getIcon()),
        GENDER_MALE(FontAwesome.Solid.MARS.create().getIcon()),
        TYPE_BREEDER(FontAwesome.Solid.VENUS_MARS.create().getIcon());

        private final String iconSource;

        ICONS(String iconSource) {
            this.iconSource = iconSource;
        }

        public String getIconSource() {
            return this.iconSource;
        }
    }
    
    public static final String emptyValue = "--";
    
    private static void createStockColumnNameList(){
        stockColumnNameList.clear();
        stockColumnNameList.put("ageInDays", new ColumnName("Age", "ageInDays"));
        stockColumnNameList.put("breed", new ColumnName("Breed", "breed"));
        stockColumnNameList.put("doB", new ColumnName("Birthdate", "doB"));
        stockColumnNameList.put("color", new ColumnName("Color", "color"));
        stockColumnNameList.put("tattoo", new ColumnName("ID", "tattoo"));
        stockColumnNameList.put("sex", new ColumnName("Gender", "sex"));
        stockColumnNameList.put("name", new ColumnName("Name", "name"));
        stockColumnNameList.put("prefix", new ColumnName("Prefix", "prefix"));
        stockColumnNameList.put("status", new ColumnName("Status", "status"));
        stockColumnNameList.put("weight", new ColumnName("Weight", "weight"));
        stockColumnNameList.put("litterCount", new ColumnName("# of Litters", "litterCount"));
        stockColumnNameList.put("kitCount", new ColumnName("# of Kits", "kitCount"));
    }

    public Collection<ColumnName> getStockColumnNameList() {
        List<ColumnName> list = new ArrayList<ColumnName>(stockColumnNameList.values());
        Collections.sort(list, new ColumnNameComparator());
        return list;
    }

    public Boolean hasColumnName(String name){
        return stockColumnNameList.containsKey(name);
    }
    
    public ColumnName getColumnName(String name){
        if(name==null || name.isEmpty()){
            return stockColumnNameList.get("name");
        }
        if(stockColumnNameList.containsKey(name)){
            return stockColumnNameList.get(name);
        }else{
            return null;
        }
    }

    private static void createStockStatusList(){
        stockStatusList.clear();
        stockStatusList.put("active", new StockStatus("active", ICONS.STATUS_ACTIVE, "Active",Boolean.FALSE,1));
        stockStatusList.put("inactive", new StockStatus("inactive", ICONS.STATUS_INACTIVE, "Inactive",Boolean.FALSE,2));
        stockStatusList.put("all", new StockStatus("all", ICONS.STATUS_ALL, "All",Boolean.FALSE,3));
        stockStatusList.put("archived", new StockStatus("archived", ICONS.STATUS_ARCHIVED, "Archived",Boolean.TRUE,5));
        stockStatusList.put("butchered", new StockStatus("butchered", ICONS.STATUS_BUTCHERED, "Butchered",Boolean.TRUE,6));
        stockStatusList.put("culled", new StockStatus("culled", ICONS.STATUS_CULLED, "Culled",Boolean.TRUE,7));
        stockStatusList.put("died", new StockStatus("died", ICONS.STATUS_DIED, "Died",Boolean.TRUE,8));
        stockStatusList.put("forsale", new StockStatus("forsale", ICONS.ACTION_MARK_FOR_SALE, "For sale",Boolean.FALSE,9));
        stockStatusList.put("sold", new StockStatus("sold", ICONS.STATUS_SOLD, "Sold",Boolean.TRUE,10));
        stockStatusList.put("deposit", new StockStatus("deposit", ICONS.STATUS_SOLD_W_DEPOSIT, "Sold with deposit",Boolean.FALSE,11));
    }
    
    public Boolean hasStockStatus(String name){
        return stockStatusList.containsKey(name);
    }
    
    public StockStatus getStockStatus(String name){
        if(name==null || name.isEmpty()){
            return stockStatusList.get("active");
        }
        if(stockStatusList.containsKey(name)){
            return stockStatusList.get(name);
        }else{
            return null;
        }
    }

    public Collection<StockStatus> getStockStatusList() {
        List<StockStatus> list = new ArrayList<StockStatus>(stockStatusList.values());
        Collections.sort(list, new StockStatusComparator());
        return list;
    }
    

    public Double round(Double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
    public Integer WeightConverterPoundsOuncesToOz(Integer pounds, Integer ounces){
        Integer totalOunces = pounds * 16 + ounces;
        //System.out.println("Total ounces: " + totalOunces);        
        return totalOunces;
    }
    
    public Integer WeightConverterOzToPounds(Integer ounces){
        Integer pounds = (int) (ounces / 16);
        //System.out.println("Ounces: " + ounces + " = " + retVal);        
        return pounds;        
    }
    
    public Integer WeightConverterOzToRemainingOunces(Integer ounces){
        System.out.println("WeightConverterOzToRemainingOunces: ounces in:" + ounces);
        Integer remainingOunces = (int) (ounces % 16);
        System.out.println("WeightConverterOzToRemainingOunces: remainingOunces:" + remainingOunces);
        return remainingOunces;        
    }
    
    public Integer WeightConverterStringToOz(String weight){
        //String weight = "2 lbs 6 oz"; // Example input
        String[] parts = weight.split(" ");
        int pounds = Integer.parseInt(parts[0]);
        int ounces = 0;
        if(parts.length>2){
            ounces = Integer.parseInt(parts[2]);
        }

        Integer totalOunces = pounds * 16 + ounces;
        //System.out.println("Total ounces: " + totalOunces);        
        return totalOunces;
    }
    
    public String WeightConverterOzToString(Integer ounces){
        Integer pounds = (int) (ounces / 16);
        Integer remainingOunces = (int) (ounces % 16);
        String retVal = pounds.toString() + " lbs " + remainingOunces.toString() + " oz";
        //System.out.println("Ounces: " + ounces + " = " + retVal);        
        return retVal;        
    }
    
    public String WeightConverterOzToHTML(Integer ounces){
        String sub = "<sub>";
        String subEnd = "</sub>";
        Integer pounds = (int) (ounces / 16);
        Integer remainingOunces = (int) (ounces % 16);
        String retVal = "<p>" + pounds.toString() + sub + "lbs" + subEnd + " " + remainingOunces.toString() + sub + "oz" + subEnd + "</p>";
        return retVal;        
    }
    
}
