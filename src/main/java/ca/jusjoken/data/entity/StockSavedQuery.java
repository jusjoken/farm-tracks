/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Sort.Direction;

import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockViewStyleConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 *
 * @author birch
 */
@Entity
public class StockSavedQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    
    @Column(name = "saved_query_name", nullable = false)
    private String savedQueryName;

    private Boolean breeder = false;

    @ManyToOne
    private StockType stockType;  //rabbit, goat, pig, cow etc.

    private String stockStatusName;

    private String sort1Column = null;  
    private String sort1Direction = Direction.ASC.name();
    private String sort2Column = null;  
    private String sort2Direction = Direction.ASC.name();
    private String sortOrder = null;
    private Boolean defaultQuery = false;
    private String visibleColumns = null;
    private Boolean displayAsTile = false;
    private Boolean valueLayout = false;
    private Boolean includeExternalStock = false;
    
    @Transient
    private Boolean dirty = false;
    @Transient
    private Boolean needsSaving = false;

    //Note: StockViewStyleConverter class is used to convert these so the shortname is stored in the database
    public static enum StockViewStyle{
        TILE("Tile"), LIST("List"), VALUE("Value"), VALUE_TILE("ValueTile");

        private final String shortName;
        
        private StockViewStyle(String shortName) {
            this.shortName = shortName;
        }
        
        public String getShortName(){
            return shortName;
        }
        
        public static StockViewStyle fromShortName(String shortName){
            switch (shortName){
                case "Tile" -> {
                    return StockViewStyle.TILE;
                }
                case "List" -> {
                    return StockViewStyle.LIST;
                }
                case "Value" -> {
                    return StockViewStyle.VALUE;
                }
                case "ValueTile" -> {
                    return StockViewStyle.VALUE_TILE;
                }
                default -> throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
            }
        }
    }

    @Convert(converter = StockViewStyleConverter.class)
    private StockViewStyle viewStyle = StockViewStyle.TILE;


    public StockSavedQuery() {
    }

    public StockSavedQuery(Integer id,String savedQueryName,Boolean breeder, StockType stockType, StockStatus stockStatus, ColumnSort sort1, ColumnSort sort2) {
        this.id = id;
        this.breeder = breeder;
        this.savedQueryName = savedQueryName;
        this.stockType = stockType;
        this.stockStatusName = stockStatus.getName();
        if(sort1!=null){
            this.sort1Column = sort1.getColumnName();
            this.sort1Direction = sort1.getColumnSortDirection().name();
        }
        if(sort2!=null){
            this.sort2Column = sort2.getColumnName();
            this.sort2Direction = sort2.getColumnSortDirection().name();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSavedQueryName() {
        return savedQueryName;
    }

    public void setSavedQueryName(String savedQueryName) {
        this.savedQueryName = savedQueryName;
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

    public StockStatus getStockStatus() {
        return Utility.getInstance().getStockStatus(stockStatusName);
    }

    public void setStockStatus(StockStatus stockStatus) {
        this.stockStatusName = stockStatus.getName();
    }

    public Boolean getDefaultQuery() {
        return defaultQuery;
    }

    public void setDefaultQuery(Boolean defaultQuery) {
        this.defaultQuery = defaultQuery;
    }

    public Boolean getDirty() {
        return dirty;
    }

    public void setDirty(Boolean dirty) {
        this.dirty = dirty;
    }

    public Boolean getNeedsSaving() {
        return needsSaving;
    }

    public void setNeedsSaving(Boolean needsSaving) {
        this.needsSaving = needsSaving;
    }
    

    public String getSort1Column() {
        return sort1Column;
    }

    public void setSort1Column(String sort1Column) {
        this.sort1Column = sort1Column;
    }

    public String getSort1Direction() {
        return sort1Direction;
    }

    public void setSort1Direction(String sort1Direction) {
        this.sort1Direction = sort1Direction;
    }

    public String getSort2Column() {
        return sort2Column;
    }

    public void setSort2Column(String sort2Column) {
        this.sort2Column = sort2Column;
    }

    public String getSort2Direction() {
        return sort2Direction;
    }

    public void setSort2Direction(String sort2Direction) {
        this.sort2Direction = sort2Direction;
    }

    public ColumnSort getSort1() {
        List<ColumnSort> sortOrders = getSortOrders();
        if (sortOrders.isEmpty()) {
            return new ColumnSort(sort1Column, Direction.fromString(sort1Direction));
        }
        return sortOrders.get(0);
    }

    public void setSort1(ColumnSort sort1) {
        if (sort1 == null) {
            this.sort1Column = null;
            this.sort1Direction = Direction.ASC.name();
            if (sort2Column == null) {
                sortOrder = null;
            }
            return;
        }
        this.sort1Column = sort1.getColumnName();
        this.sort1Direction = sort1.getColumnSortDirection().name();
        List<ColumnSort> sortOrders = getSortOrders();
        if (sortOrders.isEmpty()) {
            sortOrders.add(sort1);
        } else {
            sortOrders.set(0, sort1);
        }
        setSortOrders(sortOrders);
    }

    public ColumnSort getSort2() {
        List<ColumnSort> sortOrders = getSortOrders();
        if (sortOrders.size() < 2) {
            return new ColumnSort(sort2Column, Direction.fromString(sort2Direction));
        }
        return sortOrders.get(1);
    }

    public void setSort2(ColumnSort sort2) {
        if (sort2 == null) {
            this.sort2Column = null;
            this.sort2Direction = Direction.ASC.name();
            List<ColumnSort> sortOrders = getSortOrders();
            if (sortOrders.size() > 1) {
                sortOrders = new ArrayList<>(sortOrders.subList(0, 1));
            }
            setSortOrders(sortOrders);
            return;
        }
        this.sort2Column = sort2.getColumnName();
        this.sort2Direction = sort2.getColumnSortDirection().name();
        List<ColumnSort> sortOrders = getSortOrders();
        if (sortOrders.isEmpty()) {
            sortOrders.add(new ColumnSort(sort1Column, Direction.fromString(sort1Direction)));
        }
        if (sortOrders.size() == 1) {
            sortOrders.add(sort2);
        } else {
            sortOrders.set(1, sort2);
        }
        setSortOrders(sortOrders);
    }

    public List<ColumnSort> getSortOrders() {
        if (sortOrder != null && !sortOrder.isBlank()) {
            List<ColumnSort> parsed = new ArrayList<>();
            String[] entries = sortOrder.split(",");
            for (String entry : entries) {
                String[] kv = entry.split(":");
                if (kv.length != 2 || kv[0] == null || kv[0].isBlank()) {
                    continue;
                }
                parsed.add(new ColumnSort(kv[0], Direction.fromString(kv[1])));
            }
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }

        List<ColumnSort> legacy = new ArrayList<>();
        if (sort1Column != null && !sort1Column.isBlank()) {
            legacy.add(new ColumnSort(sort1Column, Direction.fromString(sort1Direction)));
        }
        if (sort2Column != null && !sort2Column.isBlank()) {
            legacy.add(new ColumnSort(sort2Column, Direction.fromString(sort2Direction)));
        }
        return legacy;
    }

    public void setSortOrders(List<ColumnSort> sortOrders) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            sortOrder = null;
            sort1Column = null;
            sort1Direction = Direction.ASC.name();
            sort2Column = null;
            sort2Direction = Direction.ASC.name();
            return;
        }

        List<String> serialized = new ArrayList<>();
        for (ColumnSort sort : sortOrders) {
            if (sort == null || sort.getColumnName() == null || sort.getColumnName().isBlank()) {
                continue;
            }
            serialized.add(sort.getColumnName() + ":" + sort.getColumnSortDirection().name());
        }

        if (serialized.isEmpty()) {
            sortOrder = null;
            sort1Column = null;
            sort1Direction = Direction.ASC.name();
            sort2Column = null;
            sort2Direction = Direction.ASC.name();
            return;
        }

        sortOrder = String.join(",", serialized);

        ColumnSort first = sortOrders.stream()
                .filter(s -> s != null && s.getColumnName() != null && !s.getColumnName().isBlank())
                .findFirst()
                .orElse(null);
        if (first != null) {
            sort1Column = first.getColumnName();
            sort1Direction = first.getColumnSortDirection().name();
        }

        List<ColumnSort> cleaned = sortOrders.stream()
                .filter(s -> s != null && s.getColumnName() != null && !s.getColumnName().isBlank())
                .toList();
        if (cleaned.size() > 1) {
            sort2Column = cleaned.get(1).getColumnName();
            sort2Direction = cleaned.get(1).getColumnSortDirection().name();
        } else {
            sort2Column = null;
            sort2Direction = Direction.ASC.name();
        }
    }

    public String getStockStatusName() {
        return stockStatusName;
    }

    public void setStockStatusName(String stockStatusName) {
        this.stockStatusName = stockStatusName;
    }

    @Override
    public String toString() {
        return "StockSavedQuery [id=" + id + ", savedQueryName=" + savedQueryName + ", breeder=" + breeder
                + ", stockType=" + stockType + ", stockStatusName=" + stockStatusName + ", sort1Column=" + sort1Column
                + ", sort1Direction=" + sort1Direction + ", sort2Column=" + sort2Column + ", sort2Direction="
                + sort2Direction + ", defaultQuery=" + defaultQuery + ", visibleColumns=" + visibleColumns + ", dirty="
                + dirty + ", needsSaving=" + needsSaving + ", viewStyle=" + viewStyle + "]";
    }

    public StockViewStyle getViewStyle() {
        return viewStyle;
    }

    public void setViewStyle(StockViewStyle viewStyle) {
        this.viewStyle = viewStyle;
    }

    public String getVisibleColumns() {
        return visibleColumns;
    }

    public void setVisibleColumns(String visibleColumns) {
        if(visibleColumns!=null && visibleColumns.isEmpty()){
            this.visibleColumns = null;
        }else{  
            this.visibleColumns = visibleColumns;
        }
    }

    public List<String> getVisibleColumnKeyList(){
        if(visibleColumns==null) return new ArrayList<>();
        String[] elements = visibleColumns.split(",\\s*"); // Split and trim whitespace
        List<String> fixedList = Arrays.asList(elements);
        return fixedList;
    }

    public Boolean getDisplayAsTile() {
        return displayAsTile;
    }

    public Boolean getValueLayout() {
        return valueLayout;
    }

    public void setValueLayout(Boolean valueLayout) {
        this.valueLayout = valueLayout;
    }

    public void setDisplayAsTile(Boolean displayAsTile) {
        this.displayAsTile = displayAsTile;
    }

    public Boolean getIncludeExternalStock() {
        return Boolean.TRUE.equals(includeExternalStock);
    }

    public void setIncludeExternalStock(Boolean includeExternalStock) {
        this.includeExternalStock = includeExternalStock;
    }

}
