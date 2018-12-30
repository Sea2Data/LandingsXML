/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package XMLadapters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class LocalDateConstantAdapter extends XmlAdapter<String, LocalDate>{

   protected static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
   protected static Map<String, LocalDate> datepool = new HashMap<>();

    @Override
    public LocalDate unmarshal(String val) {
        if (!datepool.containsKey(val)){
            datepool.put(val, LocalDate.parse(val, dateFormatter));
        }
        return datepool.get(val);
    }

    @Override
    public String marshal(LocalDate val){
        return val != null ? val.format(dateFormatter) : null;
    }
        
}
