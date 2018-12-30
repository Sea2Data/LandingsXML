/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.IO;
import LandingsTypes.v1.LandingsdataType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class ZippedReadExample {
    
    public static void main(String args[]) throws FileNotFoundException, JAXBException, XMLStreamException, IOException{
        File zipfile = new File(args[0]);
        //File zipfile = new File("/Users/a5362/landingsets/xml/compressed/2015.zip");
        
        ZipInputStream input = new ZipInputStream(new FileInputStream(zipfile));
        
        ZipEntry entry = input.getNextEntry();
        System.out.println("Start parse " + entry.getName() + " from zipped file: " + zipfile.toString());
        long start = System.currentTimeMillis();
        LandingsdataType l = IO.parse(input, LandingsdataType.class);
        long stop = System.currentTimeMillis();
        System.out.println("End parse ...");
        System.out.println("Time: " + (stop-start)/1000 +  "s");
        System.out.println("Landings in file: " + l.getSeddellinje().size());
        System.out.println("Species of first line: " + l.getSeddellinje().get(0).getArt().getArtBokmål());
        
        ZipEntry nextEntry = input.getNextEntry();
        if (nextEntry!=null){
            System.err.println("Does not support multiple files in zip");
            System.exit(1);
        }
        
    }
    
}
