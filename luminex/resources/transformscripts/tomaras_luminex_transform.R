#
# Copyright (c) 2011-2012 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay.
#
# First, the script subtracts the FI-Bkgd value for the blank bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI -Bkgd and FI -Bkgd - Blank
# values that are <= 0 to 1 (as per the lab's request).
#
# Next, the script calculates curve fit parameters for each titration/analyte combination using both
# 4PL and 5PL curve fits (from the fit.drc function in the Ruminex package (developed by Youyi at SCHARP).
#
# Then, the script calculates new estimated concentration values for unknown samples using the
# rumi function. The rumi function takes a dataframe as input and uses the given Standard curve data to
# calculate est.log.conc an se for the unknowns.
#
# Finally, the script calculates positivity of samples by comparing 3x or 5x fold change of the baseline visit
# FI-Bkgd and FI-Bkgd-Blank values with other visits.
#
# CHANGES :
#  - 2.1.20111216 : Issue 13696: Luminex transform script should use excel file titration "Type" for EC50 and Conc calculations
#  - 3.0.20120229 : Changes for LabKey server 12.1
#
# Author: Cory Nathe, LabKey
transformVersion = "3.0.20120229";

# print the starting time for the transform script
writeLines(paste("Processing start time:",Sys.time(),"\n",sep=" "));

source("${srcDirectory}/youtil.R");
# Ruminex package available from http://labs.fhcrc.org/fong/Ruminex/index.html
library(Ruminex);
ruminexVersion = installed.packages()["Ruminex","Version"];

########################################## FUNCTIONS ##########################################

getCurveFitInputCol <- function(runProps, fiRunCol, defaultFiCol)
{
    runCol = runProps$val1[runProps$name == fiRunCol];
    if (runCol == "FI") {
        runCol = "fi"
    } else if (runCol == "FI-Bkgd") {
    	runCol = "fiBackground"
    } else if (runCol == "FI-Bkgd-Blank") {
    	runCol = "fiBackgroundBlank"
    } else {
        runCol = defaultFiCol
    }
    runCol;
}

getFiDisplayName <- function(fiCol)
{
    displayVal = fiCol;
    if (fiCol == "fi") {
        displayVal = "FI"
    } else if (fiCol == "fiBackground") {
        displayVal = "FI-Bkgd"
    } else if (fiCol == "fiBackgroundBlank") {
        displayVal = "FI-Bkgd-Blank"
    }
    displayVal;
}

fiConversion <- function(val)
{
    1 + max(val,0);
}

getRunPropertyValue <- function(runProps, colName)
{
    value = NA;
    if (any(run.props$name == colName))
    {
        value = run.props$val1[run.props$name == colName];

        # reutrn NA for an empty string
        if (nchar(value) == 0)
        {
            value = NA;
        }
    }
    value;
}

compareNumbersForEquality <- function(val1, val2, epsilon)
{
	val1 = as.numeric(val1);
	val2 = as.numeric(val2);
	equal = val1 > (val2 - epsilon) & val1 < val2 + epsilon;
	equal
}

######################## STEP 0: READ IN THE RUN PROPERTIES AND RUN DATA #######################

# set up a data frame to store the run properties
run.props = data.frame(NA, NA, NA, NA);
colnames(run.props) = c("name", "val1", "val2", "val3");

#read in the run properties from the TSV
lines = readLines("${runInfo}");

# each line has a run property with the name, val1, val2, etc.
for (i in 1:length(lines))
{
	# split the line into the various parts (tab separated)
	parts = strsplit(lines[i], split="\t")[[1]];

	# if the line does not have 4 parts, add NA's as needed
	if (length(parts) < 4)
	{
		for (j in 1:4)
		{
			if (is.na(parts[j]))
			{
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

# determine if the data contains both raw and summary data
# if both exists, only the raw data will be used for the calculations
bothRawAndSummary = any(run.data$summary == "true") & any(run.data$summary == "false");

######################## STEP 1: SET THE VERSION NUMBERS ################################

runprop.output.file = run.props$val1[run.props$name == "transformedRunPropertiesFile"];
fileConn<-file(runprop.output.file);
writeLines(c(paste("TransformVersion",transformVersion,sep="\t"),
    paste("RuminexVersion",ruminexVersion,sep="\t")), fileConn);
close(fileConn);

################################# STEP 2: BLANK BEAD SUBTRACTION ################################

# read in the titration information
run.data$isStandard = NA;
run.data$isQCControl = NA;
run.data$isUnknown = NA;
titration.data.file = run.props$val1[run.props$name == "titrationData"];
titration.data = data.frame();
if (file.exists(titration.data.file))
{
    titration.data = read.delim(titration.data.file, header=TRUE, sep="\t");

    # apply the titration data to the run.data object
    run.data$isStandard[run.data$titration == "false"] = FALSE;
    run.data$isQCControl[run.data$titration == "false"] = FALSE;
    run.data$isUnknown[run.data$titration == "false"] = TRUE;
    for (tIndex in 1:nrow(titration.data))
    {
        titrationName = as.character(titration.data[tIndex,]$Name);
        run.data$isStandard[run.data$titration == "true" & run.data$description == titrationName] = (titration.data[tIndex,]$Standard == "true");
        run.data$isQCControl[run.data$titration == "true" & run.data$description == titrationName] = (titration.data[tIndex,]$QCControl == "true");
        run.data$isUnknown[run.data$titration == "true" & run.data$description == titrationName] = (titration.data[tIndex,]$Unknown == "true");
    }
}

# initialize the FI - Bkgd - Blank variable
run.data$fiBackgroundBlank = NA;

# get the unique analyte values
analytes = unique(run.data$name);

# if there is a "Blank" bead, then continue. otherwise, there is no new variable to calculate
if (any(regexpr("^blank", analytes, ignore.case=TRUE) > -1))
{
    # store a boolean vector of blanks, nonBlanks, and unknowns (i.e. non-standards)
    blanks = regexpr("^blank", run.data$name, ignore.case=TRUE) > -1;
    nonBlanks = regexpr("^blank", run.data$name, ignore.case=TRUE) == -1;
    unks = (is.na(run.data$isStandard) & is.na(run.data$isQCControl)) | (!run.data$isStandard & !run.data$isQCControl);

    # read the run property from user to determine if we are to only blank bead subtract from unks
    unksOnly = TRUE;
    if (any(run.props$name == "SubtBlankFromAll"))
    {
        if (run.props$val1[run.props$name == "SubtBlankFromAll"] == "1")
        {
            unksOnly = FALSE;
        }
    }

	# loop through the unique dataFile/description/excpConc/dilution combos and subtract the mean blank fiBackground from the fiBackground
	blank.data = run.data[blanks,];
	combos = unique(subset(blank.data, select=c("dataFile", "description", "dilution", "expConc")));

	for (index in 1:nrow(combos))
	{
	    dataFile = combos$dataFile[index];
	    description = combos$description[index];
	    dilution = combos$dilution[index];
	    expConc = combos$expConc[index];

        # only standards have expConc, the rest are NA
	    combo = run.data$dataFile == dataFile & run.data$description == description & run.data$dilution == dilution & run.data$expConc == expConc;
	    if (is.na(expConc))
	    {
	        combo = run.data$dataFile == dataFile & run.data$description == description & run.data$dilution == dilution & is.na(run.data$expConc);
	    }

		# get the mean blank bead FI-Bkgrd values for the given description/dilution
		blank.mean = mean(run.data$fiBackground[blanks & combo]);

		# calc the fiBackgroundBlank for all of the non-"Blank" analytes for this combo
        if (unksOnly) {
		    run.data$fiBackgroundBlank[unks & nonBlanks & combo] = run.data$fiBackground[unks & nonBlanks & combo] - blank.mean;
		} else{
		    run.data$fiBackgroundBlank[nonBlanks & combo] = run.data$fiBackground[nonBlanks & combo] - blank.mean;
		}
	}
}

################################## STEP 3: TITRATION CURVE FIT #################################

# initialize the curve coefficient variables
run.data$Slope_4pl = NA;
run.data$Lower_4pl = NA;
run.data$Upper_4pl = NA;
run.data$Inflection_4pl = NA;
run.data$EC50_4pl = NA;
run.data$Slope_5pl = NA;
run.data$Lower_5pl = NA;
run.data$Upper_5pl = NA;
run.data$Inflection_5pl = NA;
run.data$Asymmetry_5pl = NA;
run.data$EC50_5pl = NA;

# loop through the possible titrations and to see if it is a standard, qc control, or titrated unknown
if (nrow(titration.data) > 0)
{
  for (tIndex in 1:nrow(titration.data))
  {
    if (titration.data[tIndex,]$Standard == "true" |
        titration.data[tIndex,]$QCControl == "true" |
        titration.data[tIndex,]$Unknown == "true")
    {
       titrationName = as.character(titration.data[tIndex,]$Name);

       # 2 types of curve fits for the EC50 calculations, with separate PDFs for the QC Curves
       fitTypes = c("4pl", "5pl");
       for (typeIndex in 1:length(fitTypes))
       {
          # we want to create PDF plots of the curves for QC Controls
          if (titration.data[tIndex,]$QCControl == "true") {
              mypdf(file=paste(titrationName, "QC_Curves", toupper(fitTypes[typeIndex]), sep="_"), mfrow=c(1,1));
          }

          # calculate the curve fit params for each analyte
          for (aIndex in 1:length(analytes))
          {
            analyteName = as.character(analytes[aIndex]);
            print(paste("Calculating the", fitTypes[typeIndex], "curve fit params for ",titrationName, analyteName, sep=" "));
            dat = subset(run.data, description == titrationName & name == analyteName);

            yLabel = "";
            if (titration.data[tIndex,]$Standard == "true" | titration.data[tIndex,]$QCControl == "true") {
                # choose the FI column for standards and qc controls based on the run property provided by the user, default to the FI-Bkgd value
                if (any(run.props$name == "StndCurveFitInput"))
                {
                    fiCol = getCurveFitInputCol(run.props, "StndCurveFitInput", "fiBackground")
                    yLabel = getFiDisplayName(fiCol);
                    dat$fi = dat[, fiCol]
                }
            } else {
                # choose the FI column for unknowns based on the run property provided by the user, default to the FI-Bkgd value
                if (any(run.props$name == "UnkCurveFitInput"))
                {
                    fiCol = getCurveFitInputCol(run.props, "UnkCurveFitInput", "fiBackground")
                    yLabel = getFiDisplayName(fiCol);
                    dat$fi = dat[, fiCol]
                }
            }

            # subset the dat object to just those records that have an FI
            dat = subset(dat, !is.na(fi));

            # if both raw and summary data are available, just use the raw data for the calc
            if (bothRawAndSummary) {
                dat = subset(dat, summary == "false");
            }

            # remove any excluded replicate groups for this titration/analyte
            dat = subset(dat, tolower(FlaggedAsExcluded) == "false");

            # for standards, use the expected conc values for the curve fit
            # for non-standard titrations, use the dilution values for the curve fit
            # Issue 13696
            if (nrow(dat) > 0 && (toupper(substr(dat$type[1],0,1)) == "S" || toupper(substr(dat$type[1],0,2)) == "ES")) {
                dat$dose = dat$expConc;
                xLabel = "Expected Conc";
            } else {
                dat$dose = dat$dilution;
                xLabel = "Dilution";
            }

            # subset the dat object to just those records that have a dose (dilution or expConc) issue 13173
            dat = subset(dat, !is.na(dose));

            if (nrow(dat) > 0)
            {
                runDataIndex = run.data$description == titrationName & run.data$name == analyteName;

                # use the decided upon conversion function for handling of negative values
                dat$fi = sapply(dat$fi, fiConversion);

                if (fitTypes[typeIndex] == "4pl")
                {
                    tryCatch({
                            fit = drm(fi~dose, data=dat, fct=LL.4());
                            run.data[runDataIndex,]$Slope_4pl = as.numeric(coef(fit))[1]
                            run.data[runDataIndex,]$Lower_4pl = as.numeric(coef(fit))[2]
                            run.data[runDataIndex,]$Upper_4pl = as.numeric(coef(fit))[3]
                            run.data[runDataIndex,]$Inflection_4pl = as.numeric(coef(fit))[4]
                            run.data[runDataIndex,]$EC50_4pl = as.numeric(coef(fit))[4]

                            # plot the curve fit for the QC Controls
                            if (titration.data[tIndex,]$QCControl == "true") {
                                plot(fit, type="all", main=analyteName, cex=.5, ylab=yLabel, xlab=xLabel);
                            }
                        },
                        error = function(e) {
                            print(e);

                            # plot the individual data points for the QC Controls
                            if (titration.data[tIndex,]$QCControl == "true") {
                                plot(fi ~ dose, data = dat, log="x", cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel);
                            }
                        }
                    );
                } else if (fitTypes[typeIndex] == "5pl")
                {
                    tryCatch({
                            fit = fit.drc(log(fi)~dose, data=dat, force.fit=TRUE, fit.4pl=FALSE);
                            run.data[runDataIndex,]$Slope_5pl = as.numeric(coef(fit))[1];
                            run.data[runDataIndex,]$Lower_5pl = as.numeric(coef(fit))[2];
                            run.data[runDataIndex,]$Upper_5pl = as.numeric(coef(fit))[3];
                            run.data[runDataIndex,]$Inflection_5pl = as.numeric(coef(fit))[4];
                            run.data[runDataIndex,]$Asymmetry_5pl = as.numeric(coef(fit))[5];

                            y = log((exp(run.data[runDataIndex,]$Lower_5pl) + exp(run.data[runDataIndex,]$Upper_5pl))/2);
                            estimated = unname(getConc(fit, y, verbose=TRUE));
                            if (!is.nan(estimated[3]))
                            {
                                run.data[runDataIndex,]$EC50_5pl = estimated[3];
                            }

                            # plot the curve fit for the QC Controls
                            if (titration.data[tIndex,]$QCControl == "true") {
                                plot(fit, type="all", main=analyteName, cex=.5, ylab=paste("log(",yLabel,")",sep=""), xlab=xLabel);
                            }
                        },
                        error = function(e) {
                            print(e);

                            # plot the individual data points for the QC Controls
                            if (titration.data[tIndex,]$QCControl == "true") {
                                plot(fi ~ dose, data = dat, log="x", cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel);
                            }

                        }
                    );
                }    
            } else {
                # create an empty plot indicating that there is no data available
                if (titration.data[tIndex,]$QCControl == "true") {
                    plot(NA, NA, log="x", cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel, xlim=c(1,1), ylim=c(0,1));
                    text(1, 0.5, "Data Not Available");
                }
            }
          }

          # if we are creating a PDF for the QC Control, close the device
          if (titration.data[tIndex,]$QCControl == "true") {
            dev.off();
          }
       }
    }
  }
}  

################################## STEP 4: CALCULATE EST CONC #################################

# read in the analyte information (to get the mapping from analyte to standard/titration)
analyte.data.file = run.props$val1[run.props$name == "analyteData"];
analyte.data = read.delim(analyte.data.file, header=TRUE, sep="\t");

# get the analyte associated standard/titration information from the analyte data file and put it into the run.data object
run.data$Standard = NA;
run.data$well_role = ""; # initialize to empty string and set to Standard accordingly, well_role used by Rumi function
for (index in 1:nrow(analyte.data))
{
    # hold on to the run data for the given analyte
    run.analyte.data = subset(run.data, as.character(name) == as.character(analyte.data$Name[index]));

    # some analytes may have > 1 standard selected
    stndSet = unlist(strsplit(as.character(analyte.data$titrations[index]), ","));

    # if there are more than 1 standard for this analyte, duplicate run.data records for that analyte and set standard accordingly
    if (length(stndSet) > 0)
    {
        for (stndIndex in 1:length(stndSet))
        {
            if (stndIndex == 1)
            {
                run.data$Standard[as.character(run.data$name) == as.character(analyte.data$Name[index])] = stndSet[stndIndex];
                run.data$well_role[as.character(run.data$name) == as.character(analyte.data$Name[index]) & run.data$description == stndSet[stndIndex]] = "Standard";
            } else
            {
                temp.data = run.analyte.data;
                temp.data$Standard = stndSet[stndIndex];
                temp.data$well_role[temp.data$description == stndSet[stndIndex]] = "Standard";
                temp.data$lsid = NA; # lsid will be set by the server
                run.data = rbind(run.data, temp.data);
            }
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
dat = subset(run.data, select=c("dataFile", "Standard", "lsid", "well", "description", "name", "expConc", "fi", "fiBackground", "fiBackgroundBlank", "dilution", "well_role", "summary", "FlaggedAsExcluded", "isStandard", "isQCControl", "isUnknown"));

# if both raw and summary data are available, just use the raw data for the calc
if (bothRawAndSummary) {
    dat = subset(dat, summary == "false");
}

# remove any excluded standard replicate groups
dat = subset(dat, (isStandard & tolower(FlaggedAsExcluded) == "false") | !isStandard);

if (any(dat$isStandard) & length(standards) > 0)
{
    # change column name from "name" to "analyte"
    colnames(dat)[colnames(dat) == "name"] = "analyte";

    # change column name from expConc to expected_conc
    colnames(dat)[colnames(dat) == "expConc"] = "expected_conc";

    # set the sample_id to be description||dilution or description||expected_conc
    dat$sample_id[!is.na(dat$expected_conc)] = paste(dat$description[!is.na(dat$expected_conc)], "||", dat$expected_conc[!is.na(dat$expected_conc)], sep="");
    dat$sample_id[is.na(dat$expected_conc)] = paste(dat$description[is.na(dat$expected_conc)], "||", dat$dilution[is.na(dat$expected_conc)], sep="");

    # choose the FI column for standards and qc controls based on the run property provided by the user, default to the original FI value
    if (any(run.props$name == "StndCurveFitInput"))
    {
        fiCol = getCurveFitInputCol(run.props, "StndCurveFitInput", "fi")
        dat$fi[dat$isStandard] = dat[dat$isStandard, fiCol]
        dat$fi[dat$isQCControl] = dat[dat$isQCControl, fiCol]
    }

    # choose the FI column for unknowns based on the run property provided by the user, default to the original FI value
    if (any(dat$isUnknown))
    {
        if (any(run.props$name == "UnkCurveFitInput"))
        {
            fiCol = getCurveFitInputCol(run.props, "UnkCurveFitInput", "fi")
            dat$fi[dat$isUnknown] = dat[dat$isUnknown, fiCol]
        }
    }

    # subset the dat object to just those records that have an FI
    dat = subset(dat, !is.na(fi));

    # loop through the selected standards in the data.frame and call the rumi function once for each
    # this will also create one pdf for each standard
    for (s in 1:length(standards))
    {
        stndVal = as.character(standards[s]);

        # subset the data for those analytes set to use the given standard curve
        # note: also need to subset the standard records for only those where description matches the given standard
        standard.dat = subset(dat, Standard == stndVal & (!isStandard | (isStandard & description == stndVal)));

        # LabKey Issue 13034: replicate standard records as unknowns so that Rumi will calculated estimated concentrations
        tempStnd.dat = subset(standard.dat, well_role=="Standard");
        if (nrow(tempStnd.dat) > 0)
        {
            tempStnd.dat$well_role = "";
            tempStnd.dat$sample_id = paste(tempStnd.dat$description, "||", tempStnd.dat$expected_conc, sep="");
            standard.dat=rbind(standard.dat, tempStnd.dat);
        }

        # LabKey Issue 13033: check if we need to "add" standard data for any analytes if this is a subclass assay
        #              (i.e. standard data from "Anti-Human" analyte to be used for other analytes)
        selectedAnalytes = unique(standard.dat$analyte);
        subclass.dat = subset(dat, well_role == "Standard" & description == stndVal & !is.na(lsid));
        subclass.dat = subset(subclass.dat, regexpr("^blank", analyte, ignore.case=TRUE) == -1);
        # if we only have standard data for one analyte, it is the subclass standard data to be used
        if (length(unique(subclass.dat$analyte)) == 1)
        {
            subclassAnalyte = subclass.dat$analyte[1];
            for (a in 1:length(selectedAnalytes))
            {
                analyteStnd.dat = subset(standard.dat, well_role == "Standard" & analyte == selectedAnalytes[a]);
                # if there is no standard data for this analyte/standard, "use" the subclass analyte standard data
                if (nrow(analyteStnd.dat) == 0)
                {
                    print(paste("Using ", subclassAnalyte, " standard data for analyte ", selectedAnalytes[a], sep=""));
                    subclass.dat$sample_id = NA;
                    subclass.dat$analyte = selectedAnalytes[a]; 
                    standard.dat = rbind(standard.dat, subclass.dat);
                }
            }
        }

        # set the assay_id (this value will be used in the PDF plot header)
        standard.dat$assay_id = stndVal;

        # check to make sure there are expected_conc values in the standard data frame that will be passed to Rumi
        if (any(!is.na(standard.dat$expected_conc)))
        {
            # use the decided upon conversion function for handling of negative values
            standard.dat$fi = sapply(standard.dat$fi, fiConversion);

            # LabKey issue 13445: Don't calculate estimated concentrations for analytes where max(FI) is < 1000
            agg.dat = subset(standard.dat, well_role == "Standard");
            if (nrow(agg.dat) > 0)
            {
                agg.dat = aggregate(agg.dat$fi, by = list(Standard=agg.dat$Standard,Analyte=agg.dat$analyte), FUN = max);
                for (aggIndex in 1:nrow(agg.dat))
                {
                    # remove the rows from the standard.dat object where the max FI < 1000
                    if (agg.dat$x[aggIndex] < 1000)
                    {
                        print(paste("Max(FI) is < 1000 for", agg.dat$Standard[aggIndex], agg.dat$Analyte[aggIndex], "don't calculate estimated concentrations for this standard/analyte.", sep=" "));
                        standard.dat = subset(standard.dat, !(Standard == agg.dat$Standard[aggIndex] & analyte == agg.dat$Analyte[aggIndex]));
                    }
                }
            }

            # check to make sure that we still have some standard data to pass to the rumi function calculations
            if (nrow(standard.dat) == 0 | !any(standard.dat$isStandard))
            {
                next();
            }

            # call the rumi function to calculate new estimated log concentrations using 5PL for the unknowns
            mypdf(file=paste(stndVal, "5PL", sep="_"), mfrow=c(2,2));
            fits = rumi(standard.dat, force.fit=TRUE, verbose=TRUE);
            fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
            dev.off();

            # put the calculated values back into the run.data dataframe by matching on analyte, description, expConc OR dilution, and standard
            if (nrow(fits) > 0)
            {
                for (index in 1:nrow(fits))
                {
                    a = fits$analyte[index];
                    dil = fits$dilution[index];
                    desc = fits$description[index];
                    exp = fits$expected_conc[index];

                    elc = fits$"est.log.conc"[index];
                    ec = fits$"est.conc"[index];
                    se = fits$"se"[index];

                    if (!is.na(exp)) {
                        runDataIndex = run.data$name == a & run.data$expConc == exp & run.data$description == desc & run.data$Standard == stndVal
                    } else {
                        runDataIndex = run.data$name == a & run.data$dilution == dil & run.data$description == desc & run.data$Standard == stndVal
                    }
                    run.data$EstLogConc_5pl[runDataIndex] = elc;
                    run.data$EstConc_5pl[runDataIndex] = ec;
                    run.data$SE_5pl[runDataIndex] = se;
                }

                # convert Inf and -Inf to Java string representation for DB persistance
                run.data$SE_5pl[run.data$SE_5pl == "Inf"] = "Infinity";
                run.data$SE_5pl[run.data$SE_5pl == "-Inf"] = "-Infinity";
            }

            # call the rumi function to calculate new estimated log concentrations using 4PL for the unknowns
            mypdf(file=paste(stndVal, "4PL", sep="_"), mfrow=c(2,2));
            fits = rumi(standard.dat, fit.4pl=TRUE, force.fit=TRUE, verbose=TRUE);
            fits$"est.conc" = 2.71828183 ^ fits$"est.log.conc";
            dev.off();

            # put the calculated values back into the run.data dataframe by matching on analyte, description, dilution, and standard
            if (nrow(fits) > 0)
            {
                for (index in 1:nrow(fits))
                {
                    a = fits$analyte[index];
                    dil = fits$dilution[index];
                    desc = fits$description[index];
                    exp = fits$expected_conc[index];

                    elc = fits$"est.log.conc"[index];
                    ec = fits$"est.conc"[index];
                    se = fits$"se"[index];

                    if (!is.na(exp)) {
                        runDataIndex = run.data$name == a & run.data$expConc == exp & run.data$description == desc & run.data$Standard == stndVal
                    } else {
                        runDataIndex = run.data$name == a & run.data$dilution == dil & run.data$description == desc & run.data$Standard == stndVal
                    }
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
}

################################## STEP 5: Positivity Calculation ################################

run.data$Positivity = NA;

# get the run property that are used for teh positivity calculation
calc.positivity = getRunPropertyValue(run.props, "CalculatePositivity");
base.visit = getRunPropertyValue(run.props, "BaseVisit");
fold.change = getRunPropertyValue(run.props, "PositivityFoldChange");

# if all run props are specified, and calc positivity is true
if (!is.na(calc.positivity) & !is.na(base.visit) & !is.na(fold.change) & calc.positivity == "1")
{
    analytePtids = subset(run.data, select=c("name", "participantID")); # note: analyte variable column name is "name"
    analytePtids = unique(analytePtids[!is.na(run.data$participantID),]);
    if (nrow(analytePtids) > 0)
    {
        for (index in 1:nrow(analytePtids))
        {
            # default MFI threshold is set by the server, but that can be changed manually on upload per analyte
            threshold = NA;
            if (!is.null(analyte.data$PositivityThreshold))
            {
                if (!is.na(analyte.data$PositivityThreshold[analyte.data$Name == analytePtids$name[index]]))
                {
                    threshold = analyte.data$PositivityThreshold[analyte.data$Name == analytePtids$name[index]];
                }
            }

            # calculate the positivity by comparing all non-baseline visits with the baseline visit value times the fold change specified
            fi.dat = subset(run.data, name == analytePtids$name[index] & participantID == analytePtids$participantID[index], select=c("fiBackground", "fiBackgroundBlank"));
            visits.dat = subset(run.data, name == analytePtids$name[index] & participantID == analytePtids$participantID[index], select=c("name", "participantID", "visitID"));
            visits.fi.agg = aggregate(fi.dat, by = list(analyte=visits.dat$name, ptid=visits.dat$participantID, visit=visits.dat$visitID), FUN = mean);
            if (!is.na(threshold) & any(compareNumbersForEquality(visits.fi.agg$visit, base.visit, 1e-10)))
            {
                baseVisitFiBkgd = fiConversion(visits.fi.agg$fiBackground[compareNumbersForEquality(visits.fi.agg$visit, base.visit, 1e-10)]);
                baseVisitFiBkgdBlank = fiConversion(visits.fi.agg$fiBackgroundBlank[compareNumbersForEquality(visits.fi.agg$visit, base.visit, 1e-10)]);
                if (!is.na(baseVisitFiBkgd) & !is.na(baseVisitFiBkgdBlank))
                {
                    for (v in 1:nrow(visits.fi.agg))
                    {
                        # for each non-baseline visit, verify that the FI-Bkgd and FI-Bkgd-Blank values are above the specified threshold for that analyte
                        visit = visits.fi.agg$visit[v];
                        if (!compareNumbersForEquality(visit, base.visit, 1e-10))
                        {
                            # if the FI-Bkgd and FI-Bkgd-Blank values are greater than the baseline visit value * fold change, consider them positive
                            runDataIndex = run.data$name == visits.fi.agg$analyte[v] & run.data$participantID == visits.fi.agg$ptid[v] & run.data$visitID == visits.fi.agg$visit[v];
                            if ((visits.fi.agg$fiBackground[v] > threshold) & (visits.fi.agg$fiBackground[v] > (baseVisitFiBkgd * as.numeric(fold.change))) &
                                (visits.fi.agg$fiBackgroundBlank[v] > threshold) & (visits.fi.agg$fiBackgroundBlank[v] > (baseVisitFiBkgdBlank * as.numeric(fold.change))))
                            {
                                run.data$Positivity[runDataIndex] = "positive"
                            } else
                            {
                                run.data$Positivity[runDataIndex] = "negative"
                            }
                        }
                    }
                }
            }
        }
    }
}

#####################  STEP 6: WRITE THE RESULTS TO THE OUTPUT FILE LOCATION #####################

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

# print the ending time for the transform script
writeLines(paste("\nProcessing end time:",Sys.time(),sep=" "));
