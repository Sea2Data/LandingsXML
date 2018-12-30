/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package XMLadapters;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class IntegerConstantAdapter extends XmlAdapter<String, Integer>{

    protected static Map<String,Integer> integerpool = new HashMap<>();
    
    @Override
    public Integer unmarshal(String v) throws Exception {
        if (!IntegerConstantAdapter.integerpool.containsKey(v)){
            IntegerConstantAdapter.integerpool.put(v, Integer.parseInt(v));
        }
        return IntegerConstantAdapter.integerpool.get(v);
    }

    @Override
    public String marshal(Integer v) throws Exception {
        return v.toString();
    }
    
}
