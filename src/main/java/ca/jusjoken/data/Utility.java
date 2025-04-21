/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;

import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusComparator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.vaadin.lineawesome.LineAwesomeIcon;

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
        }
        return Utility.instance;
    }
    
    private static final Map<String, StockStatus> stockStatusList = new HashMap();

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
        STATUS_ACTIVE(LineAwesomeIcon.CHECK_CIRCLE_SOLID.getSource()),
        STATUS_INACTIVE(LineAwesomeIcon.TIMES_CIRCLE_SOLID.getSource()),
        STATUS_ALL(LineAwesomeIcon.GLOBE_SOLID.getSource()),
        STATUS_CULLED(LineAwesomeIcon.CUT_SOLID.getSource()),
        STATUS_BUTHERED(LineAwesomeIcon.UTENSILS_SOLID.getSource()),
        STATUS_DIED(LineAwesomeIcon.HEART_BROKEN_SOLID.getSource()),
        STATUS_SOLD(LineAwesomeIcon.DOLLAR_SIGN_SOLID.getSource()),
        STATUS_SOLD_W_DEPOSIT(LineAwesomeIcon.HAND_HOLDING_USD_SOLID.getSource()),
        STATUS_ARCHIVED(LineAwesomeIcon.ARCHIVE_SOLID.getSource()),
        ACTION_DELETE(LineAwesomeIcon.TRASH_SOLID.getSource()),
        ACTION_MARK_FOR_SALE(LineAwesomeIcon.MONEY_BILL_SOLID.getSource()),
        ACTION_CAGE_CARD(LineAwesomeIcon.LIST_SOLID.getSource()),
        ACTION_BIRTH(LineAwesomeIcon.BIRTHDAY_CAKE_SOLID.getSource()),
        ACTION_EDIT(LineAwesomeIcon.PENCIL_ALT_SOLID.getSource()),
        GENDER_FEMALE(LineAwesomeIcon.VENUS_SOLID.getSource()),
        GENDER_MALE(LineAwesomeIcon.MARS_SOLID.getSource()),
        TYPE_BREEDER(LineAwesomeIcon.VENUS_MARS_SOLID.getSource());

        private final String iconSource;

        ICONS(String iconSource) {
            this.iconSource = iconSource;
        }

        public String getIconSource() {
            return this.iconSource;
        }
    }
    
    public static final String emptyValue = "--";

    private static void createStockStatusList(){
        stockStatusList.clear();
        stockStatusList.put("active", new StockStatus("active", ICONS.STATUS_ACTIVE, "Active",Boolean.FALSE,1));
        stockStatusList.put("inactive", new StockStatus("inactive", ICONS.STATUS_INACTIVE, "Inactive",Boolean.FALSE,2));
        stockStatusList.put("all", new StockStatus("all", ICONS.STATUS_ALL, "All",Boolean.FALSE,3));
        stockStatusList.put("archived", new StockStatus("archived", ICONS.STATUS_ARCHIVED, "Archived",Boolean.TRUE,5));
        stockStatusList.put("butchered", new StockStatus("butchered", ICONS.STATUS_BUTHERED, "Butchered",Boolean.TRUE,6));
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
