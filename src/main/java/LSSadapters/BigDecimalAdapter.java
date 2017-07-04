/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LSSadapters;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class BigDecimalAdapter {

    protected DecimalFormat format = new DecimalFormat();

    public BigDecimalAdapter() {
        DecimalFormatSymbols df = new DecimalFormatSymbols();
        df.setDecimalSeparator(',');
        format.setDecimalFormatSymbols(df);
        format.setParseBigDecimal(true);
    }

    public BigDecimal parse(String val) throws Exception {
        return (BigDecimal) this.format.parse(val);
    }

    public String write(BigDecimal val) throws Exception {
        if (val != null) {
            return format.format(val.doubleValue());
        } else {
            return null;
        }
    }
}
