/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import ca.jusjoken.data.entity.Stock;

/**
 *
 * @author birch
 */
public class StatusHistoryConverter implements Converter<String, String>{
    private Stock stock;

    public StatusHistoryConverter(Stock stock) {
        this.stock = stock;
    }
    
    @Override
    public Result<String> convertToModel(String prsntn, ValueContext vc) {
        //never used as is ReadOnly
        return Result.ok("ok");
    }

    @Override
    public String convertToPresentation(String model, ValueContext vc) {
        String displayStatus = stock.getEffectiveStatusKey();
        if (stock.isDisplayStatusOverriddenBySaleStatus()) {
            return displayStatus;
        }
        if(stock.getStatusDate()==null) return displayStatus;
        return displayStatus + " - " + stock.getStatusDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
    }
    
}
