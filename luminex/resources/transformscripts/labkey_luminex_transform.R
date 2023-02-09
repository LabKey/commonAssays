#
# Copyright (c) 2014-2019 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Luminex Assay.
#
# The script subtracts the FI-Bkgd value for the negative bead from the FI-Bkgd value
# for the other analytes within a given run data file. It also converts FI-Bkgd and FI-Bkgd-Neg
# values that are <= 0 to 1 (as per the lab's request).
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
#  - 10.1.20180903 : Use transform script helper functions from Rlabkey package
#  - 11.0.20230206 : Remove Ruminex package usage and calculations
#
# Author: Cory Nathe, LabKey
transformVersion = "11.0.20230206";

# print the starting time for the transform script
writeLines(paste("Processing start time:",Sys.time(),"\n",sep=" "));

source("${srcDirectory}/youtil.R");
suppressMessages(library(drc));

suppressMessages(library(Rlabkey));

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
        negControlVal = as.logical(analytedata$NegativeControl[analytedata$Name == analyteVal]);
        if (!is.na(negControlVal))
        {
            negControl = negControlVal
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
        if (labkey.transform.getRunPropertyValue(run.props, "SubtNegativeFromAll") == "1")
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

run.props = labkey.transform.readRunPropertiesFile("${runInfo}");

# save the important run.props as separate variables
run.data.file = labkey.transform.getRunPropertyValue(run.props, "runDataFile");
run.output.file = run.props$val3[run.props$name == "runDataFile"];
error.file = labkey.transform.getRunPropertyValue(run.props, "errorsFile");

# read in the run data file content
run.data = read.delim(run.data.file, header=TRUE, sep="\t");

# read in the analyte information (to get the mapping from analyte to standard/titration)
analyte.data.file = labkey.transform.getRunPropertyValue(run.props, "analyteData");
analyte.data = read.delim(analyte.data.file, header=TRUE, sep="\t");

# read in the titration information
titration.data.file = labkey.transform.getRunPropertyValue(run.props, "titrationData");
titration.data = data.frame();
if (file.exists(titration.data.file)) {
    titration.data = read.delim(titration.data.file, header=TRUE, sep="\t");
}
run.data <- populateTitrationData(run.data, titration.data);

# determine if the data contains both raw and summary data
# if both exists, only the raw data will be used for the calculations
bothRawAndSummary = any(run.data$summary == "true") & any(run.data$summary == "false");

######################## STEP 1: SET THE VERSION NUMBERS ################################

runprop.output.file = labkey.transform.getRunPropertyValue(run.props, "transformedRunPropertiesFile");
fileConn<-file(runprop.output.file);
writeLines(c(paste("TransformVersion",transformVersion,sep="\t"),
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

# get the unique analyte values
analytes = unique(run.data$name);

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

       # use 4pl curve fit for the EC50 calculations, with separate PDFs for the QC Curves
       fitTypes = c("4pl");
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
                }
            } else {
                # create an empty plot indicating that there is no data available
                if (titrationDataRow$QCControl == "true" | titrationDataRow$OtherControl == "true") {
                    plot(NA, NA, log="x", cex=.5, las=1, main=paste("FAILED:", analyteName, sep=" "), ylab=yLabel, xlab=xLabel, xlim=c(1,1), ylim=c(0,1));
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

#####################  STEP 4: WRITE THE RESULTS TO THE OUTPUT FILE LOCATION #####################

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

# print the ending time for the transform script
writeLines(paste("\nProcessing end time:",Sys.time(),sep=" "));
