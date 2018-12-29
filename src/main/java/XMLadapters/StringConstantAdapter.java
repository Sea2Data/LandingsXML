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
 * String adapter that returns reference to the same of any repeating string.
 * As strings are immutable, the use should of this adapter compared to regular string unmarshalling should be transparent.
 * Memory usage will be affected such that memory usage will decrease if there is a lot of repetition among unmarshalled strings, and increase if there is little or no repetition.
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class StringConstantAdapter extends XmlAdapter<String, String>{

    protected static Map<String,String> stringpool = new HashMap<>();
    
    @Override
    public String unmarshal(String v) throws Exception {
        if (!StringConstantAdapter.stringpool.containsKey(v)){
            StringConstantAdapter.stringpool.put(v, v);
        }
        return StringConstantAdapter.stringpool.get(v);
    }

    @Override
    public String marshal(String v) throws Exception {
        return v;
    }
    
}
