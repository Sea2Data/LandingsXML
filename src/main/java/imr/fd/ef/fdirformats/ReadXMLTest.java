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

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class ReadXMLTest {
    
    public static void main(String[] args) throws FileNotFoundException, JAXBException, XMLStreamException{
        String xml = "/Users/a5362/landingsets/xml/2015.xml";
        System.out.println("Start parse ...");
        long start = System.currentTimeMillis();
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        long stop = System.currentTimeMillis();
        System.out.println("End parse ...");
        System.out.println("Time: " + (stop-start)/1000 +  "s");
        System.out.println(l.getSeddellinje().size());
        System.out.println(l.getSeddellinje().get(10).getArt().getArtBokmål());
    }
}
