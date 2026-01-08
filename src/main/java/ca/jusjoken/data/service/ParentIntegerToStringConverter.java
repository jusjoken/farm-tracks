/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

/**
 *
 * @author birch
 */
public class ParentIntegerToStringConverter implements Converter<String, Integer>{
    private StockService stockService;
    private Stock source;
    private Utility.Gender gender;

    public ParentIntegerToStringConverter(Stock source, Utility.Gender gender) {
        this.stockService = Registry.getBean(StockService.class);
        this.source = source;
        this.gender = gender;
    }

    @Override
    public Result<Integer> convertToModel(String prsntn, ValueContext vc) {
        //System.out.println("convertToModel: string:" + gender.getShortName() + " : " + prsntn);

        if(gender.equals(Utility.Gender.MALE) && source.getFatherExtName() !=null && !source.getFatherExtName().isEmpty()){
            //System.out.println("convertToPresentation: external so -1");
            return Result.ok(-1);
        }else if(gender.equals(Utility.Gender.FEMALE) && source.getMotherExtName() !=null && !source.getMotherExtName().isEmpty()){
            //System.out.println("convertToPresentation: external so -1");
            return Result.ok(-1);
        }
        if(gender.equals(Utility.Gender.MALE) && source.getFatherId()!=null){
            return Result.ok(source.getFatherId());
        }else if(gender.equals(Utility.Gender.FEMALE) && source.getMotherId()!=null){
            return Result.ok(source.getMotherId());
        }
        return Result.error("No parent found");
    }

    @Override
    public String convertToPresentation(Integer model, ValueContext vc) {
        //System.out.println("convertToPresentation: id:" + gender.getShortName() + " : "  + model + " source:" + source);
        if(gender.equals(Utility.Gender.MALE) && source.getFatherExtName() !=null && !source.getFatherExtName().isEmpty()){
            //Stock returnStock = stockService.getParentExt(source.getFatherExtName(), source.getStockType());
            //System.out.println("convertToPresentation: returnStock:" + gender.getShortName() + " : "  + source.getFatherExtName());
            return source.getFatherExtName();
        }else if(gender.equals(Utility.Gender.FEMALE) && source.getMotherExtName() !=null && !source.getMotherExtName().isEmpty()){
            //Stock returnStock = stockService.getParentExt(source.getMotherExtName(), source.getStockType());
            //System.out.println("convertToPresentation: returnStock:" + gender.getShortName() + " : "  + source.getMotherExtName());
            return source.getMotherExtName();
        }
        if(model==null) return "";
        Stock returnStock = stockService.findById(model);
        if(returnStock==null) return "";
        return returnStock.getDisplayName();
    }
    
}
