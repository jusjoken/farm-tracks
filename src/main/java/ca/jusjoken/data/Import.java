/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;

import ca.jusjoken.component.ProgressBarUpdateListener;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.StockWeightHistory;
import ca.jusjoken.data.service.LitterRepository;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockRepository;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.StockWeightHistoryService;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.scheduling.annotation.Async;


/**
 *
 * @author birch
 */
public class Import {
    StockRepository stockRepository;
    StockService stockService;
    StockTypeRepository stockTypeRepository;
    StockTypeService typeService;
    LitterRepository litterRepository;
    LitterService litterService;
    StockStatusHistoryService statusService;
    StockWeightHistoryService weightService;
    private List<Stock> breederImportList = new ArrayList<>();
    private List<Stock> kitImportList = new ArrayList<>();
    private List<Litter> litterImportList = new ArrayList<>();
    private List<ProgressBarUpdateListener> listProgressListeners = new ArrayList<>();

    public Import() {
        this.stockRepository = Registry.getBean(StockRepository.class);
        this.stockService = Registry.getBean(StockService.class);
        this.stockTypeRepository = Registry.getBean(StockTypeRepository.class);
        this.typeService = Registry.getBean(StockTypeService.class);
        this.litterRepository = Registry.getBean(LitterRepository.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.statusService = Registry.getBean(StockStatusHistoryService.class);
        this.weightService = Registry.getBean(StockWeightHistoryService.class);
    }
    
    public void importBreederFromEverbreed(String filePath){
        try {
            breederImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Stock.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearBreederList();
            Logger.getLogger(Import.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void importKitFromEverbreed(String filePath){
        try {
            kitImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Stock.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearKitList();
            Logger.getLogger(Import.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void importLitterFromEverbreed(String filePath){
        try {
            litterImportList = new CsvToBeanBuilder(new FileReader(filePath))
                    .withType(Litter.class).build().parse();
        } catch (FileNotFoundException ex) {
            clearLitterList();
            Logger.getLogger(Import.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Async
    public CompletableFuture processAllImportsFromEverbreed(){
        Boolean continueImport = Boolean.TRUE;
        StockType rabbitType = typeService.findRabbits();
        if(rabbitType==null){
            System.out.println("deleteEverbreedRabbits: Rabbit Stock Type not found.  Nothing deleted");
            continueImport = Boolean.FALSE;
        }else{
            notifyProgressStatus("Deleting all supporting everbreed records prior to import.");
            //first delete related records
            List<Stock> rabbitStock = stockRepository.findAllByStockTypeId(rabbitType.getId());
            notifyProgressMax(Double.valueOf(rabbitStock.size()));
            
            Double counter = 0.0;
            for(Stock rabbit: rabbitStock){
                //delete status
                statusService.deleteByStockId(rabbit.getId());
                //delete weight
                weightService.deleteByStockId(rabbit.getId());
                //delete litters
                litterService.deleteByStockId(rabbit.getId());
                counter++;
                notifyProgressUpdate(counter);
            }

            notifyProgressStatus("Deleting all remaining everbreed records prior to import.");
            notifyProgressMax(1.0);
            stockRepository.deleteByStockType(rabbitType.getId());
            notifyProgressUpdate(1.0);
        }
        
        /*
        notifyProgressMax(1.0);
        notifyProgressStatus("Deleting all existing everbreed records prior to import.");
        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed stock records");
        notifyProgressUpdate(0.2);
        stockRepository.deleteAll();

        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed litter records");
        notifyProgressUpdate(0.7);
        litterRepository.deleteAllLittersNative();
        
        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed status history records");
        notifyProgressUpdate(0.8);
        statusService.deleteAll();

        System.out.println("processAllImportsFromEverbreed: deleting all existing everbreed weight history records");
        notifyProgressUpdate(0.9);
        weightService.deleteAll();

        notifyProgressUpdate(1.0);
        */

        if(continueImport){
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
            if(!litterImportList.isEmpty()){
                processLitterList(litterImportList);
            }

            System.out.println("processAllImportsFromEverbreed: complete processing " + kitImportList.size() + " kits, " + breederImportList.size() + " breeders, " + litterImportList.size() + " litters.");
        }else{
            System.out.println("processAllImportsFromEverbreed: skipped processing as delete failed: " + kitImportList.size() + " kits, " + breederImportList.size() + " breeders, " + litterImportList.size() + " litters.");
        }
        
        return CompletableFuture.completedFuture("done");
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
        StockType stRabbit = stockTypeRepository.findByName("Rabbits");
        if(stRabbit==null) stRabbit = new StockType("Rabbits", "Rabbit", "Buck", "Doe", "Breeders", "Kits", Boolean.TRUE);

        notifyProgressMax(Double.valueOf(importList.size()));
        if(isBreeder){
            notifyProgressStatus("Processing all Breeders from everbreed.");
        }else{
            notifyProgressStatus("Processing all Kits from everbreed.");
        }
        Double counter = 0.0;
        for(Stock item: importList){
            //process the import
            //determine if this is an existing record to update
            item.setNeedsSaving(Boolean.FALSE);
            Stock existingStock = stockRepository.findByNameAndTattoo(item.getName(), item.getTattoo());
            if(existingStock==null){
                item.setNeedsSaving(Boolean.TRUE);
                System.out.println("processStockList " + isBreeder + ": existing Not Found for:" + item.toString());
                item.updateFromImported(isBreeder,null,stRabbit);
            }else{
                //existing record exists so ensure if this is a KIT it does not overwrite the existing breeder
                if(!item.getBreeder() && existingStock.getBreeder()){
                    //skip this KIT record as Breeder info should be more current
                    item.setNeedsSaving(Boolean.FALSE);
                    System.out.println("processStockList " + isBreeder + ": existing: skipping KIT record as breeder already exists:" + existingStock.toString());
                }else{
                    item.setNeedsSaving(Boolean.TRUE);
                    System.out.println("processStockList " + isBreeder + ": existing:" + existingStock.toString());
                    item.updateFromImported(isBreeder,existingStock.getId(),stRabbit);
                }
            }
            if(item.getNeedsSaving()){
                Stock newStock = stockRepository.saveAndFlush(item);
                //status info
                List<StockStatusHistory> statusList = statusService.findByStockId(newStock.getId());
                if(statusList.isEmpty()){
                    //save new status history item
                    statusService.save(new StockStatusHistory(newStock.getId(),newStock.getStatus(),newStock.getStatusDate()),newStock,Boolean.TRUE);
                    System.out.println("processStockList " + isBreeder + ": status new saved:" + newStock.getStatus() + " : " + newStock.getStatusDate());
                }else{
                    //update existing imported item
                    statusList.get(0).setStatusName(newStock.getStatus());
                    statusService.save(statusList.get(0),newStock,Boolean.TRUE);
                    System.out.println("processStockList " + isBreeder + ": status updated:" + newStock.getStatus() + " : " + newStock.getStatusDate());
                }
                //weight info to save if > 0
                if(newStock.getWeight()>0){
                    List<StockWeightHistory> weightList = weightService.findByStockId(newStock.getId());
                    if(weightList.isEmpty()){
                        //save new weight history item
                        weightService.save(new StockWeightHistory(newStock.getId(),newStock.getWeight(),newStock.getWeightDate()),newStock);
                        System.out.println("processStockList " + isBreeder + ": weight new saved:" + newStock.getWeight() + " : " + newStock.getWeightDate());
                    }else{
                        //update existing imported item
                        weightList.get(0).setWeight(newStock.getWeight());
                        weightService.save(weightList.get(0),newStock);
                        System.out.println("processStockList " + isBreeder + ": weight updated:" + newStock.getWeight() + " : " + newStock.getWeightDate());
                    }
                }
                System.out.println("processStockList " + isBreeder + ": item after save:" + item.toString());
            }
            counter++;
            notifyProgressUpdate(counter);
        }
    }
    
    private void processStockParents(List<Stock> importList) {
        //run to update stock for fields that could be in the imported stock (mother/father)
        notifyProgressMax(Double.valueOf(importList.size()));
        Double counter = 0.0;
        notifyProgressStatus("Processing all Parent records from everbreed.");
        for(Stock item: importList){
            //process the import
            Integer fatherId = null;
            Integer motherId = null;
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
                stockRepository.saveAndFlush(item);
                System.out.println("processStockParents: item after save:" + item.toString());
            }else{
                System.out.println("processStockParents: item does not need saving:" + item.toString());
            }
            counter++;
            notifyProgressUpdate(counter);
        }
    }
    
    private void processLitterList(List<Litter> importList) {
        notifyProgressMax(Double.valueOf(importList.size()));
        Double counter = 0.0;
        notifyProgressStatus("Processing all Litter records from everbreed.");
        List<Litter> litterMissingKits = new ArrayList<>();
        for(Litter item: importList){
            System.out.println("processLitterList: processing item:" + item.toString());
            item.setNeedsSaving(Boolean.FALSE);
            Integer fatherId = null;
            Integer motherId = null;
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
                litterRepository.saveAndFlush(item);
                System.out.println("processLitterList: item after save:" + item.toString());
            }else{
                System.out.println("processLitterList: item does not need saving:" + item.toString());
            }
            
            //loop to find matching kits and store when you do not find a match
            //work around null mother and/or father
            if(item.getMother()==null || item.getFather()==null){
                System.out.println("processLitterList: " + item.toString() + ": has NULL Mother and/or Father - skipping kit processing");
            }else{
                List<Stock> kitsFromLitter = stockRepository.findAllKitsByMotherFatherDoB(item.getMother().getId(), item.getFather().getId(), item.getDoB());
                for(Stock kit: kitsFromLitter){
                    System.out.println("processLitterList: found kit:" + kit.toString());
                    kit.setLitter(item);
                    stockRepository.saveAndFlush(kit);
                }
                if(kitsFromLitter.size()<item.getKitsSurvivedCount()){
                    litterMissingKits.add(item);
                }
            }
            counter++;
            notifyProgressUpdate(counter);
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

    public void addListener(ProgressBarUpdateListener listener){
        listProgressListeners.add(listener);
    }

    private void notifyProgressUpdate(Double value){
        System.out.println("notifyProgressUpdate:" + value);
        for (ProgressBarUpdateListener listener: listProgressListeners) {
            listener.progressUpdate(value);
        }
    }

    private void notifyProgressMax(Double value){
        System.out.println("notifyProgressMax:" + value);
        for (ProgressBarUpdateListener listener: listProgressListeners) {
            listener.progressMax(value);
        }
    }

    private void notifyProgressStatus(String value){
        System.out.println("notifyProgressStatus:" + value);
        for (ProgressBarUpdateListener listener: listProgressListeners) {
            listener.progressStatusMessage(value);
        }
    }

    
}
