
#
# D(date,value)
#

QC_dot_by_date <- function(D, label="Values")
{
  length = length(D$date)
  seq = 1:length

  xmax= ceiling(length * 1.05)

  # set up the x-axis labels to be the Experiment Dates
# xtcks = 1:length
  dates <- D[,"date"]
  xtcks = (1:length)[1 <= c(1, dates[2:(length-1)]  - dates[1:(length-2)])]
  xlabels <- D$date[xtcks]


  ######## Values plot #########
  # set the margins (bottom, left, top, right)
  par(mar=c(5,5,5,5))

  # set the main title to be enlarged
  par(cex.main=1.5)

  # build the plot area without the data
  plot(NA, NA, type = c("b"),
        ylim=c(0,max(D$value, na.rm = TRUE)), xlim=c(1,xmax),
        ylab="", xlab="",
        axes=F, main=label)
  grid()

  # add the data as lines and points
#  lines(seq, D$value, col='black')
  points(seq, D$value, col='black', pch=16, cex=1.2)

  # set parameter for rotating the axis labels to be perpendicular to axis
  par(las=2)
  # add axes and labels
  axis(2, col="black", cex.axis=1.1)
  axis(1, col="black", at=xtcks, labels=xlabels, cex.axis=0.85)
  box()
}




QC_levey_jennings_by_date <- function(D, image, type="jpeg", width=700, height=700, label="Values")
{
  length = length(D$date)
  seq = 1:length

  # get the mean and standard deviation for all of the data
  m = mean(D$value)
  s = sd(D$value)

  if (!is.finite(s) || !is.finite(m) || m < 0.01)
    return;

  # calculate the 1CV, 2CV, 3CV values for the plot
  s1 = s / m * 100
  s2 = 2 * s / m * 100
  s3 = 3 * s / m * 100
  s4 = 4 * s / m * 100

  # calculate the deviation from mean for each data point as a percentage
  D$diff = (D$value - m) / m * 100

  xmax= ceiling(length * 1.05)

  # set up the x-axis labels to be the Experiment Dates
# xtcks = 1:length
  dates <- D[,"date"]
  xtcks = (1:length)[1 <= c(1, dates[2:(length-1)]  - dates[1:(length-2)])]
  xlabels <- D$date[xtcks]


  ######## Levey-Jennings plot #########
  # set the margins (bottom, left, top, right)
  par(mar=c(5,5,5,5))

  # set the main title to be enlarged
  par(cex.main=1.5)

  # round the mean and cv values for display purposes
  m = round(m, digits=1)
  s1 = round(s1, digits=0)

  # build the plot area without the data, y-limits to be 4CV above and below 0
  plot(NA, NA, type = c("b"), ylim=c(-s4,s4), xlim=c(1,xmax), xlab="",
        ylab="Deviation from mean (%)", axes=F, main=paste("Levey-Jennings Chart","(Mean:", m, "CV:", s1, "%)"))

  # add the horizontal lines for the mean, 1CV, -1CV, 2CV, -2CV, 3CV, and -3CV
  lines(x=c(1,length), y=c(0,0), col="black")
  text(x=xmax, y=0,labels=c("Mean"))

  lines(x=c(1,length), y=c(s1,s1), col="green", lty=2)
  text(x=xmax, y=s1,labels=c("1CV"))
  lines(x=c(1,length), y=c(-s1,-s1), col="green", lty=2)
  text(x=xmax, y=-s1,labels=c("-1CV"))

  lines(x=c(1,length), y=c(s2,s2), col="blue", lty=2)
  text(x=xmax, y=s2,labels=c("2CV"))
  lines(x=c(1,length), y=c(-s2,-s2), col="blue", lty=2)
  text(x=xmax, y=-s2,labels=c("-2CV"))

  lines(x=c(1,length), y=c(s3,s3), col="red", lty=2)
  text(x=xmax, y=s3,labels=c("3CV"))
  lines(x=c(1,length), y=c(-s3,-s3), col="red", lty=2)
  text(x=xmax, y=-s3,labels=c("-3CV"))

  # add the actual data lines and points
#  lines(seq, D$diff, col='black')
  points(seq, D$diff, col='black', pch=16, cex=1.2)

  # set parameter for rotating the axis labels to be perpendicular to axis
  par(las=2)
  # add axes and labels
  axis(2, col="black", cex.axis=1.1)
  axis(1, col="black", at=xtcks, labels=xlabels, cex.axis=0.85)
  box()
}




QC_boxplot_by_date <- function(D, label="Values")
{
  # set the margins (bottom, left, top, right)
  par(mar=c(5,5,5,5))

  # set parameter for rotating the axis labels to be perpendicular to axis
  par(las=2)
  # add axes and labels

#  axis(2, col="black", cex.axis=1.1)
#  axis(1, col="black", cex.axis=0.85)
  boxplot(value ~ date, D, main=label);
}



labkey.data <- labkey.data[order(labkey.data$datetime),]
labkey.data <- labkey.data[is.finite(labkey.data$value),]
length = length(labkey.data$value)

D <- data.frame(
    statistic=labkey.data$statistic,
    date=as.Date(labkey.data$datetime),
    run=labkey.data$run,
    run.href=labkey.data$run_href,
    well=rep("details",length), #well=labkey.data$well,
    well.href=labkey.data$well_href,
    value=labkey.data$value
    )


if (length < 2)
{
    print("too few values to plot")
}
if (length >= 2)
{
    png(filename="${imgout:labkey_png}", width=800, height=600)
#   layout(matrix(c(1,2,3), 3, 1, byrow = TRUE))
    layout(matrix(c(1,2), 2, 1, byrow = TRUE))

    label <- report.parameters$statistic
    QC_dot_by_date(D, label=label)
#    QC_boxplot_by_date(D, label=label)
    QC_levey_jennings_by_date(D)
    dev.off();
}

write.table(D, file = "${tsvout:tsvfile}", sep = "\t", qmethod = "double", col.names=NA)
