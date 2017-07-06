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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    FlatTableMaker converter;
    DelimitedOutputWriter writer;

    public XML2LSSConverter() throws JAXBException, ParserConfigurationException {
        this.converter = new FlatTableMaker(new SchemaReader(XML2LSSConverter.class.getClassLoader().getResourceAsStream("landinger.xsd")), new LandingsLeafNodeHandler());
        converter.setNamingConvention(new DoNothingNamingConvention());
        this.writer = new DelimitedOutputWriter("|", "\\", null, ".psv", "");
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
        if (lss.length() > 0) {
            throw new FileAlreadyExistsException("Landings lss file already exist");
        }
        XML2LSSConverter.convertFile(landings_xml, lss);
    }

    public static void convertFile(File xml, File lss_file) throws JAXBException, ParserConfigurationException, XMLStreamException, ITableMakerNamingConvention.NamingException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, RelationalConvertionException, IOException {
        XML2LSSConverter conv = new XML2LSSConverter();
        
        PrintStream stream = new PrintStream(new FileOutputStream(lss_file));
        LandingsdataType data = IO.parse(new FileInputStream(xml), LandingsdataType.class);
        Iterator<List<String>> it = conv.converter.getTableContentIterator(data.getSeddellinje());
        conv.writer.writeLine(conv.converter.getHeaders(data.getSeddellinje().get(0)), stream);
        while (it.hasNext()){
            conv.writer.writeLine(it.next(), stream);
        }
        stream.close();
    }

    private static class LandingsLeafNodeHandler implements ILeafNodeHandler {

        protected BigDecimalAdapter adapter;

        public LandingsLeafNodeHandler() {
            adapter = new BigDecimalAdapter();
        }

        @Override
        public String extractValue(Object node) throws ClassCastException {
            if (node == null) {
                return null;
            }
            if (node instanceof BigDecimal) {
                try {
                    return this.adapter.write((BigDecimal) node);
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
