##
#  Copyright (c) 2009-2010 LabKey Corporation
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

      if (!is.na(data$stat[i]) && !is.na(data$stat_bg[i]) && !is.na(data$parent[i]) && !is.na(data$parent_bg[i]))
      {
        x.ant = data$stat[i]
        N.ant = data$parent[i] - data$stat[i]
        x.neg = data$stat_bg[i]
        N.neg = data$parent_bg[i] - data$stat_bg[i]

        m = matrix(c(x.ant,N.ant,x.neg,N.neg), nc=2, byrow=FALSE)

        pval = fisher.test(m, alternative="greater")$p.value
      }

      return(pval)
    })


    # compute adjusted p-values
    data = by(data, subset(data, select=grouping_columns), function(ss) {
      ss$adj_p = p.adjust(ss$raw_p, method="holm")
      return(ss)
    })
    dat = do.call(rbind, data)

    # define response call
    dat$response = as.numeric(dat$adj_p <= ALPHA)

    return(dat)
}

# UNDONE: check labkey.data has statistic, background statistic, parent statistic, background parent statistic

result <- positivity(labkey.data, flow.metadata.matchColumns)

PRINT <- data.frame(
    date = as.Date(result$datetime),
    run = result$run,
    run.href = result$run_href,
    well = result$well,
    well.href = result$well_href
)

PRINT[report.parameters$subsetDisplay] = result$stat
PRINT[paste("BG ", report.parameters$subsetDisplay)] = result$stat_bg
PRINT[report.parameters$subsetParentDisplay] = result$parent
PRINT[paste("BG ", report.parameters$subsetParentDisplay)] = result$parent_bg
PRINT$response = result$response
PRINT$raw_p = result$raw_p
PRINT$adj_p = result$adj_p

write.table(PRINT, file = "${tsvout:tsvfile}", sep = "\t", qmethod = "double", col.names=NA)

