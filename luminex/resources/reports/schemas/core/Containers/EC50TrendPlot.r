#
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# R script to create a quality control trending plot for the Luminex assay showing a records EC50, AUC, or High MFI plotted
# with the average and +/- 3 standard deviation range for the record's associated guide run set.
#
# The script expects URL parameters for the Assay Protocol Name and the selected graph parameters (Titration, Analyte, Isotype, and Conjugate)
#
# Author: Cory Nathe, LabKey

# get the data from the server using the Rlabkey selectRows command
library(Rlabkey);
labkey.data <- labkey.selectRows(baseUrl=labkey.url.base,
                            folderPath=labkey.url.path,
                            schemaName="assay",
                            queryName=paste(labkey.url.params$Protocol, "AnalyteTitration", sep=" "),
                            colSelect="Analyte/Name,Titration/Name,Titration/Run/Isotype,Titration/Run/Conjugate,Analyte/Properties/LotNumber,Titration/Run/NotebookNo,Titration/Run/TestDate,GuideSet/Created,Four ParameterCurveFit/EC50,GuideSet/Four ParameterCurveFit/EC50Average,GuideSet/Four ParameterCurveFit/EC50StdDev",
                            colFilter=makeFilter(c("Analyte/Name","EQUAL",labkey.url.params$Analyte),c("Titration/Run/Conjugate","EQUAL",labkey.url.params$Conjugate),c("Titration/Run/Isotype","EQUAL",labkey.url.params$Isotype),c("Titration/Name","EQUAL",labkey.url.params$Titration)),
                            colSort="-Titration/Run/TestDate,-Titration/Run/Created",
                            containerFilter="AllFolders",
                            colNameOpt="rname");

mainTitle = paste(labkey.url.params$Titration, "EC50 for", labkey.url.params$Analyte, "-", labkey.url.params$Isotype, labkey.url.params$Conjugate, sep=" ");

# setup the png for the plot
png(filename="${imgout:ec50trend_png}", width=810, height=290);

# if there is no data for the selection, display a blank plot
if(length(labkey.data$analyte_name) > 0)
{
  # calculate the guide set ranges for each of the data points
  labkey.data$guidesetplus1stddev = labkey.data$guideset_four_parametercurvefit_ec50average + (1 * labkey.data$guideset_four_parametercurvefit_ec50stddev);
  labkey.data$guidesetplus2stddev = labkey.data$guideset_four_parametercurvefit_ec50average + (2 * labkey.data$guideset_four_parametercurvefit_ec50stddev);
  labkey.data$guidesetplus3stddev = labkey.data$guideset_four_parametercurvefit_ec50average + (3 * labkey.data$guideset_four_parametercurvefit_ec50stddev);
  labkey.data$guidesetminus1stddev = labkey.data$guideset_four_parametercurvefit_ec50average - (1 * labkey.data$guideset_four_parametercurvefit_ec50stddev);
  labkey.data$guidesetminus2stddev = labkey.data$guideset_four_parametercurvefit_ec50average - (2 * labkey.data$guideset_four_parametercurvefit_ec50stddev);
  labkey.data$guidesetminus3stddev = labkey.data$guideset_four_parametercurvefit_ec50average - (3 * labkey.data$guideset_four_parametercurvefit_ec50stddev);

  # get the y axis min and max based on the data
  ymin = min(labkey.data$four_parametercurvefit_ec50, na.rm=TRUE);
  if (min(labkey.data$guidesetminus3stddev, na.rm=TRUE) < ymin)
	ymin = min(labkey.data$guidesetminus3stddev, na.rm=TRUE);
  ymax = max(labkey.data$four_parametercurvefit_ec50, na.rm=TRUE);
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
  par(mar=c(6,4.5,2,0.2));
  plot(NA, NA, type = c("b"), ylim=c(ymin,ymax), xlim=c(1,xmax), xlab="", ylab="", axes=F, main=mainTitle);

  # draw the guide set ranges for each of the records
  for (i in 1:length(labkey.data$analyte_name))
  {
  	# draw a vertial line to connect the min and max of the range
  	lines(c(labkey.data$seq[i], labkey.data$seq[i]), c(labkey.data$guidesetplus3stddev[i], labkey.data$guidesetminus3stddev[i]), col='grey60', lty='solid');

  	# draw dotted lines for the guide set ranges (3 stdDev above average)
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus1stddev[i], labkey.data$guidesetplus1stddev[i]), col='green', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus2stddev[i], labkey.data$guidesetplus2stddev[i]), col='blue', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetplus3stddev[i], labkey.data$guidesetplus3stddev[i]), col='red', lty='dotted');

  	# draw dotted lines for the guide set ranges (3 stdDev below average)
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus1stddev[i], labkey.data$guidesetminus1stddev[i]), col='green', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus2stddev[i], labkey.data$guidesetminus2stddev[i]), col='blue', lty='dotted');
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guidesetminus3stddev[i], labkey.data$guidesetminus3stddev[i]), col='red', lty='dotted');

  	# draw a solid line at the guide set average
  	lines(c(labkey.data$seq[i] - 0.3, labkey.data$seq[i] + 0.3), c(labkey.data$guideset_four_parametercurvefit_ec50average[i], labkey.data$guideset_four_parametercurvefit_ec50average[i]), col='grey60', lty='solid');
  }

  # draw points for the EC50 values for each record
  points(labkey.data$seq, labkey.data$four_parametercurvefit_ec50, col='black', pch=15, cex=1.3);

  # add the axis labels and tick marks
  par(las=2);
  axis(2, col="black", cex.axis=1);
  axis(1, col="black", at=xtcks, labels=xlabels, cex.axis=1);
  box();

} else {
  par(mar=c(6,5,2,0.2));
  plot(NA, NA, type = c("b"), ylim=c(0,1), xlim=c(1,30), xlab="", ylab="", axes=F, main=mainTitle);
  text(15,0.5,"No Data Available for Selected Graph Parameters");
  axis(1, at=seq(0,30,by=5), labels=matrix("",1,7));
  box();
}

# close the graphing device
dev.off();


