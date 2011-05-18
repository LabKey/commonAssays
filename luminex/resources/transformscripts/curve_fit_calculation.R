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

source("http://youtil.googlecode.com/files/youtil.R");
source("http://www.labkey.org/download/rumi.R");

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

# initialize the columns to be calculated
run.data$estLogConc = NA;
run.data$estConc = NA;
run.data$se = NA;

# setup the dataframe needed for the call to rumi
dat = subset(run.data, select=c("well", "description", "name", "expConc", "type", "fi", "fiBackground", "fiBackgroundBlank", "dilution"));

# convert the type to a well_role
dat$well_role[substr(dat$type,0,1) == "S"] = "Standard";
dat$well_role[substr(dat$type,0,1) == "X"] = "Sample";
dat$well_role[substr(dat$type,0,1) == "C"] = "QC";
dat$well_role[substr(dat$type,0,1) == "B"] = "Blank";

# get a booelan vector of the records that are of type standard
standards = !is.na(dat$well_role) & dat$well_role == "Standard";

if(any(standards)){
    # change column name from "name" to "analyte"
    colnames(dat)[colnames(dat) == "name"] = "analyte";

    # change column name from expConc to expected_conc
    colnames(dat)[colnames(dat) == "expConc"] = "expected_conc";

    # get the assayId from the run properties
    dat$assay_id = NA;
    if(any(run.props$name == "assayId"))
          dat$assay_id = run.props$val1[run.props$name == "assayId"];

    # TODO: WHAT TO USE FOR SAMPLE_ID? FOR NOW, SET SAMPLE_ID TO WELL|DESCRIPTION
    dat$sample_id=paste(dat$well, dat$description, sep="|");

    # designate which value to use for the FI to be passed to the rumi function
    # TODO: WHAT VALUE SHOULD BE USED FOR STANDARDS? fi OR fiBackground
    colnames(dat)[colnames(dat) == "fi"] = "fiOrig";
    dat$fi[standards] = dat$fiOrig[standards];
    # choose the FI column based on the run property provided by the user, default to the original FI value
    if(any(!standards)){
        fiCol = "FI";
        if(any(run.props$name == "UnkCurveFitInput"))
            fiCol = run.props$val1[run.props$name == "UnkCurveFitInput"];

        if(fiCol == "FI-Bkgd")
            dat$fi[!standards] = dat$fiBackground[!standards]
        else if(fiCol == "FI-Bkgd-Blank")
            dat$fi[!standards] = dat$fiBackgroundBlank[!standards]
        else
            dat$fi[!standards] = dat$fiOrig[!standards];
    }

    # subset the dat object to just those records that have an FI
    dat = subset(dat, !is.na(fi));

    # TODO: CHANGE TO GENERATE ONE PDF PER STANDARD (WITH TITLE = STANDARD DESC)

    # call the rumi function to calculate new estimated log concentrations for the uknowns
    mypdf(file="StndCurvePlots", mfrow=c(2,2))
    fits = rumi(dat, plot.se.profile=TRUE, test.lod=TRUE, verbose=TRUE);
    fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
    dev.off();

    # put the calculated values back into the run.data dataframe by matching well, description, and analyte
    if(nrow(fits) > 0){
        for(index in 1:nrow(fits)){
            w = fits$well[index];
            a = fits$analyte[index];
            d = fits$description[index];
            elc = fits$"est.log.conc"[index];
            ec = fits$"est.conc"[index];
            se = fits$"se"[index];

            runDataIndex = run.data$well == w & run.data$name == a & run.data$description == d;

            run.data$estLogConc[runDataIndex] = elc;
            run.data$estConc[runDataIndex] = ec;
            run.data$se[runDataIndex] = se;
        }

        # TODO: WHAT TO DO WITH SE = INF?
        run.data$se[run.data$se == "Inf"] = NA;
    }
}

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

