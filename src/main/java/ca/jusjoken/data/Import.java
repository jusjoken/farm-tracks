/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;

import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterRepository;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockRepository;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.views.stock.BreedersViewOld;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author birch
 */
public class Import {
    StockRepository stockRepository;
    StockTypeRepository stockTypeRepository;
    LitterRepository litterRepository;
    private List<Stock> breederImportList = new ArrayList<>();
    private List<Stock> kitImportList = new ArrayList<>();
    private List<Litter> litterImportList = new ArrayList<>();

    public Import() {
        this.stockRepository = Registry.getBean(StockRepository.class);
        this.stockTypeRepository = Registry.getBean(StockTypeRepository.class);
        this.litterRepository = Registry.getBean(LitterRepository.class);
    }
    
    public void importBreederFromEverbreed(String filePath){
        try {
            breederImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Stock.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearBreederList();
            Logger.getLogger(BreedersViewOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void importKitFromEverbreed(String filePath){
        try {
            kitImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Stock.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearKitList();
            Logger.getLogger(BreedersViewOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void importLitterFromEverbreed(String filePath){
        try {
            litterImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Litter.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearLitterList();
            Logger.getLogger(BreedersViewOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean processAllImportsFromEverbreed(){
        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed records");
        stockRepository.deleteAll();
        if(!kitImportList.isEmpty()){
            System.out.println("processAllImportsFromEverbreed: processing kits");
            processStockList(kitImportList, Boolean.FALSE);
        }
        if(!breederImportList.isEmpty()){
            System.out.println("processAllImportsFromEverbreed: processing breeder");
            processStockList(breederImportList, Boolean.TRUE);
        }
        //process parents
        if(!kitImportList.isEmpty()){
            System.out.println("processAllImportsFromEverbreed: processing kit parent records");
            processStockParents(kitImportList);
        }
        if(!breederImportList.isEmpty()){
            System.out.println("processAllImportsFromEverbreed: processing breeder parent records");
            processStockParents(breederImportList);
        }
        //process Litters
        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed litter records");
        litterRepository.deleteAll();
        if(!litterImportList.isEmpty()){
            processLitterList(litterImportList);
        }
        
        System.out.println("processAllImportsFromEverbreed: complete processing " + kitImportList.size() + " kits, " + breederImportList.size() + " breeders, " + litterImportList.size() + " litters.");
        return Boolean.TRUE;
    }
    
    public Boolean processBreedersFromEverbreed(){
        if(breederImportList.isEmpty()){
            return Boolean.FALSE;
        }
        processStockList(breederImportList, Boolean.TRUE);
        processStockParents(breederImportList);
        return Boolean.TRUE;
    }
    
    public Boolean processKitsFromEverbreed(){
        if(kitImportList.isEmpty()){
            return Boolean.FALSE;
        }
        processStockList(kitImportList, Boolean.FALSE);
        processStockParents(kitImportList);
        return Boolean.TRUE;
    }
    
    public Boolean processLittersFromEverbreed(){
        if(litterImportList.isEmpty()){
            return Boolean.FALSE;
        }
        processLitterList(litterImportList);
        return Boolean.TRUE;
    }
    
    private void processStockList(List<Stock> importList, Boolean isBreeder) {
        StockType stRabbit = stockTypeRepository.findByName("Rabbit");
        if(stRabbit==null) stRabbit = new StockType("Rabbits", "Buck", "Doe", "Breeders", "Kits", Boolean.TRUE);

        for(Stock item: importList){
            //process the import
            //determine if this is an existing record to update
            item.setNeedsSaving(Boolean.FALSE);
            Stock existingStock = stockRepository.findByNameAndTattoo(item.getName(), item.getTattoo());
            if(existingStock==null){
                item.setNeedsSaving(Boolean.TRUE);
                System.out.println("processStockList: existing Not Found for:" + item.toString());
                item.updateFromImported(isBreeder,null,stRabbit);
            }else{
                //existing record exists so ensure if this is a KIT it does not overwrite the existing breeder
                if(!item.isBreeder() && existingStock.isBreeder()){
                    //skip this KIT record as Breeder info should be more current
                    item.setNeedsSaving(Boolean.FALSE);
                    System.out.println("processStockList: existing: skipping KIT record as breeder already exists:" + existingStock.toString());
                }else{
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processStockList: existing:" + existingStock.toString());
                    item.updateFromImported(isBreeder,existingStock.getId(),stRabbit);
                }
            }
            if(item.getNeedsSaving()){
                stockRepository.save(item);
                System.out.println("processStockList: item after save:" + item.toString());
            }
        }
    }
    
    private void processStockParents(List<Stock> importList) {
        //run to update stock for fields that could be in the imported stock (mother/father)
        for(Stock item: importList){
            //process the import
            Long fatherId = null;
            Long motherId = null;
            item.setNeedsSaving(Boolean.FALSE);
            if(!item.getFatherName().isEmpty()){
                System.out.println("processStockParents: FatherText:" + item.getFatherName() + " nameWithoutTattoo:" + item.getFatherNameWithoutTattoo() + " tattoo:" + item.getFatherTattoo());
                Stock fatherStock = stockRepository.findByNameAndTattoo(item.getFatherNameWithoutTattoo(),item.getFatherTattoo());
                System.out.println("processStockParents: FatherStock:" + fatherStock);
                if(fatherStock!=null){
                    fatherId = fatherStock.getId();
                    item.setFatherId(fatherId);
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processStockParents: Father needs saving id:" + fatherId);
                }
            }
            if(!item.getMotherName().isEmpty()){
                System.out.println("processStockParents: MotherText:" + item.getMotherName());
                Stock motherStock = stockRepository.findByNameAndTattoo(item.getMotherNameWithoutTattoo(), item.getMotherTattoo());
                System.out.println("processStockParents: MotherStock:" + motherStock);
                if(motherStock!=null){
                    motherId = motherStock.getId();
                    item.setMotherId(motherId);
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processStockParents: Mother needs saving id:" + motherId);
                }
            }
            //if updated then save again
            if(item.getNeedsSaving()){
                stockRepository.save(item);
                System.out.println("processStockParents: item after save:" + item.toString());
            }else{
                System.out.println("processStockParents: item does not need saving:" + item.toString());
            }

        }
    }
    
    private void processLitterList(List<Litter> importList) {
        List<Litter> litterMissingKits = new ArrayList<>();
        for(Litter item: importList){
            System.out.println("processLitterList: processing item:" + item.toString());
            item.setNeedsSaving(Boolean.FALSE);
            Long fatherId = null;
            Long motherId = null;
            if(!item.getFatherName().isEmpty()){
                System.out.println("processLitterList: FatherText:" + item.getFatherName() + " nameWithoutTattoo:" + item.getFatherNameWithoutTattoo() + " tattoo:" + item.getFatherTattoo());
                Stock fatherStock = stockRepository.findByNameAndTattoo(item.getFatherNameWithoutTattoo(),item.getFatherTattoo());
                System.out.println("processLitterList: FatherStock:" + fatherStock);
                if(fatherStock!=null){
                    fatherId = fatherStock.getId();
                    item.setFather(fatherStock);
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processLitterList: Father needs saving id:" + fatherId);
                }
            }
            if(!item.getMotherName().isEmpty()){
                System.out.println("processLitterList: MotherText:" + item.getMotherName());
                Stock motherStock = stockRepository.findByNameAndTattoo(item.getMotherNameWithoutTattoo(), item.getMotherTattoo());
                System.out.println("processLitterList: MotherStock:" + motherStock);
                if(motherStock!=null){
                    motherId = motherStock.getId();
                    item.setMother(motherStock);
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processLitterList: Mother needs saving id:" + motherId);
                }
            }
            
            //convert imported weight to oz
            if(item.getLitterWeightText().isEmpty()){
                item.setLitterWeight(0);
                item.setNeedsSaving(Boolean.TRUE);
            }else{
                item.setLitterWeight(Utility.getInstance().WeightConverterStringToOz(item.getLitterWeightText()));
                item.setNeedsSaving(Boolean.TRUE);
            }
            
            //if updated then save again
            if(item.getNeedsSaving()){
                litterRepository.save(item);
                System.out.println("processLitterList: item after save:" + item.toString());
            }else{
                System.out.println("processLitterList: item does not need saving:" + item.toString());
            }
            
            //TODO: find each kit in the stock and set the LitterId
            //loop to find matching kits and store when you do not find a match
            //List<Stock> kitsFromLitter = stockRepository.findAllKitsByLitterName(item.getName());
            List<Stock> kitsFromLitter = stockRepository.findAllKitsByMotherFatherDoB(item.getMother().getId(), item.getFather().getId(), item.getDoB());
            for(Stock kit: kitsFromLitter){
                System.out.println("processLitterList: found kit:" + kit.toString());
                kit.setLitter(item);
                stockRepository.save(kit);
            }
            if(kitsFromLitter.size()<item.getKitsSurvivedCount()){
                litterMissingKits.add(item);
            }
            
        }
        for(Litter item: litterMissingKits){
            System.out.println("processLitterList: this litter is missing kits:" + item.toString());
        }


        
    }

    public void clearBreederList(){
        breederImportList.clear();
    }
    public void clearKitList(){
        kitImportList.clear();
    }
    public void clearLitterList(){
        litterImportList.clear();
    }
    public void clearAllImportLists(){
        breederImportList.clear();
        kitImportList.clear();
        litterImportList.clear();
    }
    public Boolean hasBreeders(){
        return !breederImportList.isEmpty();
    }
    public Boolean hasKits(){
        return !kitImportList.isEmpty();
    }
    public Boolean hasLitters(){
        return !litterImportList.isEmpty();
    }

}
