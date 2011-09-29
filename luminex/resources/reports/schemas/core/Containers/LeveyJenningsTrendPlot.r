#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# R script to create a quality control trending plot for the Luminex assay showing a records EC50, AUC, or Max MFI plotted
# with the average and +/- 3 standard deviation range for the record's associated guide run set.
#
# The script expects URL parameters for the Assay Protocol Name and the selected graph parameters (Titration, Analyte, Isotype, and Conjugate)
#
# Author: Cory Nathe, LabKey

# get the data from the server using the Rlabkey selectRows command
library(Rlabkey);

# create a list of filters to apply to the selectRows call
colFilter=makeFilter(c("Analyte/Name","EQUAL",labkey.url.params$Analyte));
colFilter=rbind(colFilter,makeFilter(c("Titration/Name","EQUAL",labkey.url.params$Titration)));
if (labkey.url.params$Conjugate == "") {
    colFilter=rbind(colFilter,makeFilter(c("Titration/Run/Conjugate","MISSING",labkey.url.params$Conjugate)));
} else {
    colFilter=rbind(colFilter,makeFilter(c("Titration/Run/Conjugate","EQUAL",labkey.url.params$Conjugate)));
}
if (labkey.url.params$Isotype == "") {
    colFilter=rbind(colFilter,makeFilter(c("Titration/Run/Isotype","MISSING",labkey.url.params$Isotype)));
} else {
    colFilter=rbind(colFilter,makeFilter(c("Titration/Run/Isotype","EQUAL",labkey.url.params$Isotype)));
}

# create a list of the columns that are needed for the trending plot
colSelect = paste("Analyte/Name", "Titration/Name", "Titration/Run/Isotype", "Titration/Run/Conjugate", "Analyte/Properties/LotNumber",
                "Titration/Run/NotebookNo", "Titration/Run/TestDate", "GuideSet/Created", sep=",");
if (labkey.url.params$PlotType == "EC50") {
    colSelect = paste(colSelect, "Four ParameterCurveFit/EC50", "GuideSet/Four ParameterCurveFit/EC50Average", "GuideSet/Four ParameterCurveFit/EC50StdDev", sep=","); 
} else if (labkey.url.params$PlotType == "MaxMFI") {
    colSelect = paste(colSelect, "MaxFI", "GuideSet/MaxFIAverage", "GuideSet/MaxFIStdDev", sep=",");
} else if (labkey.url.params$PlotType == "AUC") {
    colSelect = paste(colSelect, "TrapezoidalCurveFit/AUC", "GuideSet/TrapezoidalCurveFit/AUCAverage", "GuideSet/TrapezoidalCurveFit/AUCStdDev", sep=",");
}

# either filter on start and end date or on max number of rows
maxRows = NA;
if (!is.null(labkey.url.params$MaxRows)) {
	maxRows = labkey.url.params$MaxRows;
} else {
	colFilter=rbind(colFilter,makeFilter(c("Titration/Run/TestDate","GREATER_THAN_OR_EQUAL_TO",labkey.url.params$StartDate)));
	colFilter=rbind(colFilter,makeFilter(c("Titration/Run/TestDate","LESS_THAN_OR_EQUAL_TO",labkey.url.params$EndDate)));
}

# call the selectRows function to get the data from the server
labkey.data <- labkey.selectRows(baseUrl=labkey.url.base,
                            folderPath=labkey.url.path,
                            schemaName="assay",
                            queryName=paste(labkey.url.params$Protocol, "AnalyteTitration", sep=" "),
                            colSelect=colSelect,
                            colFilter=colFilter,
                            colSort="-Titration/Run/TestDate,-Titration/Run/Created",
                            containerFilter="AllFolders",
                            colNameOpt="rname",
                            maxRows=maxRows);

mainTitle = paste(labkey.url.params$Titration, labkey.url.params$PlotType, "for", labkey.url.params$Analyte, sep=" ");
if (labkey.url.params$Isotype == "") {
    mainTitle = paste(mainTitle, "- [None]", sep=" ");
} else {
    mainTitle = paste(mainTitle, "-", labkey.url.params$Isotype, sep=" ");
}
if (labkey.url.params$Conjugate == "") {
    mainTitle = paste(mainTitle, "[None]", sep=" ");
} else {
    mainTitle = paste(mainTitle, labkey.url.params$Conjugate, sep=" ");
}

# setup the data frame based on the selected plot type
if (labkey.url.params$PlotType == "EC50") {
    labkey.data$plottype_value = labkey.data$four_parametercurvefit_ec50;
    labkey.data$guideset_average = labkey.data$guideset_four_parametercurvefit_ec50average;
    labkey.data$guideset_stddev = labkey.data$guideset_four_parametercurvefit_ec50stddev;
} else if (labkey.url.params$PlotType == "MaxMFI") {
    labkey.data$plottype_value = labkey.data$maxfi;
    labkey.data$guideset_average = labkey.data$guideset_maxfiaverage;
    labkey.data$guideset_stddev = labkey.data$guideset_maxfistddev;
} else if (labkey.url.params$PlotType == "AUC") {
    labkey.data$plottype_value = labkey.data$trapezoidalcurvefit_auc;
    labkey.data$guideset_average = labkey.data$guideset_trapezoidalcurvefit_aucaverage;
    labkey.data$guideset_stddev = labkey.data$guideset_trapezoidalcurvefit_aucstddev;
}

# setup the png or pdf for the plot
if (!is.null(labkey.url.params$PdfOut)) {
    pdf(file="${pdfout:levey_jennings_trend}", width=10, height=6);
} else {
    png(filename="${imgout:levey_jennings_trend}", width=810, height=265);
}

# if there is no data for the selection, display a blank plot
if(length(labkey.data$analyte_name) > 0)
{
  # calculate the guide set ranges for each of the data points
  labkey.data$guidesetplus1stddev = labkey.data$guideset_average + (1 * labkey.data$guideset_stddev);
  labkey.data$guidesetplus2stddev = labkey.data$guideset_average + (2 * labkey.data$guideset_stddev);
  labkey.data$guidesetplus3stddev = labkey.data$guideset_average + (3 * labkey.data$guideset_stddev);
  labkey.data$guidesetminus1stddev = labkey.data$guideset_average - (1 * labkey.data$guideset_stddev);
  labkey.data$guidesetminus2stddev = labkey.data$guideset_average - (2 * labkey.data$guideset_stddev);
  labkey.data$guidesetminus3stddev = labkey.data$guideset_average - (3 * labkey.data$guideset_stddev);

  # get the y axis min and max based on the data
  ymin = min(labkey.data$plottype_value, na.rm=TRUE);
  if (min(labkey.data$guidesetminus3stddev, na.rm=TRUE) < ymin)
	ymin = min(labkey.data$guidesetminus3stddev, na.rm=TRUE);
  ymax = max(labkey.data$plottype_value, na.rm=TRUE);
  if (max(labkey.data$guidesetplus3stddev, na.rm=TRUE) > ymax)
  	ymax = max(labkey.data$guidesetplus3stddev, na.rm=TRUE);

  # set the sequence value for the records (in reverse order since they are sorted in DESC order)
  labkey.data$seq = length(labkey.data$analyte_name):1;
  labkey.data = labkey.data[order(labkey.data$seq),];

  # determine the x-axis max
  xmax= 10;
  if(length(labkey.data$analyte_name) > xmax)
     xmax = length(labkey.data$analyte_name);

  # get the scaling factor to determine how many tick marks to show
  tckFactor = ceiling(xmax/30);
  # setup the tick marks and labels based on the scaling factor
  xtcks = seq(1, xmax, by = tckFactor);
  xlabels = as.character(labkey.data$titration_run_notebookno[xtcks]);

  # create an empty plotting area with the correct margins
  par(mar=c(6.5,5,2,0.2));
  plot(NA, NA, type = c("b"), ylim=c(ymin,ymax), xlim=c(1,xmax), xlab="", ylab="", axes=F, main=mainTitle);

  # if creating a pdf, increase the line width
  if (!is.null(labkey.url.params$PdfOut)) {
    par(lwd=1.5);
  }

  # draw the guide set ranges for each of the records
  for (i in 1:length(labkey.data$analyte_name))
  {
  	# draw a vertial line to connect the min and max of the range
  	lines(c(labkey.data$seq[i], labkey.data$seq[i]), c(labkey.data$guidesetplus3stddev[i], labkey.data$guidesetminus3stddev[i]), col='grey60', lty='solid');

  	# draw dotted lines for the guide set ranges (3 stdDev above average)
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus1stddev[i], labkey.data$guidesetplus1stddev[i]), col='darkgreen', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus2stddev[i], labkey.data$guidesetplus2stddev[i]), col='blue', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus3stddev[i], labkey.data$guidesetplus3stddev[i]), col='red', lty='dotted');

  	# draw dotted lines for the guide set ranges (3 stdDev below average)
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus1stddev[i], labkey.data$guidesetminus1stddev[i]), col='darkgreen', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus2stddev[i], labkey.data$guidesetminus2stddev[i]), col='blue', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus3stddev[i], labkey.data$guidesetminus3stddev[i]), col='red', lty='dotted');

  	# draw a solid line at the guide set average
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guideset_average[i], labkey.data$guideset_average[i]), col='grey60', lty='solid');
  }

  # draw points for the trend values for each record
  points(labkey.data$seq, labkey.data$plottype_value, col='black', pch=15, cex=1.3);

  # add the axis labels and tick marks
  par(las=2);
  axis(2, col="black", cex.axis=1);
  mtext(labkey.url.params$PlotType, side=2, line=4, las=0);
  axis(1, col="black", at=xtcks, labels=xlabels, cex.axis=1);
  mtext("Assay", side=1, line=5.25, las=0);
  box();

} else {
  par(mar=c(6.5,5,2,0.2));
  plot(NA, NA, type = c("b"), ylim=c(0,1), xlim=c(1,30), xlab="", ylab="", axes=F, main=mainTitle);
  text(15,0.5,"No Data Available for Selected Graph Parameters");
  axis(1, at=seq(0,30,by=5), labels=matrix("",1,7));
  mtext(labkey.url.params$PlotType, side=2, line=4, las=0);
  mtext("Assay", side=1, line=5.25, las=0);
  box();
}

# close the graphing device
dev.off();


