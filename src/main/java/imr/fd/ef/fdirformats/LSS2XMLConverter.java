/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imr.fd.ef.fdirformats;

import LandingsTypes.v0_1.LandingdataType;
import LandingsTypes.v1.ArtType;
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
import LandingsTypes.v1.SalgslagdataType;
import LandingsTypes.v1.SeddellinjeType;
import XMLadapters.LocalDateConstantAdapter;
import java.io.BufferedOutputStream;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

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
        //args = new String[3];
        //args[0] = "/Users/a5362/Google Drive/code/masters/formater_fdir/FDIRFormats/src/test/resources/FDIR_HI_LSS_FANGST_2015_100_lines.psv";
        //args[1] = "/Users/a5362/Google Drive/code/masters/formater_fdir/FDIRFormats/src/test/resources/landinger_100_lines.xml";
        //args[2] = "iso-8859-1";

        if (args.length < 2) {
            //SchemaReader schemareader = new SchemaReader(new URL("http://www.imr.no/formats/landing/landingdatav2.xsd").openStream());
            System.out.println("Converts LSS file to xml-format: ");
            System.out.println("Memory intensive for large files. Consider increasing heap size, java option: -Xmx<maxheapsize>, e.g: -Xmx8g");
            System.out.println("Usage: <LSS file> <xml file> <encoding> [--dummy]");
            System.out.println("Usage: Use 'system' for default encoding");
            System.out.println("Usage: --dummy produces old format for testing purposes.");
            System.exit(0);
        }
        String encoding = args[2];

        if (encoding.equals("system")) {
            encoding = System.getProperty("file.encoding");
        }

        boolean dummy = false;
        if (args.length == 4) {
            if (args[3].equals("--dummy")) {
                dummy = true;
            } else {
                System.out.println("Option: " + args[3] + "not recognized.");
                System.exit(0);
            }
        }
        File landings_xml = new File(args[1]);
        if (landings_xml.length() > 0) {
            throw new FileAlreadyExistsException("Landings xml file already exist");
        }

        if (dummy) {
            LSS2XMLConverter.convertFile_01_dummy(new File(args[0]), new File(args[1]), encoding);
        } else {
            LSS2XMLConverter.convertFile(new File(args[0]), new File(args[1]), encoding);
        }

    }

    protected static LandingsdataType parseLSS(File lss, String encoding) throws FileNotFoundException, LSSProcessingException, Exception {
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

        return landingsdata;
    }

    public static void convertFile(File lss, File xml, String encoding) throws JAXBException, FileNotFoundException, LSSProcessingException, IOException, Exception {

        OutputStream stream = new FileOutputStream(xml);
        BufferedOutputStream buf = new BufferedOutputStream(stream);

        XMLStreamWriter xmlOut = XMLOutputFactory.newFactory().createXMLStreamWriter(buf, encoding);
        xmlOut.setDefaultNamespace("l:http://www.imr.no/formats/landinger/v2");
        xmlOut.writeStartDocument(encoding, "1.0");
        xmlOut.writeStartElement("l:http://www.imr.no/formats/landinger/v2", "l:Landingsdata");
        xmlOut.writeNamespace("l", "http://www.imr.no/formats/landinger/v2");
        JAXBContext context = JAXBContext.newInstance(SeddellinjeType.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        
        //parseLSS
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

        int l = 1;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            SeddellinjeType sl = converter.processRow(converter.getRow(line));
            marshaller.marshal(sl, xmlOut);
            if (l % 1000 == 0) {
                System.out.println("nr: " + l + "/" + nlines);
            }
            l++;
        }

        reader.close();
        instream.close();

        xmlOut.writeEndDocument();
        xmlOut.close();
    }

    /**
     * Produces a dummy file of the old format for testing purposes. File is
     * mostly correct, but contains only selected fields, undocumented fields
     * might have arbitrary values, null weights are set to 0, and sales notes
     * might be split if The lss file is not sorted on dokumentid. Weights are
     * deliberately set to hg even if unit is Kg to match old sins.
     *
     * @param lss
     * @param xml
     * @param encoding
     * @throws LSSProcessingException
     * @throws Exception
     */
    public static void convertFile_01_dummy(File lss, File xml, String encoding) throws LSSProcessingException, Exception {
        LandingsdataType landingsdata;
        landingsdata = parseLSS(lss, encoding);

        LandingsTypes.v0_1.LandingdataType landingsdataOld = convertTo_01(landingsdata);

        OutputStream stream = new FileOutputStream(xml);
        HierarchicalData.IO.save(stream, landingsdataOld);
        stream.close();
    }

    protected static LandingdataType convertTo_01(LandingsdataType landingsdata) throws ParseException, DatatypeConfigurationException, Exception {
        LandingsTypes.v0_1.ObjectFactory factory = new LandingsTypes.v0_1.ObjectFactory();
        LandingsTypes.v0_1.LandingdataType landingsdataOld = factory.createLandingdataType();
        landingsdataOld.setFangstAar(BigInteger.valueOf(landingsdata.getSeddellinje().get(0).getFangstår()));
        landingsdataOld.setId("dummyId");
        LandingsTypes.v0_1.SluttseddelType lastseddel = factory.createSluttseddelType();
        lastseddel.setFiskeListe(factory.createFiskListeType());
        landingsdataOld.getSluttseddel().add(lastseddel);
        for (SeddellinjeType seddellinje : landingsdata.getSeddellinje()) {
            BigInteger snr;
            snr = new BigInteger(seddellinje.getDokumentnummer());
            if (lastseddel.getSltsNr() == null) {
                lastseddel.setSltsNr(snr);
            }
            if (!lastseddel.getSltsNr().equals(snr)) {
                lastseddel = factory.createSluttseddelType();
                lastseddel.setFiskeListe(factory.createFiskListeType());
                landingsdataOld.getSluttseddel().add(lastseddel);
            }
            lastseddel.setDokType("" + seddellinje.getDokumenttypeKode());
            lastseddel.setFangstHomr(String.format("%02d", seddellinje.getHovedområdeKode()));
            lastseddel.setFangstKystHav("" + seddellinje.getKystHavKode());
            lastseddel.setFangstLok(seddellinje.getLokasjonKode());
            lastseddel.setFangstSone(seddellinje.getFangstdata().getSoneKode());
            lastseddel.setFartLand(seddellinje.getFartøynasjonalitetKode());
                lastseddel.setSisteFangstDato(convertDate((new XMLadapters.LocalDateConstantAdapter()).marshal(seddellinje.getSisteFangstdato())));
            if (seddellinje.getDokumentFormulardato() != null) {
                lastseddel.setFormularDato(convertDate(seddellinje.getDokumentFormulardato()));
            }
            lastseddel.setLandingsDato(convertDate((new XMLadapters.LocalDateConstantAdapter()).marshal(seddellinje.getProduksjon().getLandingsdato())));
            lastseddel.setFartRegm(seddellinje.getRegistreringsmerkeSeddel());
            lastseddel.setRedskap(seddellinje.getRedskapKode());
            lastseddel.setFartType(seddellinje.getFartøy().getFartøytypeKode());
            lastseddel.setSalgslag(seddellinje.getSalgslagdata().getSalgslagKode());
            lastseddel.setSalgslagOrgnr("" + seddellinje.getSalgslagdata().getSalgslagID());
            lastseddel.setKjopLand(seddellinje.getMottaker().getMottakernasjonalitetKode());
            lastseddel.setFangstRegion(seddellinje.getFangstdata().getHovedområdeFAOKode());
            lastseddel.setLandingsLand(seddellinje.getProduksjon().getLandingsnasjonKode());
            lastseddel.setLandingsMottak(seddellinje.getMottaker().getMottaksstasjon());
            lastseddel.setLandingsKomm(seddellinje.getProduksjon().getLandingskommune());

            LandingsTypes.v0_1.FiskType f = factory.createFiskType();
            f.setFisk(seddellinje.getArtKode());
            LandingsTypes.v0_1.VektType vekt = factory.createVektType();
            vekt.setEnhet("KG");
            if (seddellinje.getProdukt().getRundvekt() != null) {
                vekt.setValue(seddellinje.getProdukt().getRundvekt().doubleValue() * 10);
            } else {
                vekt.setValue(0);
            }
            f.setRundvekt(vekt);
            LandingsTypes.v0_1.VektType prodvekt = factory.createVektType();
            prodvekt.setEnhet("KG");
            if (seddellinje.getProdukt().getProduktvekt() != null) {
                prodvekt.setValue(seddellinje.getProdukt().getProduktvekt().doubleValue() * 10);
            } else {
                prodvekt.setValue(0);
            }
            f.setProdvekt(prodvekt);
            f.setKonservering(seddellinje.getProdukt().getKonserveringsmåteKode());
            f.setTilstand(seddellinje.getProdukt().getProdukttilstandKode());
            f.setKvalitet(seddellinje.getProdukt().getKvalitetKode());
            f.setAnvendelse(seddellinje.getProdukt().getAnvendelseKode());
            f.setId("" + seddellinje.getLinjenummer());
            lastseddel.getFiskeListe().getLinje().add(f);
        }

        return landingsdataOld;
    }

    private static XMLGregorianCalendar convertDate(String sisteFangstdato) throws ParseException, DatatypeConfigurationException {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        Date date = df.parse(sisteFangstdato);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        XMLGregorianCalendar ret = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        return ret;
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
    protected SeddellinjeType processRow(List<String> row) throws LSSProcessingException, Exception{
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
        linje.setArtKode(parseString(row.get(this.indexMap.get("Art (kode)"))));
        linje.setSalgslagdata(this.processSalgslag(row));
        linje.setArt(this.processArt(row));
        linje.setDokumentnummer(parseString(row.get(this.indexMap.get("Dokumentnummer"))));
        linje.setDokumenttypeKode(this.parseBigInteger(row.get(this.indexMap.get("Dokumenttype (kode)"))));
        linje.setDokumenttypeBokmål(parseString(row.get(this.indexMap.get("Dokumenttype (bokmål)"))));
        linje.setDokumentVersjonsnummer(this.parseBigInteger(row.get(this.indexMap.get("Dokument versjonsnummer"))));
        linje.setDokumentFormulardato(parseString(row.get(this.indexMap.get("Dokument formulardato"))));
        linje.setDokumentElektroniskDato(parseString(row.get(this.indexMap.get("Dokument elektronisk dato"))));
        linje.setFangstår(this.parseInteger(row.get(this.indexMap.get("Fangstår"))));
        linje.setHovedområdeKode(row.get(this.indexMap.get("Hovedområde (kode)")));
        linje.setKystHavKode(this.parseInteger(row.get(this.indexMap.get("Kyst/hav (kode)"))));
        linje.setLokasjonKode(parseString(row.get(this.indexMap.get("Lokasjon (kode)"))));
        linje.setSisteFangstdato(parseDate(row.get(this.indexMap.get("Siste fangstdato"))));
        linje.setFartøynasjonalitetKode(parseString(row.get(this.indexMap.get("Fartøynasjonalitet (kode)"))));
        linje.setRegistreringsmerkeSeddel(parseString(row.get(this.indexMap.get("Registreringsmerke (seddel)"))));
        linje.setStørsteLengde(this.parseBigDecimal(row.get(this.indexMap.get("Største lengde"))));
        linje.setHovedgruppeRedskapKode(parseString(row.get(this.indexMap.get("Hovedgruppe redskap (kode)"))));
        linje.setRedskapKode(parseString(row.get(this.indexMap.get("Redskap (kode)"))));
        return linje;
    }

    private DellandingType processDellanding(List<String> row) {
        DellandingType dell = this.factory.createDellandingType();
        dell.setDellandingSignal(this.parseInteger(row.get(this.indexMap.get("Dellanding (signal)"))));
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

        fangst.setNordSørFor62GraderNord(parseString(row.get(this.indexMap.get("Nord/sør for 62 grader nord"))));
        fangst.setOmrådegrupperingBokmål(parseString(row.get(this.indexMap.get("Områdegruppering (bokmål)"))));
        fangst.setSoneBokmål(parseString(row.get(this.indexMap.get("Sone (bokmål)"))));
        fangst.setSoneKode(parseString(row.get(this.indexMap.get("Sone (kode)"))));

        return fangst;
    }

    private FartøyType processFartøy(List<String> row) {
        FartøyType fartøy = this.factory.createFartøyType();
        fartøy.setBruttotonnasje1969(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje 1969"))));
        fartøy.setBruttotonnasjeAnnen(this.parseBigInteger(row.get(this.indexMap.get("Bruttotonnasje annen"))));
        fartøy.setByggeår(this.parseInteger(row.get(this.indexMap.get("Byggeår"))));
        fartøy.setFartøyGjelderFraDato(parseString(row.get(this.indexMap.get("Fartøy gjelder fra dato"))));
        fartøy.setFartøyGjelderTilDato(parseString(row.get(this.indexMap.get("Fartøy gjelder til dato"))));
        fartøy.setFartøyID(parseString(row.get(this.indexMap.get("Fartøy ID"))));
        fartøy.setFartøyfylke(parseString(row.get(this.indexMap.get("Fartøyfylke"))));
        fartøy.setFartøyfylkeKode(this.parseInteger(row.get(this.indexMap.get("Fartøyfylke (kode)"))));
        fartøy.setFartøykommune(parseString(row.get(this.indexMap.get("Fartøykommune"))));
        fartøy.setFartøykommuneKode(this.parseInteger(row.get(this.indexMap.get("Fartøykommune (kode)"))));
        fartøy.setFartøynasjonalitetBokmål(parseString(row.get(this.indexMap.get("Fartøynasjonalitet (bokmål)"))));
        fartøy.setFartøynavn(parseString(row.get(this.indexMap.get("Fartøynavn"))));
        fartøy.setFartøytypeBokmål(parseString(row.get(this.indexMap.get("Fartøytype (bokmål)"))));
        fartøy.setFartøytypeKode(parseString(row.get(this.indexMap.get("Fartøytype (kode)"))));
        fartøy.setLengdegruppeKode(parseString(row.get(this.indexMap.get("Lengdegruppe (kode)"))));
        fartøy.setLengdegruppeBokmål(parseString(row.get(this.indexMap.get("Lengdegruppe (bokmål)"))));
        fartøy.setMotorbyggeår(this.parseInteger(row.get(this.indexMap.get("Motorbyggeår"))));
        fartøy.setMotorkraft(this.parseBigInteger(row.get(this.indexMap.get("Motorkraft"))));
        fartøy.setOmbyggingsår(this.parseInteger(row.get(this.indexMap.get("Ombyggingsår"))));
        fartøy.setRadiokallesignalSeddel(parseString(row.get(this.indexMap.get("Radiokallesignal (seddel)"))));
        return fartøy;
    }

    private FiskerType processFisker(List<String> row) {
        FiskerType fisker = this.factory.createFiskerType();
        fisker.setFiskerkommune(parseString(row.get(this.indexMap.get("Fiskerkommune"))));
        fisker.setFiskerkommuneKode(this.parseInteger(row.get(this.indexMap.get("Fiskerkommune (kode)"))));
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
        produksjon.setLandingsdato(parseDate(row.get(this.indexMap.get("Landingsdato"))));
        produksjon.setLandingsfylke(parseString(row.get(this.indexMap.get("Landingsfylke"))));
        produksjon.setLandingsfylkeKode(this.parseInteger(row.get(this.indexMap.get("Landingsfylke (kode)"))));
        produksjon.setLandingsklokkeslett(parseString(row.get(this.indexMap.get("Landingsklokkeslett"))));
        produksjon.setLandingskommune(parseString(row.get(this.indexMap.get("Landingskommune"))));
        produksjon.setLandingskommuneKode(this.parseInteger(row.get(this.indexMap.get("Landingskommune (kode)"))));
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
        redskap.setRedskapBokmål(parseString(row.get(this.indexMap.get("Redskap (bokmål)"))));

        return redskap;
    }

    private ProduktType processProdukt(List<String> row) throws Exception {
        ProduktType produkt = this.factory.createProduktType();
        produkt.setAntallStykk(this.parseBigInteger(row.get(this.indexMap.get("Antall stykk"))));
        produkt.setAnvendelseBokmål(parseString(row.get(this.indexMap.get("Anvendelse (bokmål)"))));
        produkt.setAnvendelseKode(parseString(row.get(this.indexMap.get("Anvendelse (kode)"))));
        produkt.setBruttovekt(this.parseBigDecimal(row.get(this.indexMap.get("Bruttovekt"))));
        produkt.setHovedgruppeAnvendelseBokmål(parseString(row.get(this.indexMap.get("Hovedgr anvendelse (bokmål)"))));
        produkt.setHovedgruppeAnvendelseKode(parseString(row.get(this.indexMap.get("Hovedgruppe anvendelse (kode)"))));
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

    private String parseString(String s) {
        if (s.length() == 0) {
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

    private Integer parseInteger(String i) {
        if (i.length() == 0) {
            return null;
        }
        return Integer.parseInt(i);
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

    private SalgslagdataType processSalgslag(List<String> row) {
        SalgslagdataType salgslag = this.factory.createSalgslagdataType();
        salgslag.setSalgslag(parseString(row.get(this.indexMap.get("Salgslag"))));
        salgslag.setSalgslagID(this.parseInteger(row.get(this.indexMap.get("Salgslag ID"))));
        salgslag.setSalgslagKode(parseString(row.get(this.indexMap.get("Salgslag (kode)"))));
        return salgslag;
    }

    private ArtType processArt(List<String> row) {
        ArtType art = this.factory.createArtType();
        art.setArtBokmål(parseString(row.get(this.indexMap.get("Art (bokmål)"))));
        art.setArtFAOBokmål(parseString(row.get(this.indexMap.get("Art FAO (bokmål)"))));
        art.setArtFAOKode(parseString(row.get(this.indexMap.get("Art FAO (kode)"))));
        art.setArtsgruppeHistoriskBokmål(parseString(row.get(this.indexMap.get("Artsgruppe historisk (bokmål)"))));
        art.setArtsgruppeHistoriskKode(parseString(row.get(this.indexMap.get("Artsgruppe historisk (kode)"))));
        art.setHovedgruppeArtBokmål(parseString(row.get(this.indexMap.get("Hovedgruppe art (bokmål)"))));
        art.setHovedgruppeArtKode(parseString(row.get(this.indexMap.get("Hovedgruppe art (kode)"))));

        return art;
    }

    private LocalDate parseDate(String get) {
        LocalDateConstantAdapter ld = new LocalDateConstantAdapter();
        return ld.unmarshal(get);
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
