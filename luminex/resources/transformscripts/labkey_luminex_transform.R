#
# Copyright (c) 2014-2015 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Luminex Assay.
#
# First, the script subtracts the FI-Bkgd value for the negative bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI-Bkgd and FI-Bkgd-Neg
# values that are <= 0 to 1 (as per the lab's request).
#
# Next, the script calculates curve fit parameters for each titration/analyte combination using both
# 4PL and 5PL curve fits (from the fit.drc function in the Ruminex package (developed by Youyi at SCHARP).
#
# Then, the script calculates new estimated concentration values for unknown samples using the
# rumi function. The rumi function takes a dataframe as input and uses the given Standard curve data to
# calculate est.log.conc an se for the unknowns.
#
# CHANGES :
#  - 2.1.20111216 : Issue 13696: Luminex transform script should use excel file titration "Type" for EC50 and Conc calculations
#  - 2.2.20120217 : Issue 14070: Value out of range error when importing curve fit parameters for titrated unknown with flat dilution curve
#  - 3.0.20120323 : Changes for LabKey server 12.1
#  - 3.1.20120629 : Issue 15279: Luminex Positivity Calculation incorrect for titrated unknowns incorrect
#  - 4.0.20120509 : Changes for LabKey server 12.2
#  - 4.1.20120806 : Issue 15709: Luminex tranform : QC Control plots not displayed when EC50 value out of acceptable range
#  - 4.2.20121121 : Changes for LabKey server 12.3, Issue 15042: Transform script (and AUC calculation error) when luminex file uploaded that has an ExpConc value of zero for a standard well
#  - 5.0.20121210 : Change for LabKey server 13.1
#  - 5.1.20130424 : Move fix for Issue 15042 up to earliest curve fit calculation
#  - 6.0.20140117 : Changes for LabKey server 13.3, Issue 19391: Could not convert '-Inf' for field EstLogConc_5pl
#  - 7.0.20140207 : Changes for LabKey server 14.1: refactor script to use more function calls, calculate positivity based on baselines from another run in the same folder
#                   Move positivity calculation part of script to separate, lab specific, transform script
#  - 7.1.20140526 : Issue 20457: Negative blank bead subtraction results in FI-Bkgd-Blank greater than FI-Bkgd
#  - 8.0.20140509 : Changes for LabKey server 14.2: add run property to allow calc. of 4PL EC50 and AUC on upload without running Ruminex (see SkipRumiCalculation below)
#  - 8.1.20140612 : Issue 20316: Rumi estimated concentrations not calculated for unselected titrated unknowns in subclass assay case
#  - 9.0.20140716 : Changes for LabKey server 14.3: add Other Control type for titrations
#  - 9.1.20140718 : Allow use of alternate negative control bead on per-analyte basis (FI-Bkgd-Neg instead of FI-Bkgd-Blank)
#  - 9.2.20141103 : Issue 21268: Add OtherControl titrations to PDF output of curves from transform script
#  - 10.0.20150910 : Changes for LabKey server 15.2. Issue 23230: Luminex transform script error when standard or QC control name has a slash in it
#
# Author: Cory Nathe, LabKey
transformVersion = "10.0.20150910";

# print the starting time for the transform script
writeLines(paste("Processing start time:",Sys.time(),"\n",sep=" "));

source("${srcDirectory}/youtil.R");
# Ruminex package available from https://labkey.org/Documentation/wiki-page.view?name=configureLuminexScript
suppressMessages(library(Ruminex));
ruminexVersion = installed.packages()["Ruminex","Version"];

rVersion = paste(R.version$major, R.version$minor, R.version$arch, R.version$os, sep=".");

########################################## FUNCTIONS ##########################################

getCurveFitInputCol <- function(runProps, fiRunCol, defaultFiCol)
{
    runCol = runProps$val1[runProps$name == fiRunCol];
    if (runCol == "FI") {
        runCol = "fi"
    } else if (runCol == "FI-Bkgd") {
    	runCol = "fiBackground"
    } else if (runCol == "FI-Bkgd-Blank" | runCol == "FI-Bkgd-Neg") {
    	runCol = "FIBackgroundNegative"
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
    } else if (fiCol == "FIBackgroundNegative") {
        displayVal = "FI-Bkgd-Neg"
    }
    displayVal;
}

fiConversion <- function(val)
{
    1 + max(val,0);
}

# fix for Issue 14070 - capp the values at something that can be stored in the DB
maxValueConversion <- function(val)
{
    min(val, 10e37);
}

getRunPropertyValue <- function(colName)
{
    value = NA;
    if (any(run.props$name == colName))
    {
        value = run.props$val1[run.props$name == colName];

        # return NA for an empty string
        if (nchar(value) == 0)
        {
            value = NA;
        }
    }
    value;
}

readRunPropertiesFile <- function()
{
    # set up a data frame to store the run properties
    properties = data.frame(NA, NA, NA, NA);
    colnames(properties) = c("name", "val1", "val2", "val3");

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

        # add the parts for the given run property to the properties data frame
        properties[i,] = parts;
    }

    properties
}

populateTitrationData <- function(rundata, titrationdata)
{
    rundata$isStandard = FALSE;
    rundata$isQCControl = FALSE;
    rundata$isUnknown = FALSE;
    rundata$isOtherControl = FALSE;

    # apply the titration data to the rundata object
    if (nrow(titrationdata) > 0)
    {
        for (tIndex in 1:nrow(titrationdata))
        {
            titrationName = as.character(titrationdata[tIndex,]$Name);
            titrationRows = rundata$titration == "true" & rundata$description == titrationName;
            rundata$isStandard[titrationRows] = (titrationdata[tIndex,]$Standard == "true");
            rundata$isQCControl[titrationRows] = (titrationdata[tIndex,]$QCControl == "true");
            rundata$isOtherControl[titrationRows] = (titrationdata[tIndex,]$OtherControl == "true");
        }
    }

    # Issue 20316: incorrectly labeling unselected titrated unknowns as not "isUnknown"
    rundata$isUnknown[!(rundata$isStandard | rundata$isQCControl | rundata$isOtherControl)] = TRUE;

    rundata
}

isNegativeControl <- function(analytedata, analyteVal)
{
    negControl = FALSE;
    if (!is.null(analytedata$NegativeControl))
    {
        if (!is.na(analytedata$NegativeControl[analytedata$Name == analyteVal]))
        {
            negControl = as.logical(analytedata$NegativeControl[analytedata$Name == analyteVal]);
        }
    }

    negControl
}

populateNegativeBeadSubtraction <- function(rundata, analytedata)
{
    # initialize the FI-Bkgd-Neg variable
    rundata$FIBackgroundNegative = NA;

    # read the run property from user to determine if we are to subtract the negative control bead from unks only
    unksIndex = !(rundata$isStandard | rundata$isQCControl | rundata$isOtherControl);
    unksOnly = TRUE;
    if (any(run.props$name == "SubtNegativeFromAll"))
    {
        if (getRunPropertyValue("SubtNegativeFromAll") == "1")
        {
            unksOnly = FALSE;
        }
    }

    # loop through each analyte and subtract the negative control bead as specified in the analytedata
    for (index in 1:nrow(analytedata))
    {
       analyteName = analytedata$Name[index];
       negativeBeadName = as.character(analytedata$NegativeBead[index]);
       negativeControl = isNegativeControl(analytedata, analyteName);

       # store a boolean vector of indices for negControls and analyte unknowns
       analyteIndex = rundata$name == analyteName;
       negControlIndex = rundata$name == negativeBeadName;

       if (!negativeControl & !is.na(negativeBeadName) & any(negControlIndex) & any(analyteIndex))
       {
           # loop through the unique dataFile/description/excpConc/dilution combos and subtract the mean
           # negative control fiBackground from the fiBackground of the given analyte
           negControlData = rundata[negControlIndex,];
           combos = unique(subset(negControlData, select=c("dataFile", "description", "dilution", "expConc")));

           for (index in 1:nrow(combos))
           {
                dataFile = combos$dataFile[index];
                description = combos$description[index];
                dilution = combos$dilution[index];
                expConc = combos$expConc[index];

                # only standards have expConc, the rest are NA
                combo = rundata$dataFile == dataFile & rundata$description == description & rundata$dilution == dilution & !is.na(rundata$expConc) & rundata$expConc == expConc;
                if (is.na(expConc))
                {
                    combo = rundata$dataFile == dataFile & rundata$description == description & rundata$dilution == dilution & is.na(rundata$expConc);
                }

                # get the mean negative bead FI-Bkgrd values for the given description/dilution
                # issue 20457: convert negative "negative control" mean to zero to prevent subtracting a negative
                negControlMean = max(mean(rundata$fiBackground[negControlIndex & combo]), 0);

                # calc the FIBackgroundNegative for all of the non-"Negative Control" analytes for this combo
                if (unksOnly) {
                    rundata$FIBackgroundNegative[unksIndex & analyteIndex & combo] = rundata$fiBackground[unksIndex & analyteIndex & combo] - negControlMean;
                } else{
                    rundata$FIBackgroundNegative[analyteIndex & combo] = rundata$fiBackground[analyteIndex & combo] - negControlMean;
                }
           }
       }
    }

    rundata
}

writeErrorOrWarning <- function(type, msg)
{
    write(paste(type, type, msg, sep="\t"), file=error.file, append=TRUE);
    if (type == "error") {
        quit("no", 0, FALSE);
    }
}

convertToFileName <- function(name)
{
    # Issue 23230: slashes in the file name cause issues creating the PDFs, for now convert "/" and " " to "_"
    gsub("[/ ]", "_", name);
}

######################## STEP 0: READ IN THE RUN PROPERTIES AND RUN DATA #######################

run.props = readRunPropertiesFile();

# save the important run.props as separate variables
run.data.file = getRunPropertyValue("runDataFile");
run.output.file = run.props$val3[run.props$name == "runDataFile"];
error.file = getRunPropertyValue("errorsFile");

# read in the run data file content
run.data = read.delim(run.data.file, header=TRUE, sep="\t");

# read in the analyte information (to get the mapping from analyte to standard/titration)
analyte.data.file = getRunPropertyValue("analyteData");
analyte.data = read.delim(analyte.data.file, header=TRUE, sep="\t");

# read in the titration information
titration.data.file = getRunPropertyValue("titrationData");
titration.data = data.frame();
if (file.exists(titration.data.file)) {
    titration.data = read.delim(titration.data.file, header=TRUE, sep="\t");
}
run.data <- populateTitrationData(run.data, titration.data);

# determine if the data contains both raw and summary data
# if both exists, only the raw data will be used for the calculations
bothRawAndSummary = any(run.data$summary == "true") & any(run.data$summary == "false");

######################## STEP 1: SET THE VERSION NUMBERS ################################

runprop.output.file = getRunPropertyValue("transformedRunPropertiesFile");
fileConn<-file(runprop.output.file);
writeLines(c(paste("TransformVersion",transformVersion,sep="\t"),
    paste("RuminexVersion",ruminexVersion,sep="\t"),
    paste("RVersion",rVersion,sep="\t")), fileConn);
close(fileConn);

################################# STEP 2: NEGATIVE BEAD SUBTRACTION ################################

run.data <- populateNegativeBeadSubtraction(run.data, analyte.data);

################################## STEP 3: TITRATION CURVE FIT #################################

# initialize the curve coefficient variables
run.data$Slope_4pl = NA;
run.data$Lower_4pl = NA;
run.data$Upper_4pl = NA;
run.data$Inflection_4pl = NA;
run.data$EC50_4pl = NA;
run.data$Flag_4pl = NA;
run.data$Slope_5pl = NA;
run.data$Lower_5pl = NA;
run.data$Upper_5pl = NA;
run.data$Inflection_5pl = NA;
run.data$Asymmetry_5pl = NA;
run.data$EC50_5pl = NA;
run.data$Flag_5pl = NA;

# get the unique analyte values
analytes = unique(run.data$name);

# determine if the curve fits should be done with or without log transform
curveFitLogTransform = TRUE;
if (any(run.props$name == "CurveFitLogTransform")) {
    propVal = getRunPropertyValue("CurveFitLogTransform");
    if (!is.na(propVal) & propVal != "1") curveFitLogTransform = FALSE;
}

# set the weighting variance variable for use in the non-log tranform curve fits
drm.weights.var.power = -1.8;
if (any(run.props$name == "WeightingPower")) {
    propVal = getRunPropertyValue("WeightingPower");
    if (!is.na(propVal) & propVal != "") drm.weights.var.power = as.numeric(propVal);
}

# loop through the possible titrations and to see if it is a standard, qc control, or titrated unknown
if (nrow(titration.data) > 0)
{
  for (tIndex in 1:nrow(titration.data))
  {
    titrationDataRow = titration.data[tIndex,];

    if (titrationDataRow$Standard == "true" |
        titrationDataRow$QCControl == "true" |
        titrationDataRow$OtherControl == "true" |
        titrationDataRow$Unknown == "true")
    {
       titrationName = as.character(titrationDataRow$Name);

       # 2 types of curve fits for the EC50 calculations, with separate PDFs for the QC Curves
       fitTypes = c("4pl", "5pl");
       for (typeIndex in 1:length(fitTypes))
       {
          # we want to create PDF plots of the curves for QC Controls
          if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
              mypdf(file=paste(convertToFileName(titrationName), "Control_Curves", toupper(fitTypes[typeIndex]), sep="_"), mfrow=c(1,1));
          }

          # calculate the curve fit params for each analyte
          for (aIndex in 1:length(analytes))
          {
            analyteName = as.character(analytes[aIndex]);
            print(paste("Calculating the", fitTypes[typeIndex], "curve fit params for ",titrationName, analyteName, sep=" "));
            dat = subset(run.data, description == titrationName & name == analyteName);

            yLabel = "";
            if (titrationDataRow$Standard == "true" | titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
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

                # Issue 15042: check to make sure all of the ExpConc/Dilution values are non-rounded (i.e. not zero)
                zeroDoses = unique(subset(dat, dose==0, select=c("description", "well")));
                if (nrow(zeroDoses) > 0)
                {
                    wells = paste(zeroDoses$description, zeroDoses$well, collapse=', ');
                    writeErrorOrWarning("error", paste("Error: Zero values not allowed in dose (i.e. ExpConc/Dilution) for titration curve fit calculation:", wells));
                }

                if (fitTypes[typeIndex] == "4pl")
                {
                    tryCatch({
                            fit = drm(fi~dose, data=dat, fct=LL.4());
                            run.data[runDataIndex,]$Slope_4pl = maxValueConversion(as.numeric(coef(fit))[1]);
                            run.data[runDataIndex,]$Lower_4pl = maxValueConversion(as.numeric(coef(fit))[2]);
                            run.data[runDataIndex,]$Upper_4pl = maxValueConversion(as.numeric(coef(fit))[3]);
                            run.data[runDataIndex,]$Inflection_4pl = maxValueConversion(as.numeric(coef(fit))[4]);

                            ec50 = maxValueConversion(as.numeric(coef(fit))[4]);
                            if (ec50 > 10e6) {
                                writeErrorOrWarning("warn", paste("Warning: EC50 4pl value over the acceptable level (10e6) for ", titrationName, " ", analyteName, ".", sep=""));
                            } else {
                                run.data[runDataIndex,]$EC50_4pl = ec50
                            }

                            # plot the curve fit for the QC Controls
                            if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                                plot(fit, type="all", main=analyteName, cex=.5, ylab=yLabel, xlab=xLabel);
                            }
                        },
                        error = function(e) {
                            print(e);

                            # plot the individual data points for the QC Controls
                            if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                                plot(fi ~ dose, data = dat, log="x", cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel);
                            }
                        }
                    );

                    # set the failure flag if there is no EC50 value at this point
                    if (all(is.na(run.data[runDataIndex,]$EC50_4pl))) {
                        run.data[runDataIndex,]$Flag_4pl = TRUE;
                    }
                } else if (fitTypes[typeIndex] == "5pl")
                {
                    tryCatch({
                            if (curveFitLogTransform) {
                                formula = log(fi)~dose
                                weighting = FALSE
                            } else {
                                formula = fi~dose
                                weighting = TRUE
                                dat.avg=aggregate(dat$fi, by = list(dat$name, dat$dose, dat$dataId), mean)
                                names(dat.avg) = c("name", "dose","dataId", "fi.avg")
                                dat = merge(dat, dat.avg, by=c("name", "dose", "dataId"), all.x=T, all.y=T)
                            }
                            fit = fit.drc(formula, data=dat, weighting=weighting, force.fit=TRUE, fit.4pl=FALSE);
                            run.data[runDataIndex,]$Slope_5pl = maxValueConversion(as.numeric(coef(fit))[1]);
                            run.data[runDataIndex,]$Lower_5pl = maxValueConversion(as.numeric(coef(fit))[2]);
                            run.data[runDataIndex,]$Upper_5pl = maxValueConversion(as.numeric(coef(fit))[3]);
                            run.data[runDataIndex,]$Inflection_5pl = maxValueConversion(as.numeric(coef(fit))[4]);
                            run.data[runDataIndex,]$Asymmetry_5pl = maxValueConversion(as.numeric(coef(fit))[5]);

                            if (curveFitLogTransform) {
                                yLabel = paste("log(",yLabel,")", sep="");
                                y = log((exp(run.data[runDataIndex,]$Lower_5pl) + exp(run.data[runDataIndex,]$Upper_5pl)) / 2)
                            } else {
                                y = (run.data[runDataIndex,]$Lower_5pl + run.data[runDataIndex,]$Upper_5pl) / 2;
                            }
                            ec50 = unname(getConc(fit, y))[3];
                            if (is.nan(ec50) | ec50 > 10e6) {
                                writeErrorOrWarning("warn", paste("Warning: EC50 5pl value out of acceptable range (either outside standards MFI or greater than 10e6) for ", titrationName, " ", analyteName, ".", sep=""));
                            } else {
                                run.data[runDataIndex,]$EC50_5pl = ec50;
                            }

                            # plot the curve fit for the QC Controls
                            if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                                plot(fit, type="all", main=analyteName, cex=.5, ylab=yLabel, xlab=xLabel);
                            }
                        },
                        error = function(e) {
                            print(e);

                            # plot the individual data points for the QC Controls
                            if (curveFitLogTransform) {
                                yLabel = paste("log(",yLabel,")", sep="");
                                logAxes = "xy"
                            } else {
                                logAxes = "x";
                            }
                            if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                                plot(fi ~ dose, data = dat, log=logAxes, cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel);
                            }
                        }
                    );

                    # set the failure flag if there is no EC50 value at this point
                    if (all(is.na(run.data[runDataIndex,]$EC50_5pl))) {
                        run.data[runDataIndex,]$Flag_5pl = TRUE;
                    }
                }
            } else {
                # create an empty plot indicating that there is no data available
                if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                    if (curveFitLogTransform) {
                        yLabel = paste("log(",yLabel,")", sep="");
                        logAxes = "xy"
                    } else {
                        logAxes = "x";
                    }
                    plot(NA, NA, log=logAxes, cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel, xlim=c(1,1), ylim=c(0,1));
                    text(1, 0.5, "Data Not Available");
                }
            }
          }

          # if we are creating a PDF for the QC Control, close the device
          if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
            dev.off();
          }
       }
    }
  }
}

################################## STEP 4: CALCULATE EST CONC #################################

# initialize the columns to be calculated
run.data$EstLogConc_5pl = NA;
run.data$EstConc_5pl = NA;
run.data$SE_5pl = NA;

run.data$EstLogConc_4pl = NA;
run.data$EstConc_4pl = NA;
run.data$SE_4pl = NA;

run.data$Standard = NA;
run.data$well_role = ""; # initialize to empty string and set to Standard accordingly, well_role used by Rumi function

# determine if the Ruminex curve fits should be run
runRumiCalculation = TRUE;
if (any(run.props$name == "SkipRumiCalculation")) {
    propVal = getRunPropertyValue("SkipRumiCalculation");
    if (!is.na(propVal) & propVal == "1") runRumiCalculation = FALSE;
}

if (runRumiCalculation)
{
    # get the analyte associated standard/titration information from the analyte data file and put it into the run.data object
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

    # setup the dataframe needed for the call to rumi
    dat = subset(run.data, select=c("dataFile", "Standard", "lsid", "well", "description", "name", "expConc", "fi", "fiBackground", "FIBackgroundNegative", "dilution", "well_role", "summary", "FlaggedAsExcluded", "isStandard", "isQCControl", "isOtherControl", "isUnknown"));

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
            dat$fi[dat$isOtherControl] = dat[dat$isOtherControl, fiCol]
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
                            writeErrorOrWarning("warn", paste("Warning: Max(FI) is < 1000 for ", agg.dat$Standard[aggIndex], " ", agg.dat$Analyte[aggIndex], ", not calculating estimated concentrations for this standard/analyte.", sep=""));
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
                mypdf(file=paste(convertToFileName(stndVal), "5PL", sep="_"), mfrow=c(2,2));
                fits = rumi(standard.dat, force.fit=TRUE, log.transform=curveFitLogTransform, plot.se.profile=curveFitLogTransform, verbose=TRUE);
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
                }

                # call the rumi function to calculate new estimated log concentrations using 4PL for the unknowns
                mypdf(file=paste(convertToFileName(stndVal), "4PL", sep="_"), mfrow=c(2,2));
                fits = rumi(standard.dat, fit.4pl=TRUE, force.fit=TRUE, log.transform=curveFitLogTransform, plot.se.profile=curveFitLogTransform, verbose=TRUE);
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
                }
            }
        }
    }
}

#####################  STEP 5: WRITE THE RESULTS TO THE OUTPUT FILE LOCATION #####################

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

# print the ending time for the transform script
writeLines(paste("\nProcessing end time:",Sys.time(),sep=" "));
