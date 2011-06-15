#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay to calculate new estimated concentration values
# for unknown samples using the Rumi function (developed by Youyi at SCHARP). The Rumi function
# takes a dataframe as input and uses the given Standard curve data to calculate est.log.conc an se
# for the unknowns.
#

source("${srcDirectory}/youtil.R");
source("${srcDirectory}/rumi.R");

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

# read in the analyte informatin (to get the mapping from analyte to standard/titration)
analyte.data.file = run.props$val1[run.props$name == "analyteData"];
analyte.data = read.delim(analyte.data.file, header=TRUE, sep="\t");

# get the analyte associated standard/titration information from the analyte data file and put it into the run.data object
run.data$standard = NA;
for (index in 1:nrow(analyte.data))
{
    run.data$standard[as.character(run.data$name) == as.character(analyte.data$Name[index])] = as.character(analyte.data$titrations[index]);
}

# get the unique standards (not including NA or empty string)
standards = setdiff(unique(run.data$standard), c(NA, ""));

# initialize the columns to be calculated
run.data$estLogConc = NA;
run.data$estConc = NA;
run.data$se = NA;

# setup the dataframe needed for the call to rumi
dat = subset(run.data, select=c("dataFile", "standard", "lsid", "well", "description", "name", "expConc", "type", "fi", "fiBackground", "dilution"));

# convert the type to a well_role
dat$well_role[substr(dat$type,0,1) == "S"] = "Standard";
dat$well_role[substr(dat$type,0,1) == "X"] = "Sample";
dat$well_role[substr(dat$type,0,1) == "C"] = "QC";
dat$well_role[substr(dat$type,0,1) == "B"] = "Blank";

# get a booelan vector of the records that are of type standard
standardRecs = !is.na(dat$well_role) & dat$well_role == "Standard";

if(any(standardRecs) & length(standards) > 0){
    # change column name from "name" to "analyte"
    colnames(dat)[colnames(dat) == "name"] = "analyte";

    # change column name from expConc to expected_conc
    colnames(dat)[colnames(dat) == "expConc"] = "expected_conc";

    # get the assayId from the run properties
    dat$assay_id = NA;
    if(any(run.props$name == "assayId"))
          dat$assay_id = run.props$val1[run.props$name == "assayId"];

    # set the sample_id to be description||dilution concatination
    dat$sample_id = paste(dat$description, "||", dat$dilution, sep="");

    # designate which value to use for the FI to be passed to the rumi function
    # TODO: WHAT VALUE SHOULD BE USED FOR STANDARDS? fi OR fiBackground
    colnames(dat)[colnames(dat) == "fi"] = "fiOrig";
    dat$fi[standardRecs] = dat$fiOrig[standardRecs];
    # choose the FI column based on the run property provided by the user, default to the original FI value
    if(any(!standardRecs)){
        fiCol = "FI";
        if(any(run.props$name == "UnkCurveFitInput"))
            fiCol = run.props$val1[run.props$name == "UnkCurveFitInput"];

        if(fiCol == "FI-Bkgd")
            dat$fi[!standardRecs] = dat$fiBackground[!standardRecs]
        else
            dat$fi[!standardRecs] = dat$fiOrig[!standardRecs];
    }

    # subset the dat object to just those records that have an FI
    dat = subset(dat, !is.na(fi));

    # loop through the selected standards in the data.frame and call the rumi function once for each
    # this will also create one pdf for each standard
    for(s in 1:length(standards)){
        stndVal = as.character(standards[s]);

        # subset the data for those analytes set to use the given standard curve
        # note: also need to subset the standard records for only those where description matches the given standard
        standard.dat = subset(dat, standard == stndVal & (well_role != "Standard" | (well_role == "Standard" & description == stndVal)));

        # call the rumi function to calculate new estimated log concentrations for the uknowns
        mypdf(file=stndVal, mfrow=c(2,2))
        fits = rumi(standard.dat, plot.se.profile=TRUE, test.lod=TRUE, verbose=TRUE);
        fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
        dev.off();

        # put the calculated values back into the run.data dataframe by matching on analyte, description, and dilution
        if(nrow(fits) > 0){
            for(index in 1:nrow(fits)){
                a = fits$analyte[index];
                dil = fits$dilution[index];
                desc = fits$description[index];

                elc = fits$"est.log.conc"[index];
                ec = fits$"est.conc"[index];
                se = fits$"se"[index];

                runDataIndex = run.data$name == a & run.data$dilution == dil & run.data$description == desc;
                run.data$estLogConc[runDataIndex] = elc;
                run.data$estConc[runDataIndex] = ec;
                run.data$se[runDataIndex] = se;
            }

            # TODO: WHAT TO DO WITH SE = INF?
            run.data$se[run.data$se == "Inf"] = NA;
        }
    }
}

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

