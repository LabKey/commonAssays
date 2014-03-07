#
# Copyright (c) 2011-2014 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# Transform script for Tomaras Lab Luminex Assay.
#
# This script is for Tomaras lab specific calculations and uses the output from the labkey_luminex_transform.R script.
#
# The script calculates positivity of samples by comparing 3x or 5x fold change of the baseline visit
# FI-Bkgd and FI-Bkgd-Blank values with other visits.
#
# CHANGES :
#  - 1.0.20140228 : Move positivity calculation part of script out of labkey_luminex
#
# Author: Cory Nathe, LabKey
labTransformVersion = "1.0.20140228";

# print the starting time for the transform script
writeLines(paste("Processing start time:",Sys.time(),"\n",sep=" "));

${rLabkeySessionId}
suppressMessages(library(Rlabkey));

########################################## FUNCTIONS ##########################################

fiConversion <- function(val)
{
    1 + max(val,0);
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

# for Issue 15279 - use just the min dilution per visit for positivity calculation
getVisitsByMinDilution <- function(visitDat)
{
	tempVisitDat = data.frame();
	for (v in unique(visitDat$visit))
	{
		v.dat = subset(visitDat, visit = v);
		tempVisitDat = (v.dat[v.dat$dilution == min(v.dat$dilution),]);
	}
	tempVisitDat
}

compareNumbersForEquality <- function(val1, val2, epsilon)
{
	val1 = as.numeric(val1);
	val2 = as.numeric(val2);
	equal = val1 > (val2 - epsilon) & val1 < val2 + epsilon;
	equal
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
    rundata$isStandard = NA;
    rundata$isQCControl = NA;
    rundata$isUnknown = NA;

    # apply the titration data to the rundata object
    rundata$isStandard[rundata$titration == "false"] = FALSE;
    rundata$isQCControl[rundata$titration == "false"] = FALSE;
    rundata$isUnknown[rundata$titration == "false"] = TRUE;
    if (nrow(titrationdata) > 0)
    {
        for (tIndex in 1:nrow(titrationdata))
        {
            titrationName = as.character(titrationdata[tIndex,]$Name);
            rundata$isStandard[rundata$titration == "true" & rundata$description == titrationName] = (titrationdata[tIndex,]$Standard == "true");
            rundata$isQCControl[rundata$titration == "true" & rundata$description == titrationName] = (titrationdata[tIndex,]$QCControl == "true");
            rundata$isUnknown[rundata$titration == "true" & rundata$description == titrationName] = (titrationdata[tIndex,]$Unknown == "true");
        }
    }

    rundata
}

getPositivityThreshold <- function(analytedata, analyteVal)
{
    # default MFI threshold is set by the server, but that can be changed manually on upload per analyte
    threshold = NA;
    if (!is.null(analytedata$PositivityThreshold))
    {
        if (!is.na(analytedata$PositivityThreshold[analytedata$Name == analyteVal]))
        {
            threshold = analytedata$PositivityThreshold[analytedata$Name == analyteVal];
        }
    }

    threshold
}

isNegativeControl <- function(analytedata, analyteVal)
{
    negControl = FALSE;
    if (!is.null(analytedata$NegativeControl))
    {
        if (!is.na(analytedata$NegativeControl[analytedata$Name == analyteVal]))
        {
            negControl = analytedata$NegativeControl[analytedata$Name == analyteVal];
        }
    }

    negControl
}

getVisitsFIAggData <- function(rundata, fidata, analyteVal, participantVal)
{
    # Issue 15279, for titrated unknowns, just use the smallest/minimum dilution for the positivity calculation
    visitDilutionDat = subset(rundata, name == analyteVal & participantID == participantVal & !is.na(visitID) & !is.na(dilution), select=c("name", "participantID", "visitID", "dilution"));
    visitDiliutionFIAgg = aggregate(fidata, by = list(analyte=visitDilutionDat$name, ptid=visitDilutionDat$participantID, visit=visitDilutionDat$visitID, dilution=visitDilutionDat$dilution), FUN = mean);
    getVisitsByMinDilution(visitDiliutionFIAgg);
}

determineIndividualPositivityValue <- function(visitsFIagg, index, threshold, baseVisitFiBkgd, baseVisitFiBkgdBlank, foldchange)
{
    val = NA;

    if (!is.na(visitsFIagg$fiBackground[index]) & !is.na(visitsFIagg$fiBackgroundBlank[index]))
    {
        # if the FI-Bkgd and FI-Bkgd-Blank values are greater than the baseline visit value * fold change, consider them positive
        if (!is.na(baseVisitFiBkgd) & !is.na(baseVisitFiBkgdBlank))
        {
            if ((visitsFIagg$fiBackground[index] > threshold) & (visitsFIagg$fiBackground[index] > (baseVisitFiBkgd * as.numeric(foldchange))) &
                (visitsFIagg$fiBackgroundBlank[index] > threshold) & (visitsFIagg$fiBackgroundBlank[index] > (baseVisitFiBkgdBlank * as.numeric(foldchange))))
            {
                val = "positive"
            } else
            {
                val = "negative"
            }
        } else
        {
            # since there is no baseline data for this participant, compare each visit FI-Bkgd and FI-Bkgd-Blank values against the threshold
            if (visitsFIagg$fiBackground[index] > threshold & visitsFIagg$fiBackgroundBlank[index] > threshold)
            {
                val = "positive"
            } else
            {
                val = "negative"
            }
        }
    }

    val
}

getBaselineVisitFiValue <- function(ficolumn, visitsfiagg, baselinedata, basevisit)
{
    val = NA;

    # if there is a baseline visit supplied, we expect each ptid to have baseline visit data
    if (!any(compareNumbersForEquality(visitsfiagg$visit, basevisit, 1e-10)))
    {
        ptid = visitsfiagg[1, "ptid"];
        analyte = visitsfiagg[1, "analyte"];
        dilution = visitsfiagg[1, "dilution"];

        if (nrow(baselinedata) > 0) {
            rowIndex = baselinedata$participantid==ptid & baselinedata$analyte==analyte & baselinedata$dilution==dilution;
            if (any(rowIndex)) {
                val = baselinedata[rowIndex, tolower(ficolumn)];
            }
        }

        if (is.na(val)) {
            writeErrorOrWarning("warn", paste("Warning: No baseline visit data found: Analyte=", analyte, ", Participant=", ptid,
                         ", Visit=", basevisit, ", Column=", ficolumn, ".", sep=""));
        } else if (baselinedata[rowIndex, "dataidcount"] > 1)
        {
            writeErrorOrWarning("error", paste("Error: Baseline visit data found in more than one prevoiusly uploaded run: Analyte=", analyte,
                         ", Participant=", ptid, ", Visit=", basevisit, ".", sep=""));
        }
    } else
    {
        val = fiConversion(visitsfiagg[compareNumbersForEquality(visitsfiagg$visit, basevisit, 1e-10), ficolumn]);
    }

    val
}

calculatePositivityForAnalytePtid <- function(rundata, analytedata, baselinedata, analyteVal, participantVal, basevisit, foldchange)
{
    # calculate the positivity by comparing all non-baseline visits with the baseline visit value times the fold change specified,
    # for any participants that do not have baseline data, just compare against the threshold
    # skip those analytes that are designated as negative controls

    threshold = getPositivityThreshold(analytedata, analyteVal);
    negativeControl = isNegativeControl(analytedata, analyteVal);

    if (!is.na(threshold) & !negativeControl)
    {
        fidata = subset(rundata, name == analyteVal & participantID == participantVal & !is.na(visitID) & !is.na(dilution), select=c("fiBackground", "fiBackgroundBlank"));
        if (nrow(fidata) > 0)
        {
            visitsFIagg = getVisitsFIAggData(rundata, fidata, analyteVal, participantVal);

            if (!is.na(basevisit))
            {
                baseVisitFiBkgd = getBaselineVisitFiValue("fiBackground", visitsFIagg, baselinedata, basevisit);
                baseVisitFiBkgdBlank = getBaselineVisitFiValue("fiBackgroundBlank", visitsFIagg, baselinedata, basevisit);

                if (!is.na(baseVisitFiBkgd) & !is.na(baseVisitFiBkgdBlank))
                {
                    for (v in 1:nrow(visitsFIagg))
                    {
                        visit = visitsFIagg$visit[v];
                        if (!compareNumbersForEquality(visit, basevisit, 1e-10))
                        {
                            runDataIndex = rundata$name == visitsFIagg$analyte[v] & rundata$participantID == visitsFIagg$ptid[v] & rundata$visitID == visitsFIagg$visit[v] & rundata$dilution == visitsFIagg$dilution[v];
                            rundata$Positivity[runDataIndex] = determineIndividualPositivityValue(visitsFIagg, v, threshold, baseVisitFiBkgd, baseVisitFiBkgdBlank, foldchange);
                        }
                    }
                }
            }
            else
            {
                for (v in 1:nrow(visitsFIagg))
                {
                    runDataIndex = rundata$name == visitsFIagg$analyte[v] & rundata$participantID == visitsFIagg$ptid[v] & rundata$visitID == visitsFIagg$visit[v] & rundata$dilution == visitsFIagg$dilution[v];
                    rundata$Positivity[runDataIndex] = determineIndividualPositivityValue(visitsFIagg, v, threshold, NA, NA, NA);
                }
            }
        }
    }

    rundata
}

queryPreviousBaselineVisitData <- function(analytedata, ptids, basevisit)
{
    data = data.frame();

    # get list of analytes that are not marked as Negative Controls
    analyteList = toString(paste("'",analytedata$Name,"'", sep=""));
    if (!is.null(analytedata$NegativeControl)) {
        analyteList = toString(paste("'",analytedata$Name[is.na(analytedata$NegativeControl)],"'", sep=""));
    }

    if (!is.na(basevisit) & nchar(analyteList) > 0 & length(ptids) > 0)
    {
        baseUrl = getRunPropertyValue("baseUrl");
        folderPath = getRunPropertyValue("containerPath");
        schemaName = paste("assay.Luminex.",getRunPropertyValue("assayName"), sep="");

        whereClause = paste("FlaggedAsExcluded = false AND Dilution IS NOT NULL ",
                            "AND VisitID=", basevisit,
                            "AND ParticipantID IN (",toString(paste("'",ptids,"'", sep="")),")",
                            "AND Data.Analyte.Name IN (",analyteList,")");

        sql = paste("SELECT Data.Analyte.Name AS Analyte, Data.ParticipantID, Data.VisitID, Data.Dilution, ",
                    "AVG(Data.FIBackground) AS FIBackground, AVG(Data.FIBackgroundBlank) AS FIBackgroundBlank, ",
                    "COUNT(DISTINCT Data.Data) AS DataIdCount ",
                    "FROM Data WHERE ", whereClause,
                    "GROUP BY Analyte.Name, ParticipantID, VisitID, Dilution");

        data = labkey.executeSql(baseUrl=baseUrl, folderPath=folderPath, schemaName=schemaName, sql=sql, colNameOpt="rname");
    }

    data
}

verifyPositivityInputProperties <- function(basevisit, foldchange)
{
    # if there is a baseline visit supplied, make sure the fold change is not null as well
    if (!is.na(basevisit) & is.na(foldchange))
    {
        writeErrorOrWarning("error", "Error: No value provided for 'Positivity Fold Change'.");
    }
}

populatePositivity <- function(rundata, analytedata)
{
    rundata$Positivity = NA;

    # get the run property that are used for the positivity calculation
    calc.positivity = getRunPropertyValue("CalculatePositivity");
    base.visit = getRunPropertyValue("BaseVisit");
    fold.change = getRunPropertyValue("PositivityFoldChange");

    # if calc positivity is true, continue
    if (!is.na(calc.positivity) & calc.positivity == "1")
    {
        verifyPositivityInputProperties(base.visit, fold.change);

        analytePtids = subset(rundata, select=c("name", "participantID")); # note: analyte variable column name is "name"
        analytePtids = unique(analytePtids[!is.na(rundata$participantID),]);

        prevBaselineVisitData = queryPreviousBaselineVisitData(analytedata, unique(analytePtids$participantID), base.visit);

        if (nrow(analytePtids) > 0)
        {
            for (index in 1:nrow(analytePtids))
            {
                rundata <- calculatePositivityForAnalytePtid(rundata, analytedata, prevBaselineVisitData,
                                analytePtids$name[index], analytePtids$participantID[index], base.visit, fold.change);
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
writeLines(paste("LabTransformVersion",labTransformVersion,sep="\t"), fileConn);
close(fileConn);

################################## STEP 2: Positivity Calculation ################################

run.data <- populatePositivity(run.data, analyte.data);

#####################  STEP 3: WRITE THE RESULTS TO THE OUTPUT FILE LOCATION #####################

# write the new set of run data out to an output file
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);

# print the ending time for the transform script
writeLines(paste("\nProcessing end time:",Sys.time(),sep=" "));
