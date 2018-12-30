/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.IO;
import LandingsTypes.v1.LandingsdataType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class XMLReadTest {
    
    @Test
    public void testRead() throws Exception{
        File xml = new File(XMLReadTest.class.getClassLoader().getResource("landinger_100_lines.xml").toURI());
        
        //read xml
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        // save to temp
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.save(baos, l);
        String m1 = baos.toString();

        //read raw
        ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        byte[] b = java.nio.file.Files.readAllBytes(xml.toPath());
        baos3.write(b, 0, b.length);

        String m3 = baos3.toString();

        //check equality
        assertEquals(m1, m3);
    }
    
       @Test
    public void testChange() throws Exception{
        File xml = new File(XMLReadTest.class.getClassLoader().getResource("landinger_100_lines.xml").toURI());
        
        //read xml
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        l.getSeddellinje().get(19).setArtKode("test");
        // save to temp
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IO.save(baos, l);
        String m1 = baos.toString();

        //read raw
        ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
        byte[] b = java.nio.file.Files.readAllBytes(xml.toPath());
        baos3.write(b, 0, b.length);

        String m3 = baos3.toString();

        //check equality
        assertNotEquals(m1, m3);
    }
    
    @Test
    public void testChangeStringConstant() throws Exception{
        File xml = new File(XMLReadTest.class.getClassLoader().getResource("landinger_100_lines.xml").toURI());
        
        //read xml
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        assertEquals(l.getSeddellinje().size(), 99);
        String l20 = l.getSeddellinje().get(20).getArtKode();
        assertEquals(l20, l.getSeddellinje().get(20).getArtKode());
        assertEquals(l20, l.getSeddellinje().get(19).getArtKode());
        l.getSeddellinje().get(19).setArtKode("test");
        assertNotEquals(l20, l.getSeddellinje().get(19).getArtKode());
        
    }
    
    @Test
    public void testIntegerStringConstant() throws Exception{
        File xml = new File(XMLReadTest.class.getClassLoader().getResource("landinger_100_lines.xml").toURI());
        
        //read xml
        LandingsdataType l = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        assertEquals(l.getSeddellinje().size(), 99);
        Integer l20 = l.getSeddellinje().get(20).getKystHavKode();
        assertEquals(l20, l.getSeddellinje().get(20).getKystHavKode());
        assertEquals(l20, l.getSeddellinje().get(19).getKystHavKode());
        l.getSeddellinje().get(19).setKystHavKode(1);
        assertNotEquals(l20, l.getSeddellinje().get(19).getKystHavKode());
        
    }

    
}
