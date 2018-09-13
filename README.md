# LandingsXML #

Repo for maven project used for developing xsd for landings, and experimenting with tools to read and convert.
Landings are salesnotes and landing notes delivered from FDIR to IMR.

## Format specification ##

src/main/resources:

* landinger_0.1.xsd: Specification of tentative format used in development of stox-reca. This is not compatible with current deliveries from FDIR.
* landinger.xsd: revised format, compatible with LSS (format now delivered from FDIR.)

## Rationale for landinger.xsd ##
* The format spesification was developed by Edvin and Hans. Since the underlying data model is maintained at FDIR and delivered as pipe delimited tabular files (LSS-files), we have attempted to keep all variable names as close to the LSS names as possible. Since these are in Norwegian, type names are also made to be in Norwegian. The LSS-files also comes with a thorough documentation.
* Since the format is verbose and the volume of data large, we have tried to facilitate streamed-filtering. That is filtering that does not require looking forward or backward from the processed tag. This is reflected in the following ways in the data model:
	* We have added relevant filtering parameters as attributes to each landing line (SeddellinjeType).
	* We have attempted to group parameters in order to facilitate in stream selection of which fields to include. If vessel details are not of interest, they can be excluded by dropping elements sluttseddellinje/fartøy while reading data.
	* From the landing lines it is possible to aggregate data on a  sales-note / landing-note, but it is not common practice in the industry to make one sales-note identify one trip, or one delivery of product. One delivery are frequently split on several notes. It is therefore common to treat a landing as all landings from a vessel on one day. The attributes necessary for this aggregation is therefore also added as attributes to the landing lines, to facilitate lookup/filtering on landings.

## Implementation ##
* Contains methods for converting from LSS to xml and vice-versa (src/main/LSS2XMLConverter.java)
* Contains methods for producing the tentative landings model that was used in development of stox-eca (src/main/LSS2XMLConverter.java)
* Contains proof of concept for streamed filtering (src/main/FilterLandings.java)

## Run ##
* Currently LSS to xml conversion is done by running java - jar target/LandingsXML-1.0-SNAPSHOT-jar-with-dependencies.jar. Run without arguments for usage description.
* Example for batch conversion is inclded as an R script in landingssets/populate_sets.R_