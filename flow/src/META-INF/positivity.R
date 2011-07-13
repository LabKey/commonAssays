##
#  Copyright (c) 2011 Fred Hutchinson Cancer Research Center
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##

# Original positivity script by Alan DeCamp <decamp@scharp.org>

# Define constants
NSUB_MIN = 5000
ALPHA    = 0.00001

positivity <- function (data, grouping_columns)
{
    # generate raw p-values
    data$raw_p = sapply(1:nrow(data), function(i) {
      pval = NA

      if (is.numeric(data$stat[i]) && is.numeric(data$stat_bg[i]) && is.numeric(data$parent[i]) && is.numeric(data$parent_bg[i]))
      {
        x.ant = as.integer(data$stat[i])
        N.ant = as.integer(data$parent[i] - data$stat[i])
        x.neg = as.integer(data$stat_bg[i])
        N.neg = as.integer(data$parent_bg[i] - data$stat_bg[i])

        m = matrix(c(x.ant,N.ant,x.neg,N.neg), nc=2, byrow=FALSE)

        pval = fisher.test(m, alternative="greater")$p.value
      }

      return(pval)
    })

    # compute adjusted p-values
    data = by(data, subset(data, select=grouping_columns), function(ss) {
      #ss$adj_p = p.adjust(ss$raw_p, method="holm")
      ss$adj_p = p.adjust(ss$raw_p, method="bonferroni")
      return(ss)
    })
    data = do.call(rbind, data)

    # define response call
    data$response = as.numeric(data$adj_p <= ALPHA)

    return(data)
}

if (length(labkey.data$stat) == 0)
    stop("labkey.data is empty");

if (length(labkey.data$stat) == 0 || length(labkey.data$stat_bg) == 0 || length(labkey.data$parent) == 0 || length(labkey.data$parent_bg) == 0)
    stop("labkey.data$stat, labkey.data$stat_bg, labkey.data$parent, labkey.data$paarent_bg are required");

if (!exists("flow.metadata.study.participantColumn"))
    stop("ICS study metadata must include participant column")

if (!exists("flow.metadata.study.visitColumn") && !exists("flow.metadata.study.dateColumn"))
    stop("ICS study metadata must include either visit or date column")

if (exists("flow.metadata.study.visitColumn")) {
    grouping_cols = c(flow.metadata.study.participantColumn, flow.metadata.study.visitColumn)
} else {
    grouping_cols = c(flow.metadata.study.participantColumn, flow.metadata.study.dateColumn)
}

result <- positivity(labkey.data, grouping_cols)

write.table(result, file = "${tsvout:FCSAnalyses}", sep = "\t", qmethod = "double", col.names=NA)

#PRINT <- data.frame(
#    date = as.Date(result$datetime),
#    run = result$run,
#    run.href = result$run_href,
#    well = result$well,
#    well.href = result$well_href
#)
#
#PRINT[flow.metadata.study.participantColumn] = result[flow.metadata.study.participantColumn]
#if (exists("flow.metadata.study.visitColumn")) {
#    PRINT[flow.metadata.study.visitColumn] = result[flow.metadata.study.visitColumn]
#} else {
#    PRINT[flow.metadata.study.dateColumn] = result[flow.metadata.study.dateColumn]
#}
#
#PRINT[report.parameters$subsetParentDisplay] = result$parent
#PRINT[paste("BG", report.parameters$subsetParentDisplay)] = result$parent_bg
#PRINT[report.parameters$subsetDisplay] = result$stat
#PRINT[paste("BG", report.parameters$subsetDisplay)] = result$stat_bg
#PRINT$response = result$response
#PRINT$raw_p = result$raw_p
#PRINT$adj_p = result$adj_p
#
#write.table(PRINT, file = "{tsvout:tsvfile?FlowTableType=FCSAnalyses}", sep = "\t", qmethod = "double", col.names=NA)

