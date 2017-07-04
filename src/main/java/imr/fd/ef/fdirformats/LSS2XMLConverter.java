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
import LandingsTypes.v1.KvoteType;
import LandingsTypes.v1.LandingOgProduksjonType;
import LandingsTypes.v1.LandingsdataType;
import LandingsTypes.v1.MottakendeFartøyType;
import LandingsTypes.v1.MottakerType;
import LandingsTypes.v1.ObjectFactory;
import LandingsTypes.v1.ProduktType;
import LandingsTypes.v1.RedskapType;
import LandingsTypes.v1.SalgslagType;
import LandingsTypes.v1.SeddellinjeType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 * Read LSS files and converts to xml.
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class LSS2XMLConverter {

    ObjectFactory factory;
    Map<String, Integer> indexMap; // maps header of column to 0-baed column index.

    public LSS2XMLConverter() {
        this.factory = new ObjectFactory();
        this.indexMap = new HashMap<>();
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
        linje.setKvote(this.processKvote(row));

        if (row.size() > 0) {
            throw new LSSProcessingException("Some columns not handled: " + row.toString());
        }

        return linje;
    }

    private DokumentType processDokument(List<String> row) throws Exception {
        int lpre = row.size();
        DokumentType dokument = this.factory.createDokumentType();
        dokument.setDokumentnummer(row.remove(this.indexMap.get("Dokumentnummer").intValue()));
        dokument.setDokumenttypeKode(new BigInteger(row.remove(this.indexMap.get("Dokumenttype (kode)").intValue())));
        dokument.setDokumenttypeBokmål(row.remove(this.indexMap.get("Dokumenttype (bokmål)").intValue()));
        dokument.setDokumentVersjonsnummer(new BigInteger(row.remove(this.indexMap.get("Dokument versjonsnummer").intValue())));        
        dokument.setDokumentFormulardato(row.remove(this.indexMap.get("Dokument formulardato").intValue()));
        dokument.setDokumentElektroniskDato(row.remove(this.indexMap.get("Dokument elektronisk dato").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        
        return dokument;
    }

    private DellandingType processDellanding(List<String> row) {
        int lpre = row.size();
        DellandingType dell = this.factory.createDellandingType();
        dell.setDellandingSignal(new BigInteger(row.remove(this.indexMap.get("Dellanding (signal)").intValue())));
        dell.setForrigeMottakstasjon(row.remove(this.indexMap.get("Neste mottaksstasjon").intValue()));
        dell.setNesteMottaksstasjon(row.remove(this.indexMap.get("Forrige mottakstasjon").intValue()));
        
        if (lpre == row.size()){
            return null;
        }

        return dell;
    }

    private FangstdataType processFangsData(List<String> row) {
        int lpre = row.size();
        
        FangstdataType fangst = this.factory.createFangstdataType();
        fangst.setFangstdagbokNummer(new Long(row.remove(this.indexMap.get("Fangstdagbok (nummer)").intValue())));
        fangst.setFangstdagbokTurnummer(new Long(row.remove(this.indexMap.get("Fangstdagbok (turnummer)").intValue())));
        fangst.setFangstfeltKode(row.remove(this.indexMap.get("Fangstfelt (kode)").intValue()));
        fangst.setFangstår(new BigInteger(row.remove(this.indexMap.get("Fangstår").intValue())));
        fangst.setHovedområdeBokmål(row.remove(this.indexMap.get("Hovedområde (bokmål)").intValue()));
        fangst.setHovedområdeFAOBokmål(row.remove(this.indexMap.get("Hovedområde FAO (bokmål)").intValue()));
        fangst.setHovedområdeFAOKode(row.remove(this.indexMap.get("Hovedområde FAO (kode)").intValue()));
        fangst.setHovedområdeKode(new BigInteger(row.remove(this.indexMap.get("Hovedområde (kode)").intValue())));
        fangst.setKystHavKode(new BigInteger(row.remove(this.indexMap.get("Kyst/hav (kode)").intValue())));
        fangst.setLokasjonKode(row.remove(this.indexMap.get("Lokasjon (kode)").intValue()));
        fangst.setNordSørFor62GraderNord(row.remove(this.indexMap.get("Nord/sør for 62 grader nord").intValue()));
        fangst.setOmrådegrupperingBokmål(row.remove(this.indexMap.get("Områdegruppering (bokmål)").intValue()));
        fangst.setSisteFangstdato(row.remove(this.indexMap.get("Siste fangstdato").intValue()));
        fangst.setSoneBokmål(row.remove(this.indexMap.get("Sone (bokmål)").intValue()));
        fangst.setSoneKode(row.remove(this.indexMap.get("Sone (kode)").intValue()));
        
        if (row.size() == lpre){
            return null;
        }
        
        return fangst;
    }

    private FartøyType processFartøy(List<String> row) {
        int lpre = row.size();
        
        FartøyType fartøy = this.factory.createFartøyType();
        fartøy.setBruttotonnasje1969(new BigInteger(row.remove(this.indexMap.get("Bruttotonnasje 1969").intValue())));
        fartøy.setBruttotonnasjeAnnen(new BigInteger(row.remove(this.indexMap.get("Bruttotonnasje annen").intValue())));
        fartøy.setByggeår(new BigInteger(row.remove(this.indexMap.get("Byggeår").intValue())));
        fartøy.setFartøyGjelderFraDato(row.remove(this.indexMap.get("Fartøy gjelder fra dato").intValue()));
        fartøy.setFartøyGjelderTilDato(row.remove(this.indexMap.get("Fartøy gjelder til dato").intValue()));
        fartøy.setFartøyID(row.remove(this.indexMap.get("Fartøy ID").intValue()));
        fartøy.setFartøyfylke(row.remove(this.indexMap.get("Fartøyfylke").intValue()));
        fartøy.setFartøyfylkeKode(new BigInteger(row.remove(this.indexMap.get("Fartøyfylke (kode)").intValue())));
        fartøy.setFartøykommune(row.remove(this.indexMap.get("Fartøykommune").intValue()));
        fartøy.setFartøykommuneKode(new BigInteger(row.remove(this.indexMap.get("Fartøykommune (kode)").intValue())));
        fartøy.setFartøynasjonalitetBokmål(row.remove(this.indexMap.get("Fartøynasjonalitet (bokmål)").intValue()));
        fartøy.setFartøynasjonalitetKode(row.remove(this.indexMap.get("Fartøynasjonalitet (kode)").intValue()));
        fartøy.setFartøynavn(row.remove(this.indexMap.get("Fartøynavn").intValue()));
        fartøy.setFartøytypeBokmål(row.remove(this.indexMap.get("Fartøytype (bokmål)").intValue()));
        fartøy.setFartøytypeKode(row.remove(this.indexMap.get("Fartøytype (kode)").intValue()));
        fartøy.setLengdegruppeKode(row.remove(this.indexMap.get("Lengdegruppe (kode)").intValue()));
        fartøy.setLengdegruppeBokmål(row.remove(this.indexMap.get("Lengdegruppe (bokmål)").intValue()));
        fartøy.setMotorbyggeår(new BigInteger(row.remove(this.indexMap.get("Motorbyggeår").intValue())));
        fartøy.setMotorkraft(new BigInteger(row.remove(this.indexMap.get("Motorkraft").intValue())));
        fartøy.setOmbyggingsår(new BigInteger(row.remove(this.indexMap.get("Ombyggingsår").intValue())));
        fartøy.setRadiokallesignalSeddel(row.remove(this.indexMap.get("Radiokallesignal (seddel)").intValue()));
        fartøy.setRegistreringsmerkeSeddel(row.remove(this.indexMap.get("Registreringsmerke (seddel)").intValue()));
        fartøy.setStørsteLengde(new BigDecimal(row.remove(this.indexMap.get("Største lengde").intValue())));

        if (lpre == row.size()){
            return null;
        }
        
        return fartøy;
    }

    private FiskerType processFisker(List<String> row) {
        int lpre = row.size();
        
        FiskerType fisker = this.factory.createFiskerType();
        fisker.setFiskerkommune(row.remove(this.indexMap.get("Fiskerkommune").intValue()));
        fisker.setFiskerkommuneKode(new BigInteger(row.remove(this.indexMap.get("Fiskerkommune (kode)").intValue())));
        fisker.setFiskernasjonalitetBokmål(row.remove(this.indexMap.get("Fiskernasjonalitet (bokmål)").intValue()));
        fisker.setFiskernasjonalitetKode(row.remove(this.indexMap.get("Fiskernasjonalitet (kode)").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        
        return fisker;
    }

    private MottakendeFartøyType processMottakendeFartøy(List<String> row) {
        int lpre = row.size();
        
        MottakendeFartøyType mfart = this.factory.createMottakendeFartøyType();
        mfart.setMottakendeFartøyRKAL(row.remove(this.indexMap.get("Mottakende fartøy rkal").intValue()));
        mfart.setMottakendeFartøyRegMerke(row.remove(this.indexMap.get("Mottakende fartøy reg.merke").intValue()));
        mfart.setMottakendeFartøynasjBokmål(row.remove(this.indexMap.get("Mottakende fart.nasj (bokmål)").intValue()));
        mfart.setMottakendeFartøynasjKode(row.remove(this.indexMap.get("Mottakende fartøynasj. (kode)").intValue()));
        mfart.setMottakendeFartøytypeBokmål(row.remove(this.indexMap.get("Mottakende fart.type (bokmål)").intValue()));
        mfart.setMottakendeFartøytypeKode(row.remove(this.indexMap.get("Mottakende fartøytype (kode)").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        
        return mfart;
    }

    private MottakerType processMottaker(List<String> row) {
        int lpre = row.size();
        
        MottakerType mottaker = this.factory.createMottakerType();
        mottaker.setMottakernasjonalitetBokmål(row.remove(this.indexMap.get("Mottakernasjonalitet (bokmål)").intValue()));
        mottaker.setMottakernasjonalitetKode(row.remove(this.indexMap.get("Mottakernasjonalitet (kode)").intValue()));
        mottaker.setMottaksstasjon(row.remove(this.indexMap.get("Mottaksstasjon").intValue()));
        
        if (row.size() == lpre){
            return null;
        }
        
        return mottaker;
    }

    private LandingOgProduksjonType processProduksjon(List<String> row) {
        int lpre = row.size();
        
        LandingOgProduksjonType produksjon = this.factory.createLandingOgProduksjonType();
        produksjon.setLandingsdato(row.remove(this.indexMap.get("Landingsdato").intValue()));
        produksjon.setLandingsfylke(row.remove(this.indexMap.get("Landingsfylke").intValue()));
        produksjon.setLandingsfylkeKode(new BigInteger(row.remove(this.indexMap.get("Landingsfylke (kode)").intValue())));
        produksjon.setLandingsklokkeslett(row.remove(this.indexMap.get("Landingsklokkeslett").intValue()));
        produksjon.setLandingskommune(row.remove(this.indexMap.get("Landingskommune").intValue()));
        produksjon.setLandingskommuneKode(new BigInteger(row.remove(this.indexMap.get("Landingskommune (kode)").intValue())));
        produksjon.setLandingsnasjonBokmål(row.remove(this.indexMap.get("Landingsnasjon (bokmål)").intValue()));
        produksjon.setLandingsnasjonKode(row.remove(this.indexMap.get("Landingsnasjon (kode)").intValue()));
        produksjon.setProduksjonsanlegg(row.remove(this.indexMap.get("Produksjonsanlegg").intValue()));
        produksjon.setProduksjonskommune(row.remove(this.indexMap.get("Produksjonskommune").intValue()));
        produksjon.setProduksjonskommuneKode(row.remove(this.indexMap.get("Produksjonskommune (kode)").intValue()));
        
        if (row.size() == lpre){
            return null;
        }
        
        return produksjon;
    }

    private RedskapType processRedskap(List<String> row) {
        int lpre = row.size();
        
        RedskapType redskap = this.factory.createRedskapType();
        redskap.setHovedgruppeRedskapBokmål(row.remove(this.indexMap.get("Hovedgruppe redskap (bokmål)").intValue()));
        redskap.setHovedgruppeRedskapKode(row.remove(this.indexMap.get("Hovedgruppe redskap (kode)").intValue()));
        redskap.setRedskapBokmål(row.remove(this.indexMap.get("Redskap (bokmål)").intValue()));
        redskap.setRedskapKode(row.remove(this.indexMap.get("Redskap (kode)").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        
        return redskap;
    }

    private SalgslagType processSalgslag(List<String> row) {
        int lpre = row.size();
        
        SalgslagType salgslag = this.factory.createSalgslagType();
        salgslag.setSalgslag(row.remove(this.indexMap.get("Salgslag").intValue()));
        salgslag.setSalgslagID(new BigInteger(row.remove(this.indexMap.get("Salgslag ID").intValue())));
        salgslag.setSalgslagKode(row.remove(this.indexMap.get("Salgslag (kode)").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        
        return salgslag;
    }

    private ProduktType processProdukt(List<String> row) {
        int lpre = row.size();
        
        ProduktType produkt = this.factory.createProduktType();
        produkt.setAntallStykk(new BigInteger(row.remove(this.indexMap.get("Antall stykk").intValue())));
        produkt.setAnvendelseBokmål(row.remove(this.indexMap.get("Anvendelse (bokmål)").intValue()));
        produkt.setAnvendelseKode(row.remove(this.indexMap.get("Anvendelse (kode)").intValue()));
        produkt.setArtBokmål(row.remove(this.indexMap.get("Art (bokmål)").intValue()));
        produkt.setArtFAOBokmål(row.remove(this.indexMap.get("Art FAO (bokmål)").intValue()));
        produkt.setArtFAOKode(row.remove(this.indexMap.get("Art FAO (kode)").intValue()));
        produkt.setArtKode(row.remove(this.indexMap.get("Art (kode)").intValue()));
        produkt.setArtsgruppeHistoriskBokmål(row.remove(this.indexMap.get("Artsgruppe historisk (bokmål)").intValue()));
        produkt.setArtsgruppeHistoriskKode(row.remove(this.indexMap.get("Artsgruppe historisk (kode)").intValue()));
        produkt.setBruttovekt(new BigDecimal(row.remove(this.indexMap.get("Bruttovekt").intValue())));
        produkt.setHovedgruppeAnvendelseBokmål(row.remove(this.indexMap.get("Hovedgr anvendelse (bokmål)").intValue()));
        produkt.setHovedgruppeAnvendelseKode(row.remove(this.indexMap.get("Hovedgruppe anvendelse (kode)").intValue()));
        produkt.setHovedgruppeArtBokmål(row.remove(this.indexMap.get("Hovedgruppe art (bokmål)").intValue()));
        produkt.setHovedgruppeArtKode(row.remove(this.indexMap.get("Hovedgruppe art (kode)").intValue()));
        produkt.setKonserveringsmåteBokmål(row.remove(this.indexMap.get("Konserveringsmåte (bokmål)").intValue()));
        produkt.setKonserveringsmåteKode(row.remove(this.indexMap.get("Konserveringsmåte (kode)").intValue()));
        produkt.setKvalitetBokmål(row.remove(this.indexMap.get("Kvalitet (bokmål)").intValue()));
        produkt.setKvalitetKode(row.remove(this.indexMap.get("Kvalitet (kode)").intValue()));
        produkt.setLandingsmåteBokmål(row.remove(this.indexMap.get("Landingsmåte (bokmål)").intValue()));
        produkt.setLandingsmåteKode(row.remove(this.indexMap.get("Landingsmåte (kode)").intValue()));
        produkt.setProdukttilstandBokmål(row.remove(this.indexMap.get("Produkttilstand (bokmål)").intValue()));
        produkt.setProdukttilstandKode(row.remove(this.indexMap.get("Produkttilstand (kode)").intValue()));
        produkt.setProduktvekt(new BigDecimal(row.remove(this.indexMap.get("Produktvekt").intValue())));
        produkt.setRundvekt(new BigDecimal(row.remove(this.indexMap.get("Rundvekt").intValue())));
        produkt.setStørrelsesgrupperingKode(row.remove(this.indexMap.get("Størrelsesgruppering (kode)").intValue()));
        
        if (lpre == row.size()){
            return null;
        }
        return produkt;
    }

    private KvoteType processKvote(List<String> row) {
        int lpre = row.size();
        
        KvoteType kvote = this.factory.createKvoteType();
        kvote.setKvotefartøyRegMerke(row.remove(this.indexMap.get("Kvotefartøy reg.merke").intValue()));
        kvote.setKvotetypeBokmål(row.remove(this.indexMap.get("Kvotetype (bokmål)").intValue()));
        kvote.setKvotetypeKode(row.remove(this.indexMap.get("Kvotetype (kode)").intValue()));
        
        if (row.size() == lpre){
            return null;
        }
        
        return kvote;
    }

}
