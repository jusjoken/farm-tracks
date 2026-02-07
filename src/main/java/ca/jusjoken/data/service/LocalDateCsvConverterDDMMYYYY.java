/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

/**
 *
 * @author birch
 */
public class LocalDateCsvConverterDDMMYYYY extends AbstractBeanField<LocalDateTime, String>{

    @Override
    protected Object convert(String string) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        DateTimeFormatter formatter;
        LocalDateTime retDate = null;
        String[] inputFormats = {"dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy"}; // Possible input formats
        
        for (String inputFormat : inputFormats) {
            try{
                formatter = DateTimeFormatter.ofPattern(inputFormat);
                retDate = LocalDate.parse(string, formatter).atStartOfDay();
                break;
            }catch(DateTimeParseException e){
                //try next formatter
            }
        }
        return retDate;
    }
    
}
