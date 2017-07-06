/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class XML2LSSConverterTest {

    public XML2LSSConverterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of convertFile method, of class XML2LSSConverter.
     */
    @Test
    public void testConvertFile() throws Exception {
        System.out.println("convertFile");
        File xml = new File(XML2LSSConverterTest.class.getClassLoader().getResource("landinger_100_lines.xml").toURI());

        File targetDir = File.createTempFile("tmpTestConvertFileHD", "");
        targetDir.delete();
        targetDir.mkdir();
        targetDir.deleteOnExit();

        XML2LSSConverter.convertFile(xml, targetDir);

        
        File outp = new File(targetDir.getAbsolutePath() + "/LandingsdataType.psv");
        File lss = new File(LSS2XMLConverterTest.class.getClassLoader().getResource("FDIR_HI_LSS_FANGST_2015_100_lines.psv").toURI());
        
        assertEquals(countlines(outp), countlines(lss));
    }
    
    private int countlines(File f) throws FileNotFoundException, IOException{
        int nlines = 0;
        BufferedReader r;
        r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            nlines++;
        }
        return nlines;
    }

}
