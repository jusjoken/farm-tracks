/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Stock;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import java.time.format.DateTimeFormatter;

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
        if(stock.getStatusDate()==null) return stock.getStatus();
        return stock.getStatus() + " - " + stock.getStatusDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
    }
    
}
