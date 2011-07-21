#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay to calculate FI - Bkgd - Blank.
# This script subtracts the FI-Bkgd value for the blank bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI -Bkgd - Blank
# values that are <= 0 to 1 (as per the lab's request).
#

# Author: Cory Nathe, LabKey
transformVersion = "1.0";

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

# set the version number run properties for the transform script
runprop.output.file = run.props$val1[run.props$name == "transformedRunPropertiesFile"];
fileConn<-file(runprop.output.file);
writeLines(paste("TransformVersion",transformVersion,sep="\t"), fileConn);
close(fileConn);

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

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);