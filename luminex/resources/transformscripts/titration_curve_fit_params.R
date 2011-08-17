#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay to calculate curve fit parameters for each
# titration/analyte combination using both 4PL and 5PL curve fits.
#

# Author: Cory Nathe, LabKey
transformVersion = "2.0";

Sys.time();

source("${srcDirectory}/youtil.R");
# Ruminex package available from http://labs.fhcrc.org/fong/Ruminex/index.html
library(Ruminex);
ruminexVersion = installed.packages()["Ruminex","Version"];

# set up a data frame to store the run properties
run.props = data.frame(NA, NA, NA, NA);
colnames(run.props) = c("name", "val1", "val2", "val3");

#read in the run properties from the TSV
lines = readLines("${runInfo}");

# each line has a run property with the name, val1, val2, etc.
for(i in 1:length(lines)) {
	# split the line into the various parts (tab separated)
	parts = strsplit(lines[i], split="\t")[[1]];

	# if the line does not have 4 parts, add NA's as needed
	if(length(parts) < 4) {
		for(j in 1:4) {
			if(is.na(parts[j])) {
				parts[j] = NA;
			}
		}
	}

	# add the parts for the given run property to the run.props data frame
	run.props[i,] = parts;
}

# save the important run.props as separate variables
run.data.file = run.props$val1[run.props$name == "runDataFile"];
run.output.file = run.props$val3[run.props$name == "runDataFile"];

# read in the run data file content
run.data = read.delim(run.data.file, header=TRUE, sep="\t");

runprop.output.file = run.props$val1[run.props$name == "transformedRunPropertiesFile"];
fileConn<-file(runprop.output.file);
writeLines(c(paste("TransformVersion",transformVersion,sep="\t"),
    paste("RuminexVersion",ruminexVersion,sep="\t")), fileConn);
close(fileConn);

# get the unique analyte values
analytes = unique(run.data$name);

# initialize the curve coefficient variables
run.data$Slope_5pl = NA;
run.data$Lower_5pl = NA;
run.data$Upper_5pl = NA;
run.data$Inflection_5pl = NA;
run.data$Asymmetry_5pl = NA;
run.data$Slope_4pl = NA;
run.data$Lower_4pl = NA;
run.data$Upper_4pl = NA;
run.data$Inflection_4pl = NA;

print(Sys.time());

# read in the titration information
titration.data.file = run.props$val1[run.props$name == "titrationData"];
if (file.exists(titration.data.file))
{
    titration.data = read.delim(titration.data.file, header=TRUE, sep="\t");

    # loop through the possible titrations and to see if it is a standard, qc control, or titrated unknown
    for (tIndex in 1:nrow(titration.data))
    {
        if (titration.data[tIndex,]$Standard == "true" |
            titration.data[tIndex,]$QCControl == "true" |
            titration.data[tIndex,]$Unknown == "true")
        {
            # calculate the 4PL and 5PL curve fit params for each analyte
            for (aIndex in 1:length(analytes))
            {
                titrationName = as.character(titration.data[tIndex,]$Name);
                analyteName = as.character(analytes[aIndex]);
                print(paste(titrationName, analyteName, sep="..."));

                dat = subset(run.data, description == titrationName & name == analyteName);
                runDataIndex = run.data$description == titrationName & run.data$name == analyteName;

                # if no expected concentration for this titration, set expConc = starting conc / dilution
                if (any(is.na(dat$expConc)))
                {
                    dat$expConc = min(dat$dilution) / dat$dilution;
                }

print(Sys.time());

                # get curve fit params for 5PL
                fit = fit.drc(log(fi) ~ expConc, data = dat, force.fit=FALSE, fit.4pl=FALSE);
print(Sys.time());
                run.data[runDataIndex,]$Slope_5pl = as.numeric(coef(fit))[1];
                run.data[runDataIndex,]$Lower_5pl = as.numeric(coef(fit))[2];
                run.data[runDataIndex,]$Upper_5pl = as.numeric(coef(fit))[3];
                run.data[runDataIndex,]$Inflection_5pl = as.numeric(coef(fit))[4];
                run.data[runDataIndex,]$Asymmetry_5pl = as.numeric(coef(fit))[5];

                # get curve fit params for 4PL
                fit = fit.drc(log(fi) ~ expConc, data = dat, force.fit=FALSE, fit.4pl=TRUE);
                run.data[runDataIndex,]$Slope_4pl = as.numeric(coef(fit))[1];
                run.data[runDataIndex,]$Lower_4pl = as.numeric(coef(fit))[2];
                run.data[runDataIndex,]$Upper_4pl = as.numeric(coef(fit))[3];
                run.data[runDataIndex,]$Inflection_4pl = as.numeric(coef(fit))[4];
print(Sys.time());                
            }
        }
    }
}

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);
