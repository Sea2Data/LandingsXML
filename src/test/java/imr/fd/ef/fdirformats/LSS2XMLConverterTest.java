/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import java.io.File;
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
     * Test of main method, of class LSS2XML_0_1_Converter.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");

        File tmp = File.createTempFile("landinger_test_main_", ".tmp");
        tmp.deleteOnExit();

        File lss = new File(LSS2XMLConverterTest.class.getClassLoader().getResource("FDIR_HI_LSS_FANGST_2015_100_lines.psv").toURI());

        long size = tmp.length();

        String[] args = {lss.getAbsolutePath(), tmp.getAbsolutePath(), "iso-8859-1"};
        LSS2XMLConverter.main(args);

        assertTrue(tmp.length() > size);
    }

    /**
     * Test of convertFile method, of class LSS2XML_0_1_Converter.
     */
    @Test
    public void testConvertFile() throws Exception {
        System.out.println("convertFile");

        File lss = new File(LSS2XMLConverterTest.class.getClassLoader().getResource("FDIR_HI_LSS_FANGST_2015_100_lines.psv").toURI());
        File xml = File.createTempFile("landinger_test_convert_", ".tmp");

        long size = xml.length();

        xml.deleteOnExit();
        LSS2XMLConverter.convertFile(lss, xml, "iso-8859-1");

        assertTrue(xml.length() > size);

    }

    /**
     * Test of convertFile method, of class LSS2XML_0_1_Converter.
     */
    //test removed with change to streeaming conversion
    //@Test
    public void testConvertFileException() throws Exception {
        System.out.println("convertFile");

        File lss = new File(LSS2XMLConverterTest.class.getClassLoader().getResource("FDIR_HI_LSS_FANGST_2015_100_lines_extra_column.psv").toURI());
        File xml = File.createTempFile("landinger_test_convert_", ".tmp");
        xml.deleteOnExit();

        long size = xml.length();

        try {
            LSS2XMLConverter.convertFile(lss, xml, "iso-8859-1");
            fail("Exception expected");
        } catch (Exception e) {

        }

        assertTrue(xml.length() == size);

    }

 


}
