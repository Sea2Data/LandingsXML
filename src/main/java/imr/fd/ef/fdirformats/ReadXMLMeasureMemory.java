/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.IO;
import LandingsTypes.v1.LandingsdataType;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.openjdk.jol.info.GraphLayout;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class ReadXMLMeasureMemory {
    
    public static void main(String[] args) throws FileNotFoundException, JAXBException, XMLStreamException{
        String xml = args[0];
        System.out.println("Start parse: " + xml);
        long start = System.currentTimeMillis();
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        long stop = System.currentTimeMillis();
        System.out.println("End parse ...");
        System.out.println("Time: " + (stop-start)/1000 +  "s");
        System.out.println("Memory: " + GraphLayout.parseInstance(l).toFootprint());
    }
}
