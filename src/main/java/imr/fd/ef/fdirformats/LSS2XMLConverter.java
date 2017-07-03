/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import LandingsTypes.v1.DellandingType;
import LandingsTypes.v1.DokumentType;
import LandingsTypes.v1.FangstdataType;
import LandingsTypes.v1.FartøyType;
import LandingsTypes.v1.FiskerType;
import LandingsTypes.v1.LandingOgProduksjonType;
import LandingsTypes.v1.LandingsdataType;
import LandingsTypes.v1.MottakendeFartøyType;
import LandingsTypes.v1.MottakerType;
import LandingsTypes.v1.ObjectFactory;
import LandingsTypes.v1.ProduktType;
import LandingsTypes.v1.RedskapType;
import LandingsTypes.v1.SalgslagType;
import LandingsTypes.v1.SeddellinjeType;
import adapters.DateAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 * Read LSS files and converts to xml
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class LSS2XMLConverter {

    ObjectFactory factory;
    Map<String, Integer> indexMap; // maps header of column to 0-baed column index.
    DateAdapter dateadapter;

    public LSS2XMLConverter() {
        this.factory = new ObjectFactory();
        this.indexMap = new HashMap<>();
        this.dateadapter = new DateAdapter();
    }

    public static void convertFile(File lss, File xml) throws JAXBException, FileNotFoundException, LSSProcessingException, IOException, Exception {
        InputStream instream = new FileInputStream(lss);
        LSS2XMLConverter converter = new LSS2XMLConverter();
        
        List<List<String>> rows = converter.readLSS(instream);
        
        converter.processHeader(rows.remove(0));
        LandingsdataType data = converter.processRows(rows);

        OutputStream stream = new FileOutputStream(xml);
        HierarchicalData.IO.save(stream, data);
    }

    /**
     * Read LSS file as pipe delimited file.
     * @param lss
     * @return Linked list of all rows in file, including header. Each row is an ArrayList
     * @throws IOException 
     */
    protected List<List<String>> readLSS(InputStream lss) throws IOException {
        InputStreamReader instream = new InputStreamReader(lss);
        BufferedReader in = new BufferedReader(instream);
        String delim = "|";

        List<List<String>> rows = new LinkedList<>();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            rows.add(new ArrayList<>(Arrays.asList(line.split(delim))));
        }
        
        return rows;
    }

    /**
     * Initializes indexmap from header row
     *
     * @param headerRow
     */
    protected void processHeader(List<String> headerRow) {
        int i = 0;
        for (String header : headerRow) {
            this.indexMap.put(header, i);
            i++;
        }
    }

    /**
     * Processes rows from LLS. Consumes row elements as processed
     *
     * @param rows
     * @return
     * @throws LSSProcessingException
     */
    protected LandingsdataType processRows(List<List<String>> rows) throws LSSProcessingException, Exception {
        LandingsdataType landingsdata = this.factory.createLandingsdataType();
        for (List<String> row : rows) {
            landingsdata.getSelldellinje().add(this.processRow(row));
        }
        return landingsdata;
    }

    /**
     * Processes row from LLS consumes elements of rows as processed
     *
     * @param row
     * @return
     * @throws LSSProcessingException if not all row elements are processed at
     * the end
     */
    protected SeddellinjeType processRow(List<String> row) throws LSSProcessingException, Exception {

        SeddellinjeType linje = this.factory.createSeddellinjeType();
        linje.setLinjenummer(new Long(row.remove(this.indexMap.get("Linjenummer").intValue())));
        linje.setDokument(this.processDokument(row));
        linje.setDellanding(this.processDellanding(row));
        linje.setFangstdata(this.processFangsData(row));
        linje.setFartøy(this.processFartøy(row));
        linje.setFisker(this.processFisker(row));
        linje.setMottakendefartøy(this.processMottakendeFartøy(row));
        linje.setMottaker(this.processMottaker(row));
        linje.setProduksjon(this.processProduksjon(row));
        linje.setProdukt(this.processProdukt(row));
        linje.setRedskap(this.processRedskap(row));
        linje.setSalgslag(this.processSalgslag(row));

        if (row.size() > 0) {
            throw new LSSProcessingException("Some columns not handled: " + row.toString());
        }

        return linje;
    }

    private DokumentType processDokument(List<String> row) throws Exception {
        DokumentType dokument = this.factory.createDokumentType();
        dokument.setDokumentnummer(row.remove(this.indexMap.get("Dokumentnummer").intValue()));
        dokument.setDokumenttypeKode(new BigInteger(row.remove(this.indexMap.get("Dokumenttype (kode)").intValue())));
        dokument.setDokumenttypeBokmål(row.remove(this.indexMap.get("Dokumenttype (bokmål)").intValue()));
        dokument.setDokumentVersjonsnummer(new BigInteger(row.remove(this.indexMap.get("Dokument versjonsnummer").intValue())));
        dokument.setDokumentFormulardato(this.dateadapter.unmarshal(row.remove(this.indexMap.get("Dokument formulardato").intValue())));
        dokument.setDokumentElektroniskDato(row.remove(this.indexMap.get("Dokument elektronisk dato").intValue()));
        
        return dokument;
    }

    private DellandingType processDellanding(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private FangstdataType processFangsData(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private FartøyType processFartøy(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private FiskerType processFisker(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private MottakendeFartøyType processMottakendeFartøy(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private MottakerType processMottaker(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private LandingOgProduksjonType processProduksjon(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private RedskapType processRedskap(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private SalgslagType processSalgslag(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ProduktType processProdukt(List<String> row) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
