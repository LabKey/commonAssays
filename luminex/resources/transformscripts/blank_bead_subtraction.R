#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay to calculate FI - Bkgd - Blank
# This script subtracts the FI-Bkgd value for the blank bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI -Bkgd - Blank
# values that are <= 0 to 1 (as per the lab's request). 

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

# get the unique analyte values
analytes = unique(run.data$name);

# if there is a "Blank" bead, then continue. otherwise, there is no new variable to calculate
if(any(regexpr("^blank", analytes, ignore.case=TRUE) > -1)){
	# initialize the FI - Bkgd - Blank variable
	run.data$fiBackgroundBlank = NA;

	# loop through the unique description values and subtract the mean blank FI-Bkgrd from the fiBackground
	descriptions = unique(run.data$description);
	for(index in 1:length(descriptions)){
		# get the mean blank bead FI-Bkgrd values for each unique desciption
		blank.mean = mean(run.data$fiBackground[regexpr("^blank", run.data$name, ignore.case=TRUE) > -1 & run.data$description == descriptions[index]]);

		# calc the fiBackgroundBlank for all of the non-"Blank" analytes for this description
		nonBlankUnknownsForDescription = substr(run.data$type,0,1) == "X" & regexpr("^blank", run.data$name, ignore.case=TRUE) == -1 & run.data$description == descriptions[index];
		run.data$fiBackgroundBlank[nonBlankUnknownsForDescription] = run.data$fiBackground[nonBlankUnknownsForDescription] - blank.mean;
	}

	# convert fiBackgroundBlank values that are less than or equal to 0 to a value of 1 (as per the lab's calculation)
	run.data$fiBackgroundBlank[!is.na(run.data$fiBackgroundBlank) & run.data$fiBackgroundBlank <= 0] = 1;

	# write the new set of run data out to an output file
	write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);
}