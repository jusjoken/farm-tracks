/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 *
 * @author birch
 */
@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String>{

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return gender.getShortName();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return Gender.fromShortName(dbData);
    }
    
}
