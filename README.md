# LandingsXML #

Repo for maven project used for developing xsd for landings, and experimenting with tools to read and convert.
Landings are salesnotes and landing notes delivered from FDIR to IMR.

## Format specification ##

src/main/resources:

* landinger_0.1.xsd: Specification of tentative format used in development of stox-reca. This is not compatible with current deliveries from FDIR.
* The revised format is no longer maintained in this repo, but available from https://www.imr.no/formats/landinger/v2/landingerv2.xsd

## Rationale for landinger.xsd ##
* The format spesification was developed by Edvin and Hans. Since the underlying data model is maintained at FDIR and delivered as pipe delimited tabular files (LSS-files), we have attempted to keep all variable names as close to the LSS names as possible. Since these are in Norwegian, type names are also made to be in Norwegian. The LSS-files also comes with a thorough documentation (in Norwegian).
* Since the format is verbose and the volume of data large, we have tried to facilitate streamed-filtering. That is filtering that does not require looking forward or backward from the processed tag. This is reflected in the following ways in the data model:
	* We have added relevant filtering parameters as attributes to each landing line (SeddellinjeType).
	* We have attempted to group parameters in order to facilitate in stream selection of which fields to include. If vessel details are not of interest, they can be excluded by dropping elements sluttseddellinje/fartøy while reading data.
	* From the landing lines it is possible to aggregate data on a  sales-note / landing-note, but it is not common practice in the industry to make one sales-note identify one trip, or one delivery of product. One delivery are frequently split on several notes. It is therefore common to treat a landing as all landings from a vessel on one day. The attributes necessary for this aggregation is therefore also added as attributes to the landing lines, to facilitate lookup/filtering on landings.

## Implementation ##
* Contains methods for converting from LSS to xml and vice-versa (src/main/LSS2XMLConverter.java). XML to LSS conversion relies on a project atempting to generically parse xsd's. Changes made to the xsd for landings after spec was delivered to NMD does not work with this tentative xsd parser. So the XML to LSS conversion does not work. See code in StoX for conversion to tabular format.
* Contains methods for producing the tentative landings model that was used in development of stox-eca (src/main/LSS2XMLConverter.java)
* Contains proof of concept for streamed filtering (src/main/FilterLandings.java)

## memory profiling ##
* I have been experimenting with reducing the memory footprint when reading from xml by reconfigurations of the jaxb unmarshaller to reuse repeated values for immutable types. For comparison with the basic jaxb configuration checkout bindings tagged basicjaxbmapping.
* This kind of reconfiguration should be transparent for the user, but reduce memory footprint when repetition is common, and increase it when repetition is not.
* Examples are provided for immutable types String, Integer and LocalDate
* Preliminary results for memory profiles can be found in ./memprofiling/
* profile output filenames refer tags that identify the xjb bindings used (landings.xjb).

## compressed files ##
* Since the format is rather verbose, it is quite compressable. Example for reading compressed files as stream is provided in ZippedReadExample

## Run ##
* Currently LSS to xml conversion is done by running java - jar target/LandingsXML-1.0-SNAPSHOT-jar-with-dependencies.jar. Run without arguments for usage description.
* Example for batch conversion is inclded as an R script in landingssets/populate_sets.R_