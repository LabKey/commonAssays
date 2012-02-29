#
# Copyright (c) 2011-2012 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# R script to create a quality control trending plot for the Luminex assay showing a records EC50, AUC, or Max MFI plotted
# with the average and +/- 3 standard deviation range for the record's associated guide run set.
#
# The script expects URL parameters for the Assay Protocol Name and the selected graph parameters (Titration, Analyte, Isotype, and Conjugate)
#
# Author: Cory Nathe, LabKey

# load R libraries
library(Rlabkey, quietly=TRUE);
library(Cairo, quietly=TRUE);
library(plotrix, quietly=TRUE);

# verify that the correct version of Rlabkey is installed
rlabkeyVersion = installed.packages()["Rlabkey","Version"];
majorVersion = as.numeric(substr(rlabkeyVersion, 0, 3));
minorVersion = substr(rlabkeyVersion, 5, nchar(rlabkeyVersion));
if (majorVersion <= 2.1) {
    if (nchar(minorVersion) > 0 & as.numeric(minorVersion) < 117) {
        stop(paste("this report requires that version 2.1.117 or greater of Rlabkey be installed\nCurrent version installed: Rlabkey_",rlabkeyVersion,sep=""));
    }
}

plotTypes = c("EC50 4PL", "EC50 5PL", "AUC", "High MFI");

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
                "Titration/Run/NotebookNo", "Analyte/Data/AcquisitionDate", "GuideSet/Created", sep=",");

# get the columns needed for each of the 4 plot types : EC50 4PL, EC50 5PL, MaxFI, and AUC
colSelect = paste(colSelect, "Four ParameterCurveFit/EC50", "GuideSet/Four ParameterCurveFit/EC50Average", "GuideSet/Four ParameterCurveFit/EC50StdDev", sep=",");
colSelect = paste(colSelect, "Five ParameterCurveFit/EC50", "GuideSet/Five ParameterCurveFit/EC50Average", "GuideSet/Five ParameterCurveFit/EC50StdDev", sep=",");
colSelect = paste(colSelect, "MaxFI", "GuideSet/MaxFIAverage", "GuideSet/MaxFIStdDev", sep=",");
colSelect = paste(colSelect, "TrapezoidalCurveFit/AUC", "GuideSet/TrapezoidalCurveFit/AUCAverage", "GuideSet/TrapezoidalCurveFit/AUCStdDev", sep=",");

# either filter on start and end date or on max number of rows
maxRows = NA;
if (!is.null(labkey.url.params$MaxRows)) {
	maxRows = labkey.url.params$MaxRows;
} else {
	colFilter=rbind(colFilter,makeFilter(c("Analyte/Data/AcquisitionDate","DATE_GREATER_THAN_OR_EQUAL",labkey.url.params$StartDate)));
	colFilter=rbind(colFilter,makeFilter(c("Analyte/Data/AcquisitionDate","DATE_LESS_THAN_OR_EQUAL",labkey.url.params$EndDate)));
}

# call the selectRows function to get the data from the server
labkey.data <- labkey.selectRows(baseUrl=labkey.url.base,
                            folderPath=labkey.url.path,
                            schemaName="assay",
                            queryName=paste(labkey.url.params$Protocol, "AnalyteTitration", sep=" "),
                            colSelect=colSelect,
                            colFilter=colFilter,
                            colSort="-Analyte/Data/AcquisitionDate,-Titration/Run/Created",
                            containerFilter="AllFolders",
                            colNameOpt="rname",
                            maxRows=maxRows);

# setup the png or pdf for the plot
if (!is.null(labkey.url.params$PdfOut)) {
    pdf(file="${pdfout:Levey-Jennings Trend Plot}", width=10, height=6);
} else {
    CairoPNG(filename="${imgout:Levey-Jennings Trend Plot}", width=810, height=300 * length(plotTypes));
    layout(matrix(1:length(plotTypes), length(plotTypes), 1));
    par(cex=1);
}

for (typeIndex in 1:length(plotTypes))
{
	plotType = plotTypes[typeIndex];
	dat = labkey.data;

	mainTitle = paste(labkey.url.params$Titration, plotType, "for", labkey.url.params$Analyte, sep=" ");
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
	if (plotType == "EC50 4PL") {
	    dat$plottype_value = dat$four_parametercurvefit_ec50;
	    dat$guideset_average = dat$guideset_four_parametercurvefit_ec50average;
	    dat$guideset_stddev = dat$guideset_four_parametercurvefit_ec50stddev;
	} else if (plotType == "EC50 5PL") {
	    dat$plottype_value = dat$five_parametercurvefit_ec50;
	    dat$guideset_average = dat$guideset_five_parametercurvefit_ec50average;
	    dat$guideset_stddev = dat$guideset_five_parametercurvefit_ec50stddev;
	} else if (plotType == "High MFI") {
	    dat$plottype_value = dat$maxfi;
	    dat$guideset_average = dat$guideset_maxfiaverage;
	    dat$guideset_stddev = dat$guideset_maxfistddev;
	} else if (plotType == "AUC") {
	    dat$plottype_value = dat$trapezoidalcurvefit_auc;
	    dat$guideset_average = dat$guideset_trapezoidalcurvefit_aucaverage;
	    dat$guideset_stddev = dat$guideset_trapezoidalcurvefit_aucstddev;
	}

	# determine if the request is for log scale or not
	asLog = "";
	yAxisLabel = plotType;
	if (!is.null(labkey.url.params$AsLog)) {
	    asLog = "y";
	    yAxisLabel = paste(yAxisLabel, "(log)", sep=" ");
	}

	# if there is no data for the selection, display a blank plot
	if(length(dat$analyte_name) > 0)
	{
	  # calculate the guide set ranges for each of the data points
	  dat$guidesetplus1stddev = dat$guideset_average + (1 * dat$guideset_stddev);
	  dat$guidesetplus2stddev = dat$guideset_average + (2 * dat$guideset_stddev);
	  dat$guidesetplus3stddev = dat$guideset_average + (3 * dat$guideset_stddev);
	  dat$guidesetminus1stddev = dat$guideset_average - (1 * dat$guideset_stddev);
	  dat$guidesetminus2stddev = dat$guideset_average - (2 * dat$guideset_stddev);
	  dat$guidesetminus3stddev = dat$guideset_average - (3 * dat$guideset_stddev);

	  # get the y axis min and max based on the data
	  if (any(!is.na(dat$plottype_value))) {
	      ymin = min(dat$plottype_value, na.rm=TRUE);
	      if (min(dat$guidesetminus3stddev, na.rm=TRUE) < ymin)
		    ymin = min(dat$guidesetminus3stddev, na.rm=TRUE);
	      ymax = max(dat$plottype_value, na.rm=TRUE);
	      if (max(dat$guidesetplus3stddev, na.rm=TRUE) > ymax)
            ymax = max(dat$guidesetplus3stddev, na.rm=TRUE);
	  } else {
	      ymin = 0;
	      ymax= 1;
	  }

	  # if the plot is in log scale, make sure we don't have values <= 0
	  if (asLog == "y") {
	      if (ymin <= 0) { ymin = 1; }
	      dat$guidesetplus3stddev[dat$guidesetplus3stddev <= 0] = 1;
          dat$guidesetminus3stddev[dat$guidesetminus3stddev <= 0] = 1;
	  }

	  # set the sequence value for the records (in reverse order since they are sorted in DESC order)
	  dat$seq = length(dat$analyte_name):1;
	  dat = dat[order(dat$seq),];

	  # determine the x-axis max
	  xmax= 10;
	  if(length(dat$analyte_name) > xmax)
	     xmax = length(dat$analyte_name);

	  # get the scaling factor to determine how many tick marks to show
	  tckFactor = ceiling(xmax/30);
	  # setup the tick marks and labels based on the scaling factor
	  xtcks = seq(1, xmax, by = tckFactor);
	  xlabels = as.character(dat$titration_run_notebookno[xtcks]);

	  # set the point colors, giving each unique lot numer (if more than one) a different color
	  dat$ptcolor = 1;
	  lotnums = unique(as.character(dat$analyte_properties_lotnumber));
	  showLegend = length(lotnums) > 1;
	  if (length(lotnums) > 1) {
	    for (i in 1:length(lotnums)) {
	        if (is.na(lotnums[i])) {
	            dat$ptcolor[is.na(dat$analyte_properties_lotnumber)] = i+1;
	        } else {
	            dat$ptcolor[as.character(dat$analyte_properties_lotnumber) == lotnums[i]] = i+1;
            }
        }
	  }

	  # set some parameters and variables based on whether or not a legend is to be shown
	  par(mar=c(5.5,5,2,0.2));
	  mainTitleLine = 0.75;
	  if (showLegend) {
	    par(mar=par()$mar+c(0,0,2,0));
	    mainTitleLine = 2.75;
	  }

	  # create an empty plotting area with a title
	  plot(NA, NA, type = c("b"), ylim=c(ymin,ymax), xlim=c(1,xmax), xlab="", ylab="", axes=F, log=asLog);
	  mtext(mainTitle, side=3, line=mainTitleLine, font=2, las=1, cex=1.2); 

	  # if creating a pdf, increase the line width and layout position offset
	  yLegendOffset = -0.5;
	  if (!is.null(labkey.url.params$PdfOut)) {
	    par(lwd=1.5);
	    yLegendOffset = -0.75;
	  }

	  # draw the guide set ranges for each of the records
	  for (i in 1:length(dat$analyte_name))
	  {
		# draw a vertial line to connect the min and max of the range
		lines(c(dat$seq[i], dat$seq[i]), c(dat$guidesetplus3stddev[i], dat$guidesetminus3stddev[i]), col='grey60', lty='solid');

		# draw dotted lines for the guide set ranges (3 stdDev above average)
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetplus1stddev[i], dat$guidesetplus1stddev[i]), col='darkgreen', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetplus2stddev[i], dat$guidesetplus2stddev[i]), col='blue', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetplus3stddev[i], dat$guidesetplus3stddev[i]), col='red', lty='dotted');

		# draw dotted lines for the guide set ranges (3 stdDev below average)
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetminus1stddev[i], dat$guidesetminus1stddev[i]), col='darkgreen', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetminus2stddev[i], dat$guidesetminus2stddev[i]), col='blue', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guidesetminus3stddev[i], dat$guidesetminus3stddev[i]), col='red', lty='dotted');

		# draw a solid line at the guide set average
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$guideset_average[i], dat$guideset_average[i]), col='grey60', lty='solid');
	  }

	  # draw points for the trend values for each record
	  points(dat$seq, dat$plottype_value, col=dat$ptcolor, pch=15);

	  # add the legend if there are > 1 unique lot numbers
	  if (showLegend) {
	    legend(xmax/2 + 0.5, ymax, legend=lotnums, fill=2:(length(lotnums)+1), bg="white", cex=0.8,
	        ncol=length(lotnums), xjust=0.5, yjust=yLegendOffset, xpd=T);
      }

	  # add the axis labels and tick marks
	  par(las=2);
	  axis(2, col="black");
	  mtext(yAxisLabel, side=2, line=4, las=0, font=2);
	  axis(1, col="black", at=xtcks, labels=FALSE, cex.axis=0.8);
	  staxlab(1, xtcks, xlabels, srt=25)
	  mtext("Assay", side=1, line=4, las=0, font=2);	  
	  box();

	} else {
	  par(mar=c(5.5,5,2,0.2));
	  plot(NA, NA, type = c("b"), ylim=c(1,1), xlim=c(1,30), xlab="", ylab="", axes=F, log=asLog);
	  mtext(mainTitle, side=3, line=0.75, font=2, las=1, cex=1.2);
	  text(15,1,"No Data Available for Selected Graph Parameters");
	  axis(1, at=seq(0,30,by=5), labels=matrix("",1,7), cex.axis=0.8);
	  mtext(yAxisLabel, side=2, line=4, las=0, font=2);
	  mtext("Assay", side=1, line=4, las=0, font=2);
	  box();
	}
}

# close the graphing device
dev.off();


