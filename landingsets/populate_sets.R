library(imrParsers)
library(readr)

#/Volumes/prosjekt-1/: ces/prosjekt
target_location <- "~/landingsets"
converter_location <- "/Users/a5362/code/github/LandingsXML/LandingsXML/target/LandingsXML-1.0-SNAPSHOT-jar-with-dependencies.jar"

lss_files=list("2016"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS/mottatt2017/11_2017/FDIR_HI_LSS_FANGST_2016_PR_2017-10-31.psv", 
               "2015"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2015_PR_2016-12-08.psv",
               "2014"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2014_PR_2016-12-08.psv",
               "2013"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2013_PR_2016-12-09.psv",
               "2012"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2012_PR_2016-12-09.psv",
               "2011"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2011_PR_2016-12-11.psv",
               "2010"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2010_PR_2016-12-11.psv",
               "2009"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2009_PR_2016-12-12.psv",
               "2008"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2008_PR_2016-12-12.psv",
               "2007"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2007_PR_2016-12-12.psv",
               "2006"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2006_PR_2016-12-13.psv",
               "2005"="/Volumes/prosjekt-1/ressurs/mare/fiskstat/sluttseddel/sluttseddel_LSS_2005-2015/Fangst/FDIR_HI_LSS_FANGST_2005_PR_2016-12-13.psv"
               )
#species_selection <- list(sei=c("1032"), torsk=c("1022", "102201","102202", "102203", "102204"), sild=c("0611", "061101", "061102", "061103", "061104", "061105", "061106", "061107"))
species_selection <- list()

filter_and_dump <- function(year, species, data, refresh=F){
  if (!is.null(species)){
    fn <- paste(year, "_", species, "_", paste(species_selection[[species]], collapse="_"), ".lss", sep="")  
  }
  else{
    fn <- paste(year, ".lss", sep="")
  }
  
  fp <- file.path(target_location, "LSS", fn)
  if (!is.null(species)){
    d <- data[data$Art..kode. %in% species_selection[[species]],]  
  }
  else{
    d <- data
  }
  
  print(paste("writing", fp))
  write.table(d, file=fp, sep="|", row.names=F, col.names = names(imrParsers::landings_spec$cols), quote = F, fileEncoding = "latin1")

}

get_lss <- function(){
  for (n in names(lss_files)){
    print(paste("fetching:", lss_files[[n]]))
    data <- read.table(lss_files[[n]], sep="|", header = T, colClasses="character", encoding = "latin1")
    for (s in names(species_selection)){
      filter_and_dump(n, s, data)
    }
    filter_and_dump(n, NULL, data)
  }
  
  # filter and dump all years and species
}

convert <- function(filepath){
  targetname <- basename(filepath)
  print(targetname)
  targetname <- gsub(".lss", ".xml", targetname)
  fp_new <- file.path(target_location, "xml", targetname)
  fp_old <- file.path(target_location, "xml_old", targetname)
  #execnew <- paste0("java -Xmx12g -jar ", converter_location, " ", filepath, " ", fp_new, " latin1")
  execold <- paste0("java -Xmx12g -jar ", converter_location, " ", filepath, " ", fp_old, " latin1 --dummy")
  #system(execnew)
  system(execold)
}

convert_all <- function(target=target_location){
  lssfiles <- file.path(target, "LSS")
  for (f in list.files(lssfiles)){
          convert(file.path(lssfiles, f))      
        }
    }

compress <- function(){
  #compress files
}

run <- function(){
  #get_lss()
  convert_all()
}
run()