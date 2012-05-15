#
# Copyright (c) 2011-2012 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# R script to create a plot showing the curve fits for selected runs overlapping on the same plot.
#
# The script expects URL parameters for the Assay Protocol Name and the selected run IDs.
#
# Author: Cory Nathe, LabKey

# load R libraries
library(Rlabkey, quietly=TRUE);
library(Cairo, quietly=TRUE);

# fail if any of the required filter params are missing
if (is.null(labkey.url.params$RunIds)){
    stop("No Run IDs Provided.")
}
if (is.null(labkey.url.params$Protocol)){
    stop("No Protocol Provided.")
}
if (is.null(labkey.url.params$Analyte)){
    stop("No Analyte Provided.")
}
if (is.null(labkey.url.params$Titration)){
    stop("No Titration Provided.")
}

# create a list of the columns that are needed for the trending plot
colSelect = paste("Data/Run", "Data/Run/NotebookNo", "Analyte/Name", "Titration/Name", "Type", "Data/Run/StndCurveFitInput",
                "FI", "FIBackground", "fiBackgroundBlank", "Dilution", "ExpConc",
                "AnalyteTitration/Four ParameterCurveFit/MinAsymptote", "AnalyteTitration/Four ParameterCurveFit/MaxAsymptote",
                "AnalyteTitration/Four ParameterCurveFit/Inflection", "AnalyteTitration/Four ParameterCurveFit/Slope", sep=",");

# create a list of columns sort on
colSort = paste("Data/Run/RowId", "Analyte/Name", "Dilution", "ExpConc", sep=",");

# create the filter for the list of Run IDs, analyte, and titration
colFilter = makeFilter(c("Data/Run/RowId","EQUALS_ONE_OF",labkey.url.params$RunIds));
colFilter=rbind(colFilter,makeFilter(c("Analyte/Name","EQUAL",labkey.url.params$Analyte)));
colFilter=rbind(colFilter,makeFilter(c("Titration/Name","EQUAL",labkey.url.params$Titration)));

# call the selectRows function to get the data from the server
labkey.data <- labkey.selectRows(baseUrl=labkey.url.base,
                            folderPath=labkey.url.path,
                            schemaName="assay",
                            queryName=paste(labkey.url.params$Protocol, "Data", sep=" "),
                            colSelect=colSelect,
                            colFilter=colFilter,
                            colSort=colSort,
                            containerFilter="AllFolders",
                            colNameOpt="rname");

# get the list of run names/notebooknumbers, run data, and fit params
runs = unique(subset(labkey.data, select=c("data_run", "data_run_notebookno")));
runsParams = unique(subset(labkey.data, select=c("data_run", "analytetitration_four_parametercurvefit_minasymptote",
		"analytetitration_four_parametercurvefit_maxasymptote", "analytetitration_four_parametercurvefit_slope",
		"analytetitration_four_parametercurvefit_inflection")));

# process the data to set the proper FI and Dose values
runsData = subset(labkey.data, select=c("data_run", "data_run_notebookno", "data_run_stndcurvefitinput", "type"));
runsData$fi = NA;
runsData$dose = NA;
hasDilutions = FALSE;
hasExpConcs = FALSE;
for (index in 1:nrow(runs))
{
    runData = subset(runsData, data_run == runs$data_run[index]);
    runIndices = runsData$data_run == runs$data_run[index];

    if (nrow(runData) > 0)
    {
        # set the y-axis values (fi) based on the StndCurveFitInput run prop (default: fiBackground)
        fiCol = runData$data_run_stndcurvefitinput[1];
        runsData$fi[runIndices] = labkey.data$fibackground[runIndices];
        if (fiCol == "FI") {
            runsData$fi[runIndices] = labkey.data$fi[runIndices];
        } else if (fiCol == "FI-Bkgd-Blank") {
            runsData$fi[runIndices] = labkey.data$fibackgroundblank[runIndices];
        }

        # set the x-axis values (dose) based on the titration type (default: dilution)
        if ((toupper(substr(runData$type[1],0,1)) == "S" || toupper(substr(runData$type[1],0,2)) == "ES")) {
            runsData$dose[runIndices] = labkey.data$expconc[runIndices];
            hasExpConcs = TRUE;
        }
        else {
            runsData$dose[runIndices] = labkey.data$dilution[runIndices];
            hasDilutions = TRUE;
        }
    }
}

# get the axis min and max values for plotting
xmin = min(runsData$dose);
xmax = max(runsData$dose);
ymin = min(runsData$fi);
ymax = max(runsData$fi);

# get the axis labels based on the data
yLabel = unique(labkey.data$data_run_stndcurvefitinput);
xLabel = NA;
if (hasDilutions & hasExpConcs) {
    xLabel = "Dilution / ExpConc";
} else if (hasDilutions) {
    xLabel = "Dilution";
} else if (hasExpConcs) {
    xLabel = "Expected Concentration";
}

# curve type variations on color, shape, and line type
colors = rep(1:5, 6);
shapes = rep(c(rep(1, 5), rep(4, 5), rep(3, 5)), 2);
lineTypes = c(rep(1, 15), rep(5, 15));
curveTypes = data.frame(col = colors, pch = shapes, lty = lineTypes);

# initialize the plot
CairoPNG(filename="${imgout:Overlapping Curves Plot}", width=650, height=500);
par(mar=c(5, 4, 4, 10) + 0.1);
plot(NA, NA, main=labkey.url.params$MainTitle, ylab = yLabel, xlab = xLabel, xlim=c(xmin, xmax), ylim=c(ymin, ymax), log="x"); 

# loop through the selected runs to plot the curve and the data points
for (index in 1:nrow(runs))
{
	runData = subset(runsData, data_run == runs$data_run[index]);

    # get the 4PL fit params for this run
	runParams = subset(runsParams, data_run == runs$data_run[index]);
	minA = runParams$analytetitration_four_parametercurvefit_minasymptote;
	maxA = runParams$analytetitration_four_parametercurvefit_maxasymptote;
	infP = runParams$analytetitration_four_parametercurvefit_inflection;
	asymm = 1;
	slope = runParams$analytetitration_four_parametercurvefit_slope;

    # plot the curve based on the fit parameters
	curve(minA + (maxA/(1+(x/infP)^slope)^asymm), add=TRUE, col=curveTypes$col[index %% nrow(curveTypes)], lty=curveTypes$lty[index %% nrow(curveTypes)]);

	# plot the data points associated with the curve
	points(runData$dose, runData$fi, col=curveTypes$col[index %% nrow(curveTypes)], pch=curveTypes$pch[index %% nrow(curveTypes)]);
}

legend(exp(log(xmax) + 1), (ymax - ymin) / 2, legend=runs$data_run_notebookno, pch=curveTypes$pch, col=curveTypes$col, lty=curveTypes$lty,
        yjust=0.5, bty="n", xpd=T);

dev.off();