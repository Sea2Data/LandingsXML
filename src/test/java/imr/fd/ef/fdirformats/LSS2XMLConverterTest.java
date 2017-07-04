/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import LandingsTypes.v1.LandingsdataType;
import LandingsTypes.v1.SeddellinjeType;
import java.io.File;
import java.io.InputStream;
import java.util.List;
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
public class LSS2XMLConverterTest {
    
    public LSS2XMLConverterTest() {
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
     * Test of main method, of class LSS2XMLConverter.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        
        File tmp = File.createTempFile("landinger_", ".tmp");
        tmp.deleteOnExit();
                
        File lss = new File(LSS2XMLConverterTest.class.getClassLoader().getResource("FDIR_HI_LSS_FANGST_2015_100_lines.psv").toURI());
        
        long size = tmp.length();
        
        String[] args = {lss.getAbsolutePath(), tmp.getAbsolutePath(), "iso-8859-1"};
        LSS2XMLConverter.main(args);

        assertTrue(tmp.length() > size);
    }

    /**
     * Test of convertFile method, of class LSS2XMLConverter.
     */
    @Test
    public void testConvertFile() throws Exception {
        System.out.println("convertFile");
        File lss = null;
        File xml = null;
        LSS2XMLConverter.convertFile(lss, xml, null);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of readLSS method, of class LSS2XMLConverter.
     */
    @Test
    public void testReadLSS() throws Exception {
        System.out.println("readLSS");
        InputStream lss = null;
        LSS2XMLConverter instance = new LSS2XMLConverter();
        List<List<String>> expResult = null;
        List<List<String>> result = instance.readLSS(lss);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of processHeader method, of class LSS2XMLConverter.
     */
    @Test
    public void testProcessHeader() {
        System.out.println("processHeader");
        List<String> headerRow = null;
        LSS2XMLConverter instance = new LSS2XMLConverter();
        instance.processHeader(headerRow);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of processRows method, of class LSS2XMLConverter.
     */
    @Test
    public void testProcessRows() throws Exception {
        System.out.println("processRows");
        List<List<String>> rows = null;
        LSS2XMLConverter instance = new LSS2XMLConverter();
        LandingsdataType expResult = null;
        LandingsdataType result = instance.processRows(rows);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of processRow method, of class LSS2XMLConverter.
     */
    @Test
    public void testProcessRow() throws Exception {
        System.out.println("processRow");
        List<String> row = null;
        LSS2XMLConverter instance = new LSS2XMLConverter();
        SeddellinjeType expResult = null;
        SeddellinjeType result = instance.processRow(row);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
