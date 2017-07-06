/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.SchemaReader;
import LandingsTypes.v1.DellandingType;
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
import LandingsTypes.v1.SeddellinjeType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;

/**
 * Read LSS files and converts to xml.
 *
 * 2do: Handle encoding for output Handle convertion back to flat table.
 *
 * @author Edvin Fuglebakk edvin.fuglebakk@imr.no
 */
public class LSS2XMLConverter {

    ObjectFactory factory;
    TouchedMap<String, Integer> indexMap; // maps header of column to 0-baed column index.
    private LSSadapters.BigDecimalAdapter decimaladapter;
    private String LSSdelim = "\\|";

    public LSS2XMLConverter() {
        this.factory = new ObjectFactory();
        this.indexMap = new TouchedMap<>();
        this.decimaladapter = new LSSadapters.BigDecimalAdapter();
    }

    public static void main(String[] args) throws LSSProcessingException, IOException, FileNotFoundException, Exception {
        if (args.length < 2) {
            SchemaReader schemareader = new SchemaReader(LSS2XMLConverter.class.getClassLoader().getResourceAsStream("landinger.xsd"));
            System.out.println("Converts LSS file to xml-format: " + schemareader.getTargetNameSpace());
            System.out.println("Memory intensive for large files. Consider increasing heap size, java option: -Xmx<maxheapsize>, e.g: -Xmx8g");
            System.out.println("Usage: <LSS file> <xml file> [encoding]");
            System.exit(0);
        }
        String encoding = System.getProperty("file.encoding");
        if (args.length == 3) {
            encoding = args[2];
        }
        File landings_xml = new File(args[1]);
        if (landings_xml.length() > 0) {
            throw new FileAlreadyExistsException("Landings xml file already exist");
        }
        LSS2XMLConverter.convertFile(new File(args[0]), new File(args[1]), encoding);
    }

    public static void convertFile(File lss, File xml, String encoding) throws JAXBException, FileNotFoundException, LSSProcessingException, IOException, Exception {
        FileInputStream instream = new FileInputStream(lss);
        
        //count lines
        int nlines = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(instream, encoding));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            nlines += 1;
        }
        instream.getChannel().position(0);
        reader = new BufferedReader(new InputStreamReader(instream, encoding));

        LSS2XMLConverter converter = new LSS2XMLConverter();
        converter.processHeader(converter.getRow(reader.readLine()));
        List<SeddellinjeType> linjer = new ArrayList<>(nlines);

        int l = 1;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            linjer.add(converter.processRow(converter.getRow(line)));
            System.out.println("nr: " + l + "/" + nlines);
            l++;
        }
        

        LandingsdataType landingsdata = converter.factory.createLandingsdataType();
        landingsdata.getSeddellinje().addAll(linjer);
        
        if (converter.indexMap.untouched().size() > 0) {
            throw new LSSProcessingException("Some columns not handled: " + converter.indexMap.untouched());
        }

        reader.close();
        instream.close();

        OutputStream stream = new FileOutputStream(xml);
        HierarchicalData.IO.save(stream, landingsdata);
        stream.close();
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

    private List<String> getRow(String line) {
        line += " ";
        String[] a = line.split(this.LSSdelim);
        a[a.length - 1] = a[a.length - 1].trim(); //deal with delimiters at end of line..
        return new ArrayList<>(Arrays.asList(a));
    }

    /**
     * Processes row from LLS
     *
     * @param row
     * @return
     * @throws LSSProcessingException if not all row elements are processed at
     * the end
     */
    protected SeddellinjeType processRow(List<String> row) throws LSSProcessingException, Exception {
        SeddellinjeType linje = this.factory.createSeddellinjeType();
        linje.setLinjenummer(this.parseLong(row.get(this.indexMap.get("Linjenummer"))));
        linje.setDellanding(this.processDellanding(row));
        linje.setFangstdata(this.processFangsData(row));
        linje.setFartøy(this.processFartøy(row));
        linje.setFisker(this.processFisker(row));
        linje.setMottakendefartøy(this.processMottakendeFartøy(row));
        linje.setMottaker(this.processMottaker(row));
        linje.setProduksjon(this.processProduksjon(row));
        linje.setProdukt(this.processProdukt(row));
        linje.setRedskap(this.processRedskap(row));
        linje.setKvote(this.processKvote(row));
        
        linje.setDokumentnummer(parseString(row.get(this.indexMap.get("Dokumentnummer"))));
        linje.setDokumenttypeKode(this.parseBigInteger(row.get(this.indexMap.get("Dokumenttype (kode)"))));
        linje.setDokumenttypeBokmål(parseString(row.get(this.indexMap.get("Dokumenttype (bokmål)"))));
        linje.setDokumentVersjonsnummer(this.parseBigInteger(row.get(this.indexMap.get("Dokument versjonsnummer"))));
        linje.setDokumentFormulardato(parseString(row.get(this.indexMap.get("Dokument formulardato"))));
        linje.setDokumentElektroniskDato(parseString(row.get(this.indexMap.get("Dokument elektronisk dato"))));
        linje.setSalgslag(parseString(row.get(this.indexMap.get("Salgslag"))));
        linje.setSalgslagID(this.parseBigInteger(row.get(this.indexMap.get("Salgslag ID"))));
        linje.setSalgslagKode(parseString(row.get(this.indexMap.get("Salgslag (kode)"))));
        linje.setFangstår(this.parseBigInteger(row.get(this.indexMap.get("Fangstår"))));

        return linje;
    }

    private DellandingType processDellanding(List<String> row) {
        DellandingType dell = this.factory.createDellandingType();
        dell.setDellandingSignal(this.parseBigInteger(row.get(this.indexMap.get("Dellanding (signal)"))));
        dell.setForrigeMottakstasjon(parseString(row.get(this.indexMap.get("Neste mottaksstasjon"))));
        dell.setNesteMottaksstasjon(parseString(row.get(this.indexMap.get("Forrige mottakstasjon"))));

        return dell;
    }

    private FangstdataType processFangsData(List<String> row) {
        FangstdataType fangst = this.factory.createFangstdataType();
        fangst.setFangstdagbokNummer(this.parseLong(row.get(this.indexMap.get("Fangstdagbok (nummer)"))));
        fangst.setFangstdagbokTurnummer(this.parseLong(row.get(this.indexMap.get("Fangstdagbok (turnummer)"))));
        fangst.setFangstfeltKode(parseString(row.get(this.indexMap.get("Fangstfelt (kode)"))));
        fangst.setHovedområdeBokmål(parseString(row.get(this.indexMap.get("Hovedområde (bokmål)"))));
        fangst.setHovedområdeFAOBokmål(parseString(row.get(this.indexMap.get("Hovedområde FAO (bokmål)"))));
        fangst.setHovedområdeFAOKode(parseString(row.get(this.indexMap.get("Hovedområde FAO (kode)"))));
        fangst.setHovedområdeKode(this.parseBigInteger(row.get(this.indexMap.get("Hovedområde (kode)"))));
        fangst.setKystHavKode(this.parseBigInteger(row.get(this.indexMap.get("Kyst/hav (kode)"))));
        fangst.setLokasjonKode(parseString(row.get(this.indexMap.get("Lokasjon (kode)"))));
        fangst.setNordSørFor62GraderNord(parseString(row.get(this.indexMap.get("Nord/sør for 62 grader nord"))));
        fangst.setOmrådegrupperingBokmål(parseString(row.get(this.indexMap.get("Områdegruppering (bokmål)"))));
        fangst.setSisteFangstdato(parseString(row.get(this.indexMap.get("Siste fangstdato"))));
        fangst.setSoneBokmål(parseString(row.get(this.indexMap.get("Sone (bokmål)"))));
        fangst.setSoneKode(parseString(row.get(this.indexMap.get("Sone (kode)"))));

        return fangst;
    }

    private FartøyType processFartøy(List<String> row) throws Exception {
        FartøyType fartøy = this.factory.createFartøyType();
        fartøy.setBruttotonnasje1969(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje 1969"))));
        fartøy.setBruttotonnasjeAnnen(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje annen"))));
        fartøy.setByggeår(this.parseBigInteger(row.get(this.indexMap.get("Byggeår"))));
        fartøy.setFartøyGjelderFraDato(parseString(row.get(this.indexMap.get("Fartøy gjelder fra dato"))));
        fartøy.setFartøyGjelderTilDato(parseString(row.get(this.indexMap.get("Fartøy gjelder til dato"))));
        fartøy.setFartøyID(parseString(row.get(this.indexMap.get("Fartøy ID"))));
        fartøy.setFartøyfylke(parseString(row.get(this.indexMap.get("Fartøyfylke"))));
        fartøy.setFartøyfylkeKode(this.parseBigInteger(row.get(this.indexMap.get("Fartøyfylke (kode)"))));
        fartøy.setFartøykommune(parseString(row.get(this.indexMap.get("Fartøykommune"))));
        fartøy.setFartøykommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Fartøykommune (kode)"))));
        fartøy.setFartøynasjonalitetBokmål(parseString(row.get(this.indexMap.get("Fartøynasjonalitet (bokmål)"))));
        fartøy.setFartøynasjonalitetKode(parseString(row.get(this.indexMap.get("Fartøynasjonalitet (kode)"))));
        fartøy.setFartøynavn(parseString(row.get(this.indexMap.get("Fartøynavn"))));
        fartøy.setFartøytypeBokmål(parseString(row.get(this.indexMap.get("Fartøytype (bokmål)"))));
        fartøy.setFartøytypeKode(parseString(row.get(this.indexMap.get("Fartøytype (kode)"))));
        fartøy.setLengdegruppeKode(parseString(row.get(this.indexMap.get("Lengdegruppe (kode)"))));
        fartøy.setLengdegruppeBokmål(parseString(row.get(this.indexMap.get("Lengdegruppe (bokmål)"))));
        fartøy.setMotorbyggeår(this.parseBigInteger(row.get(this.indexMap.get("Motorbyggeår"))));
        fartøy.setMotorkraft(this.parseBigInteger(row.get(this.indexMap.get("Motorkraft"))));
        fartøy.setOmbyggingsår(this.parseBigInteger(row.get(this.indexMap.get("Ombyggingsår"))));
        fartøy.setRadiokallesignalSeddel(parseString(row.get(this.indexMap.get("Radiokallesignal (seddel)"))));
        fartøy.setRegistreringsmerkeSeddel(parseString(row.get(this.indexMap.get("Registreringsmerke (seddel)"))));
        fartøy.setStørsteLengde(this.parseBigDecimal(row.get(this.indexMap.get("Største lengde"))));

        return fartøy;
    }

    private FiskerType processFisker(List<String> row) {
        FiskerType fisker = this.factory.createFiskerType();
        fisker.setFiskerkommune(parseString(row.get(this.indexMap.get("Fiskerkommune"))));
        fisker.setFiskerkommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Fiskerkommune (kode)"))));
        fisker.setFiskernasjonalitetBokmål(parseString(row.get(this.indexMap.get("Fiskernasjonalitet (bokmål)"))));
        fisker.setFiskernasjonalitetKode(parseString(row.get(this.indexMap.get("Fiskernasjonalitet (kode)"))));

        return fisker;
    }

    private MottakendeFartøyType processMottakendeFartøy(List<String> row) {
        MottakendeFartøyType mfart = this.factory.createMottakendeFartøyType();
        mfart.setMottakendeFartøyRKAL(parseString(row.get(this.indexMap.get("Mottakende fartøy rkal"))));
        mfart.setMottakendeFartøyRegMerke(parseString(row.get(this.indexMap.get("Mottakende fartøy reg.merke"))));
        mfart.setMottakendeFartøynasjBokmål(parseString(row.get(this.indexMap.get("Mottakende fart.nasj (bokmål)"))));
        mfart.setMottakendeFartøynasjKode(parseString(row.get(this.indexMap.get("Mottakende fartøynasj. (kode)"))));
        mfart.setMottakendeFartøytypeBokmål(parseString(row.get(this.indexMap.get("Mottakende fart.type (bokmål)"))));
        mfart.setMottakendeFartøytypeKode(parseString(row.get(this.indexMap.get("Mottakende fartøytype (kode)"))));

        return mfart;
    }

    private MottakerType processMottaker(List<String> row) {
        MottakerType mottaker = this.factory.createMottakerType();
        mottaker.setMottakernasjonalitetBokmål(parseString(row.get(this.indexMap.get("Mottakernasjonalitet (bokmål)"))));
        mottaker.setMottakernasjonalitetKode(parseString(row.get(this.indexMap.get("Mottakernasjonalitet (kode)"))));
        mottaker.setMottaksstasjon(parseString(row.get(this.indexMap.get("Mottaksstasjon"))));

        return mottaker;
    }

    private LandingOgProduksjonType processProduksjon(List<String> row) {
        LandingOgProduksjonType produksjon = this.factory.createLandingOgProduksjonType();
        produksjon.setLandingsdato(parseString(row.get(this.indexMap.get("Landingsdato"))));
        produksjon.setLandingsfylke(parseString(row.get(this.indexMap.get("Landingsfylke"))));
        produksjon.setLandingsfylkeKode(this.parseBigInteger(row.get(this.indexMap.get("Landingsfylke (kode)"))));
        produksjon.setLandingsklokkeslett(parseString(row.get(this.indexMap.get("Landingsklokkeslett"))));
        produksjon.setLandingskommune(parseString(row.get(this.indexMap.get("Landingskommune"))));
        produksjon.setLandingskommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Landingskommune (kode)"))));
        produksjon.setLandingsnasjonBokmål(parseString(row.get(this.indexMap.get("Landingsnasjon (bokmål)"))));
        produksjon.setLandingsnasjonKode(parseString(row.get(this.indexMap.get("Landingsnasjon (kode)"))));
        produksjon.setProduksjonsanlegg(parseString(row.get(this.indexMap.get("Produksjonsanlegg"))));
        produksjon.setProduksjonskommune(parseString(row.get(this.indexMap.get("Produksjonskommune"))));
        produksjon.setProduksjonskommuneKode(parseString(row.get(this.indexMap.get("Produksjonskommune (kode)"))));

        return produksjon;
    }

    private RedskapType processRedskap(List<String> row) {
        RedskapType redskap = this.factory.createRedskapType();
        redskap.setHovedgruppeRedskapBokmål(parseString(row.get(this.indexMap.get("Hovedgruppe redskap (bokmål)"))));
        redskap.setHovedgruppeRedskapKode(parseString(row.get(this.indexMap.get("Hovedgruppe redskap (kode)"))));
        redskap.setRedskapBokmål(parseString(row.get(this.indexMap.get("Redskap (bokmål)"))));
        redskap.setRedskapKode(parseString(row.get(this.indexMap.get("Redskap (kode)"))));

        return redskap;
    }

    private ProduktType processProdukt(List<String> row) throws Exception {
        ProduktType produkt = this.factory.createProduktType();
        produkt.setAntallStykk(this.parseBigInteger(row.get(this.indexMap.get("Antall stykk"))));
        produkt.setAnvendelseBokmål(parseString(row.get(this.indexMap.get("Anvendelse (bokmål)"))));
        produkt.setAnvendelseKode(parseString(row.get(this.indexMap.get("Anvendelse (kode)"))));
        produkt.setArtBokmål(parseString(row.get(this.indexMap.get("Art (bokmål)"))));
        produkt.setArtFAOBokmål(parseString(row.get(this.indexMap.get("Art FAO (bokmål)"))));
        produkt.setArtFAOKode(parseString(row.get(this.indexMap.get("Art FAO (kode)"))));
        produkt.setArtKode(parseString(row.get(this.indexMap.get("Art (kode)"))));
        produkt.setArtsgruppeHistoriskBokmål(parseString(row.get(this.indexMap.get("Artsgruppe historisk (bokmål)"))));
        produkt.setArtsgruppeHistoriskKode(parseString(row.get(this.indexMap.get("Artsgruppe historisk (kode)"))));
        produkt.setBruttovekt(this.parseBigDecimal(row.get(this.indexMap.get("Bruttovekt"))));
        produkt.setHovedgruppeAnvendelseBokmål(parseString(row.get(this.indexMap.get("Hovedgr anvendelse (bokmål)"))));
        produkt.setHovedgruppeAnvendelseKode(parseString(row.get(this.indexMap.get("Hovedgruppe anvendelse (kode)"))));
        produkt.setHovedgruppeArtBokmål(parseString(row.get(this.indexMap.get("Hovedgruppe art (bokmål)"))));
        produkt.setHovedgruppeArtKode(parseString(row.get(this.indexMap.get("Hovedgruppe art (kode)"))));
        produkt.setKonserveringsmåteBokmål(parseString(row.get(this.indexMap.get("Konserveringsmåte (bokmål)"))));
        produkt.setKonserveringsmåteKode(parseString(row.get(this.indexMap.get("Konserveringsmåte (kode)"))));
        produkt.setKvalitetBokmål(parseString(row.get(this.indexMap.get("Kvalitet (bokmål)"))));
        produkt.setKvalitetKode(parseString(row.get(this.indexMap.get("Kvalitet (kode)"))));
        produkt.setLandingsmåteBokmål(parseString(row.get(this.indexMap.get("Landingsmåte (bokmål)"))));
        produkt.setLandingsmåteKode(parseString(row.get(this.indexMap.get("Landingsmåte (kode)"))));
        produkt.setProdukttilstandBokmål(parseString(row.get(this.indexMap.get("Produkttilstand (bokmål)"))));
        produkt.setProdukttilstandKode(parseString(row.get(this.indexMap.get("Produkttilstand (kode)"))));
        produkt.setProduktvekt(this.parseBigDecimal(row.get(this.indexMap.get("Produktvekt"))));
        produkt.setRundvekt(this.parseBigDecimal(row.get(this.indexMap.get("Rundvekt"))));
        produkt.setStørrelsesgrupperingKode(parseString(row.get(this.indexMap.get("Størrelsesgruppering (kode)"))));

        return produkt;
    }

    private KvoteType processKvote(List<String> row) {
        KvoteType kvote = this.factory.createKvoteType();
        kvote.setKvotefartøyRegMerke(parseString(row.get(this.indexMap.get("Kvotefartøy reg.merke"))));
        kvote.setKvotetypeBokmål(parseString(row.get(this.indexMap.get("Kvotetype (bokmål)"))));
        kvote.setKvotetypeKode(this.parseString(row.get(this.indexMap.get("Kvotetype (kode)"))));

        return kvote;
    }
    
    private String parseString(String s){
        if (s.length()==0){
            return null;
        }
        return s;
    }

    private long parseLong(String l) {
        if (l.length() == 0) {
            return 0;
        }
        return new Long(l);
    }

    private BigInteger parseBigInteger(String i) {
        if (i.length() == 0) {
            return null;
        }
        return new BigInteger(i);
    }

    private BigDecimal parseBigDecimal(String d) throws Exception {
        if (d.length() == 0) {
            return null;
        }
        return this.decimaladapter.parse(d);
    }

    //keeps track of which keys has been accessed, so that we can ensure that all columns have been processed.
    private static class TouchedMap<K, V> extends HashMap<K, V> {

        private Map<K, Boolean> touched;

        public TouchedMap() {
            super();
            touched = new HashMap<>();
        }

        @Override
        public V put(K key, V value) {
            if (!touched.containsKey(key)) {
                touched.put(key, Boolean.FALSE);
            }

            return super.put(key, value);
        }

        @Override
        public V get(Object key) {
            touched.put((K) key, Boolean.TRUE);
            return super.get(key);
        }

        /**
         * Returns list of all keys that have never been "getted".
         *
         * @return
         */
        public List<K> untouched() {
            List<K> untouched = new ArrayList<>();
            for (Entry<K, Boolean> entries : this.touched.entrySet()) {
                if (!entries.getValue()) {
                    untouched.add(entries.getKey());
                }
            }
            return untouched;
        }

    }

}
