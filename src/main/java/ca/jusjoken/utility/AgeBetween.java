/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.utility;

import java.time.LocalDate;
import java.time.Period;

/**
 *
 * @author birch
 */
public class AgeBetween {
    private Integer years = 0;
    private Integer months = 0;
    private Integer weeks = 0;
    private Integer days = 0;

    public AgeBetween(LocalDate date1, LocalDate date2) {
        if(date1==null || date2==null){
            //do nothing
        }else{
            this.years = Period.between(date1, date2).getYears();
            this.months = Period.between(date1, date2).getMonths();
            Integer tempDays = Period.between(date1, date2).getDays();
            this.weeks = tempDays/7;
            this.days = tempDays%7;
        }
        
    }

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getWeeks() {
        return weeks;
    }

    public Integer getDays() {
        return days;
    }
    
    public String getAgeFormattedHTML(){
        //String retVal = "<p>1<sup>yr</sup> 2<sup>mo</sup> 3<sup>wk</sup></p>";
        String sup = "<sup>";
        String supEnd = "</sup>";
        String retVal = "<p>";
        if(this.years>0 && this.months>0){
            retVal += this.years + sup + "yr" + supEnd + " " + this.months + sup + "mo" + supEnd + "</p>";
        }else if(this.years>0 && this.weeks>0){
            retVal += this.years + sup + "yr" + supEnd + " " + this.weeks + sup + "wk" + supEnd + "</p>";
        }else if(this.years>0 && this.days>0){
            retVal += this.years + sup + "yr" + supEnd + " " + this.days + sup + "dy" + supEnd + "</p>";
        }else if(this.years>0){
            retVal += this.years + sup + "yr" + supEnd + "</p>";
        }else if(this.months>0 && this.weeks>0){
            retVal += this.months + sup + "mo" + supEnd + " " + this.weeks + sup + "wk" + supEnd + "</p>";
        }else if(this.months>0 && this.days>0){
            retVal += this.months + sup + "mo" + supEnd + " " + this.days + sup + "dy" + supEnd + "</p>";
        }else if(this.months>0){
            retVal += this.months + sup + "mo" + supEnd + "</p>";
        }else if(this.weeks>0){
            retVal += this.weeks + sup + "wk" + supEnd + " " + this.days + sup + "dy" + supEnd + "</p>";
        }else if(this.days>0){
            retVal += this.days + sup + "dy" + supEnd + "</p>";
        }else{
            retVal += "--</p>";
        }
        return retVal;
    }

    @Override
    public String toString() {
        return "AgeBetween{" + "years=" + years + ", months=" + months + ", weeks=" + weeks + '}';
    }
    
    
}
