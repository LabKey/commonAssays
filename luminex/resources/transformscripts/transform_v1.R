#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay.
#
# First, the script subtracts the FI-Bkgd value for the blank bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI -Bkgd - Blank
# values that are <= 0 to 1 (as per the lab's request).
#
# Second, the script calculates new estimated concentration values for unknown samples using the
# Rumi function (developed by Youyi at SCHARP). The Rumi function takes a dataframe as input and
# uses the given Standard curve data to calculate est.log.conc an se for the unknowns.
#

# Author: Cory Nathe, LabKey
transformVersion = "1.0";

source("${srcDirectory}/youtil.R");
# Ruminex package available from http://labs.fhcrc.org/fong/Ruminex/index.html
library(Ruminex);
ruminexVersion = installed.packages()["Ruminex","Version"];

######################## STEP 0: READ IN THE RUN PROPERTIES AND RUN DATA #######################

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

######################## STEP 1: SET THE VERSION NUMBERS ################################

runprop.output.file = run.props$val1[run.props$name == "transformedRunPropertiesFile"];
fileConn<-file(runprop.output.file);
writeLines(c(paste("TransformVersion",transformVersion,sep="\t"),
    paste("RuminexVersion",ruminexVersion,sep="\t")), fileConn);
close(fileConn);

################################# STEP 2: BLANK BEAD SUBTRACTION ################################  

# initialize the FI - Bkgd - Blank variable
run.data$fiBackgroundBlank = NA;

# get the unique analyte values
analytes = unique(run.data$name);

# if there is a "Blank" bead, then continue. otherwise, there is no new variable to calculate
if(any(regexpr("^blank", analytes, ignore.case=TRUE) > -1)){
    # store a boolean vector of blanks, nonBlanks, and unknowns
    blanks = regexpr("^blank", run.data$name, ignore.case=TRUE) > -1;
    nonBlanks = regexpr("^blank", run.data$name, ignore.case=TRUE) == -1;
    unks = toupper(substr(run.data$type,0,1)) == "X";

    # read the run property from user to determine if we are to only blank bead subtract from unks
    unksOnly = TRUE;
    if(any(run.props$name == "SubtBlankFromAll")){
        if(run.props$val1[run.props$name == "SubtBlankFromAll"] == "1")
                unksOnly = FALSE;
    }

	# loop through the unique dataFile/description/excpConc/dilution combos and subtract the mean blank fiBackground from the fiBackground
	combos = unique(data.frame(dataFile=run.data$dataFile, description=run.data$description, dilution=run.data$dilution, expConc=run.data$expConc));
	print(combos);
	for(index in 1:nrow(combos)){
	    dataFile = combos$dataFile[index];
	    description = combos$description[index];
	    dilution = combos$dilution[index];
	    expConc = combos$expConc[index];

        # only standards have expConc, the rest are NA
	    combo = run.data$dataFile == dataFile & run.data$description == description & run.data$dilution == dilution & run.data$expConc == expConc;
	    if(is.na(expConc)){
	        combo = run.data$dataFile == dataFile & run.data$description == description & run.data$dilution == dilution & is.na(run.data$expConc);
	    }

		# get the mean blank bead FI-Bkgrd values for the given description/dilution
		blank.mean = mean(run.data$fiBackground[blanks & combo]);

		# calc the fiBackgroundBlank for all of the non-"Blank" analytes for this combo
        if(unksOnly){
		    run.data$fiBackgroundBlank[unks & nonBlanks & combo] = run.data$fiBackground[unks & nonBlanks & combo] - blank.mean;
		} else{
		    run.data$fiBackgroundBlank[nonBlanks & combo] = run.data$fiBackground[nonBlanks & combo] - blank.mean;
		}
	}

	# convert fiBackgroundBlank values that are less than or equal to 0 to a value of 1 (as per the lab's calculation)
	run.data$fiBackgroundBlank[!is.na(run.data$fiBackgroundBlank) & run.data$fiBackgroundBlank <= 0] = 1;
}

################################## STEP 3: CURVE FIT CALCULATION #################################

# read in the analyte informatin (to get the mapping from analyte to standard/titration)
analyte.data.file = run.props$val1[run.props$name == "analyteData"];
analyte.data = read.delim(analyte.data.file, header=TRUE, sep="\t");

# get the analyte associated standard/titration information from the analyte data file and put it into the run.data object
run.data$Standard = NA;
for (index in 1:nrow(analyte.data))
{
    # hold on to the run data for the given analyte
    run.analyte.data = subset(run.data, as.character(name) == as.character(analyte.data$Name[index]));

    # some analytes may have > 1 standard selected
    stndSet = unlist(strsplit(as.character(analyte.data$titrations[index]), ","));

    # if there are more than 1 standard for this analyte, duplicate run.data records for that analyte at set standard accordingly
    for (stndIndex in 1:length(stndSet))
    {
        if (stndIndex == 1)
        {
            run.data$Standard[as.character(run.data$name) == as.character(analyte.data$Name[index])] = stndSet[stndIndex];
        } else
        {
            temp.data = run.analyte.data;
            temp.data$Standard = stndSet[stndIndex];
            temp.data$lsid = NA; # lsid will be set by the server
            run.data = rbind(run.data, temp.data);
        }
    }
}

# get the unique standards (not including NA or empty string)
standards = setdiff(unique(run.data$Standard), c(NA, ""));

# initialize the columns to be calculated
run.data$EstLogConc_5pl = NA;
run.data$EstConc_5pl = NA;
run.data$SE_5pl = NA;

run.data$EstLogConc_4pl = NA;
run.data$EstConc_4pl = NA;
run.data$SE_4pl = NA;

# setup the dataframe needed for the call to rumi
dat = subset(run.data, select=c("dataFile", "Standard", "lsid", "well", "description", "name", "expConc", "type", "fi", "fiBackground", "fiBackgroundBlank", "dilution"));

# convert the type to a well_role
dat$well_role[toupper(substr(dat$type,0,1)) == "S" | toupper(substr(dat$type,0,2)) == "ES"] = "Standard";
dat$well_role[toupper(substr(dat$type,0,1)) == "X"] = "Sample";
dat$well_role[toupper(substr(dat$type,0,1)) == "C"] = "QC";
dat$well_role[toupper(substr(dat$type,0,1)) == "B"] = "Blank";

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
    colnames(dat)[colnames(dat) == "fi"] = "fiOrig";

    # choose the FI column for standards based on the run property provided by the user, default to the original FI value
    fiCol = "FI";
    if(any(run.props$name == "StndCurveFitInput"))
        fiCol = run.props$val1[run.props$name == "StndCurveFitInput"];
    if(fiCol == "FI-Bkgd")
        dat$fi[standardRecs] = dat$fiBackground[standardRecs]
    else if(fiCol == "FI-Bkgd-Blank")
        dat$fi[standardRecs] = dat$fiBackgroundBlank[standardRecs]
    else
        dat$fi[standardRecs] = dat$fiOrig[standardRecs];        

    # choose the FI column for unknowns based on the run property provided by the user, default to the original FI value
    if(any(!standardRecs)){
        fiCol = "FI";
        if(any(run.props$name == "UnkCurveFitInput"))
            fiCol = run.props$val1[run.props$name == "UnkCurveFitInput"];

        if(fiCol == "FI-Bkgd")
            dat$fi[!standardRecs] = dat$fiBackground[!standardRecs]
        else if(fiCol == "FI-Bkgd-Blank")
            dat$fi[!standardRecs] = dat$fiBackgroundBlank[!standardRecs]
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
        standard.dat = subset(dat, Standard == stndVal & (well_role != "Standard" | (well_role == "Standard" & description == stndVal)));

        # call the rumi function to calculate new estimated log concentrations using 5PL for the uknowns
        mypdf(file=paste(stndVal, "5PL", sep="_"), mfrow=c(2,2))
        fits = rumi(standard.dat, verbose=TRUE);
        fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
        dev.off();

        # put the calculated values back into the run.data dataframe by matching on analyte, description, dilution, and standard
        if(nrow(fits) > 0){
            for(index in 1:nrow(fits)){
                a = fits$analyte[index];
                dil = fits$dilution[index];
                desc = fits$description[index];

                elc = fits$"est.log.conc"[index];
                ec = fits$"est.conc"[index];
                se = fits$"se"[index];

                runDataIndex = run.data$name == a & run.data$dilution == dil & run.data$description == desc & run.data$Standard == stndVal;
                run.data$EstLogConc_5pl[runDataIndex] = elc;
                run.data$EstConc_5pl[runDataIndex] = ec;
                run.data$SE_5pl[runDataIndex] = se;
            }

            # convert Inf and -Inf to Java string representation for DB persistance
            run.data$SE_5pl[run.data$SE_5pl == "Inf"] = "Infinity";
            run.data$SE_5pl[run.data$SE_5pl == "-Inf"] = "-Infinity";
        }

        # call the rumi function to calculate new estimated log concentrations using 4PL for the uknowns
        mypdf(file=paste(stndVal, "4PL", sep="_"), mfrow=c(2,2))
        fits = rumi(standard.dat, fit.4pl=TRUE, verbose=TRUE);
        fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
        dev.off();

        # put the calculated values back into the run.data dataframe by matching on analyte, description, dilution, and standard
        if(nrow(fits) > 0){
            for(index in 1:nrow(fits)){
                a = fits$analyte[index];
                dil = fits$dilution[index];
                desc = fits$description[index];

                elc = fits$"est.log.conc"[index];
                ec = fits$"est.conc"[index];
                se = fits$"se"[index];

                runDataIndex = run.data$name == a & run.data$dilution == dil & run.data$description == desc & run.data$Standard == stndVal;
                run.data$EstLogConc_4pl[runDataIndex] = elc;
                run.data$EstConc_4pl[runDataIndex] = ec;
                run.data$SE_4pl[runDataIndex] = se;
            }

            # convert Inf and -Inf to Java string representation for DB persistance
            run.data$SE_4pl[run.data$SE_4pl == "Inf"] = "Infinity";
            run.data$SE_4pl[run.data$SE_4pl == "-Inf"] = "-Infinity";
        }
    }
}

#####################  STEP 4: WRITE THE RESULTS TO THE OUTPUT FILE LOCATION #####################

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);
