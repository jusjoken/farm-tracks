/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 *
 * @author birch
 */
public class LocalDateCsvConverter extends AbstractBeanField{

    @Override
    protected Object convert(String string) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        DateTimeFormatter formatter;
        LocalDate retDate = null;
        String[] inputFormats = {"yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy"}; // Possible input formats
        
        for (String inputFormat : inputFormats) {
            try{
                formatter = DateTimeFormatter.ofPattern(inputFormat);
                retDate = LocalDate.parse(string, formatter);
                break;
            }catch(DateTimeParseException e){
                //try next formatter
            }
        }
        return retDate;
    }
    
}
