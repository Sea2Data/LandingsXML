/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import HierarchicalData.SchemaReader;
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
        linje.setKvote(this.processKvote(row));

        return linje;
    }

    private DokumentType processDokument(List<String> row) throws Exception {

        DokumentType dokument = this.factory.createDokumentType();
        dokument.setDokumentnummer(row.get(this.indexMap.get("Dokumentnummer")));
        dokument.setDokumenttypeKode(this.parseBigInteger(row.get(this.indexMap.get("Dokumenttype (kode)"))));
        dokument.setDokumenttypeBokmål(row.get(this.indexMap.get("Dokumenttype (bokmål)")));
        dokument.setDokumentVersjonsnummer(this.parseBigInteger(row.get(this.indexMap.get("Dokument versjonsnummer"))));
        dokument.setDokumentFormulardato(row.get(this.indexMap.get("Dokument formulardato")));
        dokument.setDokumentElektroniskDato(row.get(this.indexMap.get("Dokument elektronisk dato")));
        dokument.setSalgslag(row.get(this.indexMap.get("Salgslag")));
        dokument.setSalgslagID(this.parseBigInteger(row.get(this.indexMap.get("Salgslag ID"))));
        dokument.setSalgslagKode(row.get(this.indexMap.get("Salgslag (kode)")));

        return dokument;
    }

    private DellandingType processDellanding(List<String> row) {
        DellandingType dell = this.factory.createDellandingType();
        dell.setDellandingSignal(this.parseBigInteger(row.get(this.indexMap.get("Dellanding (signal)"))));
        dell.setForrigeMottakstasjon(row.get(this.indexMap.get("Neste mottaksstasjon")));
        dell.setNesteMottaksstasjon(row.get(this.indexMap.get("Forrige mottakstasjon")));

        return dell;
    }

    private FangstdataType processFangsData(List<String> row) {
        FangstdataType fangst = this.factory.createFangstdataType();
        fangst.setFangstdagbokNummer(this.parseLong(row.get(this.indexMap.get("Fangstdagbok (nummer)"))));
        fangst.setFangstdagbokTurnummer(this.parseLong(row.get(this.indexMap.get("Fangstdagbok (turnummer)"))));
        fangst.setFangstfeltKode(row.get(this.indexMap.get("Fangstfelt (kode)")));
        fangst.setFangstår(this.parseBigInteger(row.get(this.indexMap.get("Fangstår"))));
        fangst.setHovedområdeBokmål(row.get(this.indexMap.get("Hovedområde (bokmål)")));
        fangst.setHovedområdeFAOBokmål(row.get(this.indexMap.get("Hovedområde FAO (bokmål)")));
        fangst.setHovedområdeFAOKode(row.get(this.indexMap.get("Hovedområde FAO (kode)")));
        fangst.setHovedområdeKode(this.parseBigInteger(row.get(this.indexMap.get("Hovedområde (kode)"))));
        fangst.setKystHavKode(this.parseBigInteger(row.get(this.indexMap.get("Kyst/hav (kode)"))));
        fangst.setLokasjonKode(row.get(this.indexMap.get("Lokasjon (kode)")));
        fangst.setNordSørFor62GraderNord(row.get(this.indexMap.get("Nord/sør for 62 grader nord")));
        fangst.setOmrådegrupperingBokmål(row.get(this.indexMap.get("Områdegruppering (bokmål)")));
        fangst.setSisteFangstdato(row.get(this.indexMap.get("Siste fangstdato")));
        fangst.setSoneBokmål(row.get(this.indexMap.get("Sone (bokmål)")));
        fangst.setSoneKode(row.get(this.indexMap.get("Sone (kode)")));

        return fangst;
    }

    private FartøyType processFartøy(List<String> row) throws Exception {
        FartøyType fartøy = this.factory.createFartøyType();
        fartøy.setBruttotonnasje1969(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje 1969"))));
        fartøy.setBruttotonnasjeAnnen(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje annen"))));
        fartøy.setByggeår(this.parseBigInteger(row.get(this.indexMap.get("Byggeår"))));
        fartøy.setFartøyGjelderFraDato(row.get(this.indexMap.get("Fartøy gjelder fra dato")));
        fartøy.setFartøyGjelderTilDato(row.get(this.indexMap.get("Fartøy gjelder til dato")));
        fartøy.setFartøyID(row.get(this.indexMap.get("Fartøy ID")));
        fartøy.setFartøyfylke(row.get(this.indexMap.get("Fartøyfylke")));
        fartøy.setFartøyfylkeKode(this.parseBigInteger(row.get(this.indexMap.get("Fartøyfylke (kode)"))));
        fartøy.setFartøykommune(row.get(this.indexMap.get("Fartøykommune")));
        fartøy.setFartøykommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Fartøykommune (kode)"))));
        fartøy.setFartøynasjonalitetBokmål(row.get(this.indexMap.get("Fartøynasjonalitet (bokmål)")));
        fartøy.setFartøynasjonalitetKode(row.get(this.indexMap.get("Fartøynasjonalitet (kode)")));
        fartøy.setFartøynavn(row.get(this.indexMap.get("Fartøynavn")));
        fartøy.setFartøytypeBokmål(row.get(this.indexMap.get("Fartøytype (bokmål)")));
        fartøy.setFartøytypeKode(row.get(this.indexMap.get("Fartøytype (kode)")));
        fartøy.setLengdegruppeKode(row.get(this.indexMap.get("Lengdegruppe (kode)")));
        fartøy.setLengdegruppeBokmål(row.get(this.indexMap.get("Lengdegruppe (bokmål)")));
        fartøy.setMotorbyggeår(this.parseBigInteger(row.get(this.indexMap.get("Motorbyggeår"))));
        fartøy.setMotorkraft(this.parseBigInteger(row.get(this.indexMap.get("Motorkraft"))));
        fartøy.setOmbyggingsår(this.parseBigInteger(row.get(this.indexMap.get("Ombyggingsår"))));
        fartøy.setRadiokallesignalSeddel(row.get(this.indexMap.get("Radiokallesignal (seddel)")));
        fartøy.setRegistreringsmerkeSeddel(row.get(this.indexMap.get("Registreringsmerke (seddel)")));
        fartøy.setStørsteLengde(this.parseBigDecimal(row.get(this.indexMap.get("Største lengde"))));

        return fartøy;
    }

    private FiskerType processFisker(List<String> row) {
        FiskerType fisker = this.factory.createFiskerType();
        fisker.setFiskerkommune(row.get(this.indexMap.get("Fiskerkommune")));
        fisker.setFiskerkommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Fiskerkommune (kode)"))));
        fisker.setFiskernasjonalitetBokmål(row.get(this.indexMap.get("Fiskernasjonalitet (bokmål)")));
        fisker.setFiskernasjonalitetKode(row.get(this.indexMap.get("Fiskernasjonalitet (kode)")));

        return fisker;
    }

    private MottakendeFartøyType processMottakendeFartøy(List<String> row) {
        MottakendeFartøyType mfart = this.factory.createMottakendeFartøyType();
        mfart.setMottakendeFartøyRKAL(row.get(this.indexMap.get("Mottakende fartøy rkal")));
        mfart.setMottakendeFartøyRegMerke(row.get(this.indexMap.get("Mottakende fartøy reg.merke")));
        mfart.setMottakendeFartøynasjBokmål(row.get(this.indexMap.get("Mottakende fart.nasj (bokmål)")));
        mfart.setMottakendeFartøynasjKode(row.get(this.indexMap.get("Mottakende fartøynasj. (kode)")));
        mfart.setMottakendeFartøytypeBokmål(row.get(this.indexMap.get("Mottakende fart.type (bokmål)")));
        mfart.setMottakendeFartøytypeKode(row.get(this.indexMap.get("Mottakende fartøytype (kode)")));

        return mfart;
    }

    private MottakerType processMottaker(List<String> row) {
        MottakerType mottaker = this.factory.createMottakerType();
        mottaker.setMottakernasjonalitetBokmål(row.get(this.indexMap.get("Mottakernasjonalitet (bokmål)")));
        mottaker.setMottakernasjonalitetKode(row.get(this.indexMap.get("Mottakernasjonalitet (kode)")));
        mottaker.setMottaksstasjon(row.get(this.indexMap.get("Mottaksstasjon")));

        return mottaker;
    }

    private LandingOgProduksjonType processProduksjon(List<String> row) {
        LandingOgProduksjonType produksjon = this.factory.createLandingOgProduksjonType();
        produksjon.setLandingsdato(row.get(this.indexMap.get("Landingsdato")));
        produksjon.setLandingsfylke(row.get(this.indexMap.get("Landingsfylke")));
        produksjon.setLandingsfylkeKode(this.parseBigInteger(row.get(this.indexMap.get("Landingsfylke (kode)"))));
        produksjon.setLandingsklokkeslett(row.get(this.indexMap.get("Landingsklokkeslett")));
        produksjon.setLandingskommune(row.get(this.indexMap.get("Landingskommune")));
        produksjon.setLandingskommuneKode(this.parseBigInteger(row.get(this.indexMap.get("Landingskommune (kode)"))));
        produksjon.setLandingsnasjonBokmål(row.get(this.indexMap.get("Landingsnasjon (bokmål)")));
        produksjon.setLandingsnasjonKode(row.get(this.indexMap.get("Landingsnasjon (kode)")));
        produksjon.setProduksjonsanlegg(row.get(this.indexMap.get("Produksjonsanlegg")));
        produksjon.setProduksjonskommune(row.get(this.indexMap.get("Produksjonskommune")));
        produksjon.setProduksjonskommuneKode(row.get(this.indexMap.get("Produksjonskommune (kode)")));

        return produksjon;
    }

    private RedskapType processRedskap(List<String> row) {
        RedskapType redskap = this.factory.createRedskapType();
        redskap.setHovedgruppeRedskapBokmål(row.get(this.indexMap.get("Hovedgruppe redskap (bokmål)")));
        redskap.setHovedgruppeRedskapKode(row.get(this.indexMap.get("Hovedgruppe redskap (kode)")));
        redskap.setRedskapBokmål(row.get(this.indexMap.get("Redskap (bokmål)")));
        redskap.setRedskapKode(row.get(this.indexMap.get("Redskap (kode)")));

        return redskap;
    }

    private ProduktType processProdukt(List<String> row) throws Exception {
        ProduktType produkt = this.factory.createProduktType();
        produkt.setAntallStykk(this.parseBigInteger(row.get(this.indexMap.get("Antall stykk"))));
        produkt.setAnvendelseBokmål(row.get(this.indexMap.get("Anvendelse (bokmål)")));
        produkt.setAnvendelseKode(row.get(this.indexMap.get("Anvendelse (kode)")));
        produkt.setArtBokmål(row.get(this.indexMap.get("Art (bokmål)")));
        produkt.setArtFAOBokmål(row.get(this.indexMap.get("Art FAO (bokmål)")));
        produkt.setArtFAOKode(row.get(this.indexMap.get("Art FAO (kode)")));
        produkt.setArtKode(row.get(this.indexMap.get("Art (kode)")));
        produkt.setArtsgruppeHistoriskBokmål(row.get(this.indexMap.get("Artsgruppe historisk (bokmål)")));
        produkt.setArtsgruppeHistoriskKode(row.get(this.indexMap.get("Artsgruppe historisk (kode)")));
        produkt.setBruttovekt(this.parseBigDecimal(row.get(this.indexMap.get("Bruttovekt"))));
        produkt.setHovedgruppeAnvendelseBokmål(row.get(this.indexMap.get("Hovedgr anvendelse (bokmål)")));
        produkt.setHovedgruppeAnvendelseKode(row.get(this.indexMap.get("Hovedgruppe anvendelse (kode)")));
        produkt.setHovedgruppeArtBokmål(row.get(this.indexMap.get("Hovedgruppe art (bokmål)")));
        produkt.setHovedgruppeArtKode(row.get(this.indexMap.get("Hovedgruppe art (kode)")));
        produkt.setKonserveringsmåteBokmål(row.get(this.indexMap.get("Konserveringsmåte (bokmål)")));
        produkt.setKonserveringsmåteKode(row.get(this.indexMap.get("Konserveringsmåte (kode)")));
        produkt.setKvalitetBokmål(row.get(this.indexMap.get("Kvalitet (bokmål)")));
        produkt.setKvalitetKode(row.get(this.indexMap.get("Kvalitet (kode)")));
        produkt.setLandingsmåteBokmål(row.get(this.indexMap.get("Landingsmåte (bokmål)")));
        produkt.setLandingsmåteKode(row.get(this.indexMap.get("Landingsmåte (kode)")));
        produkt.setProdukttilstandBokmål(row.get(this.indexMap.get("Produkttilstand (bokmål)")));
        produkt.setProdukttilstandKode(row.get(this.indexMap.get("Produkttilstand (kode)")));
        produkt.setProduktvekt(this.parseBigDecimal(row.get(this.indexMap.get("Produktvekt"))));
        produkt.setRundvekt(this.parseBigDecimal(row.get(this.indexMap.get("Rundvekt"))));
        produkt.setStørrelsesgrupperingKode(row.get(this.indexMap.get("Størrelsesgruppering (kode)")));

        return produkt;
    }

    private KvoteType processKvote(List<String> row) {
        KvoteType kvote = this.factory.createKvoteType();
        kvote.setKvotefartøyRegMerke(row.get(this.indexMap.get("Kvotefartøy reg.merke")));
        kvote.setKvotetypeBokmål(row.get(this.indexMap.get("Kvotetype (bokmål)")));
        kvote.setKvotetypeKode(row.get(this.indexMap.get("Kvotetype (kode)")));

        return kvote;
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
