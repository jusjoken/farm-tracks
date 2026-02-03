/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.Stock;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

/**
 *
 * @author birch
 */
public class ParentIntegerToStockConverter implements Converter<Stock, Integer>{
    private StockService stockService;
    private Stock source;
    private Gender gender;

    public ParentIntegerToStockConverter(Stock source, Gender gender) {
        this.stockService = Registry.getBean(StockService.class);
        this.source = source;
        this.gender = gender;
    }
    
    @Override
    public Result<Integer> convertToModel(Stock prsntn, ValueContext vc) {
        //System.out.println("convertToModel: stock:" + gender.getShortName() + " : " + prsntn);
        if(prsntn==null) return Result.ok(null);  //as parent can be blank need to return an Ok result
        return Result.ok(prsntn.getId());
    }

    @Override
    public Stock convertToPresentation(Integer model, ValueContext vc) {
        //System.out.println("convertToPresentation: id:" + gender.getShortName() + " : "  + model + " source:" + source);
        if(gender.equals(Gender.MALE) && source.getFatherExtName() !=null && !source.getFatherExtName().isEmpty()){
            Stock returnStock = stockService.getParentExt(source.getFatherExtName(), source.getStockType());
            //System.out.println("convertToPresentation: returnStock:" + gender.getShortName() + " : "  + returnStock);
            return returnStock;
        }else if(gender.equals(Gender.FEMALE) && source.getMotherExtName() !=null && !source.getMotherExtName().isEmpty()){
            Stock returnStock = stockService.getParentExt(source.getMotherExtName(), source.getStockType());
            //System.out.println("convertToPresentation: returnStock:" + gender.getShortName() + " : "  + returnStock);
            return returnStock;
        }
        return stockService.findById(model);
    }
    
}
