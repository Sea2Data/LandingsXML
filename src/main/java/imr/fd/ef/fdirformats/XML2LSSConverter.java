/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.IO;
import HierarchicalData.RelationalConversion.DelimitedOutputWriter;
import HierarchicalData.RelationalConversion.FlatTableMaker;
import HierarchicalData.RelationalConversion.ILeafNodeHandler;
import HierarchicalData.RelationalConversion.NamingConventions.DoNothingNamingConvention;
import HierarchicalData.RelationalConversion.NamingConventions.ITableMakerNamingConvention;
import HierarchicalData.RelationalConversion.RelationalConvertionException;
import HierarchicalData.RelationalConversion.TabConverter;
import HierarchicalData.RelationalConversion.TableMaker;
import HierarchicalData.SchemaReader;
import LSSadapters.BigDecimalAdapter;
import LandingsTypes.v1.LandingsdataType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class XML2LSSConverter {
    
    TabConverter converter;
    
    public XML2LSSConverter() throws JAXBException, ParserConfigurationException{
        TableMaker tablemaker = new FlatTableMaker(new SchemaReader(XML2LSSConverter.class.getClassLoader().getResourceAsStream("landinger.xsd")),new LandingsLeafNodeHandler());
        tablemaker.setNamingConvention(new DoNothingNamingConvention());
        this.converter = new TabConverter(tablemaker, LandingsdataType.class);
        this.converter.setWriter(new DelimitedOutputWriter("|", "\\", null, ".psv", ""));
    }
    
        public static void main(String[] args) throws LSSProcessingException, IOException, FileNotFoundException, Exception {
        if (args.length < 2) {
            SchemaReader schemareader = new SchemaReader(LSS2XMLConverter.class.getClassLoader().getResourceAsStream("landinger.xsd"));
            System.out.println("Converts xml-format: " + schemareader.getTargetNameSpace() + "to LSS file ");
            System.out.println("Memory intensive for large files. Consider increasing heap size, java option: -Xmx<maxheapsize>, e.g: -Xmx8g");
            System.out.println("Usage: <xml file> <LSS file>");
            System.exit(0);
        }
        File landings_xml = new File(args[0]);
        File lss = new File(args[1]);

        XML2LSSConverter.convertFile(landings_xml, lss);
    }
    
    public static void convertFile(File xml, File lss_directory) throws JAXBException, ParserConfigurationException, XMLStreamException, ITableMakerNamingConvention.NamingException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, RelationalConvertionException, IOException{
        XML2LSSConverter conv = new XML2LSSConverter();
        conv.converter.convertFile(xml, lss_directory);
    }

    private static class LandingsLeafNodeHandler implements ILeafNodeHandler{

        protected BigDecimalAdapter adapter;
        
        public LandingsLeafNodeHandler() {
            adapter = new BigDecimalAdapter();
        }

        @Override
        public String extractValue(Object node) throws ClassCastException {
            if (node == null){
                return null;
            }
            if (node instanceof BigDecimal){
                try {
                    return this.adapter.write((BigDecimal)node);
                } catch (Exception ex) {
                    throw new ClassCastException();
                }
            }
            
            return node.toString();
        }

        @Override
        public Set getLeafNodeComplexTypes() {
            return new HashSet<String>();
        }
    }
    
}
