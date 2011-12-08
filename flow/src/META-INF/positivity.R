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

DOUBLE_MIN = 1E-307

# I'm not sure why is.numeric() is returning TRUE in my script.
# We can check both is.numeric() and !is.na() to weed out NA and empty string.
isNum <- function (x)
{
    return (!is.na(x) && is.numeric(x))
}

positivity <- function (data, grouping_columns)
{
    cat("** calculating positivity for", length(data), "rows.\n")

    # generate raw p-values
    cat("** generate raw p-values\n")
    data$raw_p = sapply(1:nrow(data), function(i) {
      pval = NA

      stat      = data$stat[i]
      stat_bg   = data$stat_bg[i]
      parent    = data$parent[i]
      parent_bg = data$parent_bg[i]

      cat("  run=", as.character(data$run[i]), ", well=", as.character(data$well[i]), sep="")
      cat(", stat=", stat, ", parent=", parent, ", stat_bg=", stat_bg, ", parent_bg=", parent_bg, sep="")
      if (isNum(stat) && isNum(stat_bg) && isNum(parent) && isNum(parent_bg))
      {
        x.ant = as.integer(stat)
        N.ant = as.integer(parent - stat)
        x.neg = as.integer(stat_bg)
        N.neg = as.integer(parent_bg - stat_bg)

        m = matrix(c(x.ant,N.ant,x.neg,N.neg), nc=2, byrow=FALSE)

        pval = fisher.test(m, alternative="greater")$p.value
        #cat(", raw_p=", pval, sep="")
        if (pval < DOUBLE_MIN) {
          #cat(" ** below 1e-307 cutoff **")
          pval = 0
        }
      } else {
        cat(" ** one or more values are not a number. skipping well. **")
      }
      cat("\n")

      return(pval)
    })

    # compute adjusted p-values
    cat("\n** compute adjusted p-values\n")
    data = by(data, subset(data, select=grouping_columns), function(ss) {
      #ss$adj_p = p.adjust(ss$raw_p, method="holm")
      ss$adj_p = p.adjust(ss$raw_p, method="bonferroni")
      ss$adj_p[ss$adj_p < DOUBLE_MIN] <- 0

      #cat("\n")
      #print(ss[1,grouping_columns])
      #cat("  number of wells in group:", length(ss$well), "\n")
      #cat("  well:", as.character(ss$well), "\n", sep="\t")
      #cat("  raw_p:", ss$raw_p, "\n", sep="\t")
      #cat("  adj_p:", ss$adj_p, "\n", sep="\t")
      return(ss)
    })
    data = do.call(rbind, data)

    # define response call
    cat("\n** response cutoff for adjusted p-values <=", ALPHA, "\n", sep="")
    data$response = as.numeric(data$adj_p <= ALPHA)

    cat("\n** done\n");
    return(data)
}

if (length(labkey.data$stat) == 0)
    stop("labkey.data is empty");

if (length(labkey.data$stat) == 0 || length(labkey.data$stat_bg) == 0 || length(labkey.data$parent) == 0 || length(labkey.data$parent_bg) == 0)
    stop("labkey.data$stat, labkey.data$stat_bg, labkey.data$parent, labkey.data$parent_bg are required");

if (!exists("flow.metadata.study.participantColumn"))
    stop("ICS study metadata must include participant column")

if (!exists("flow.metadata.study.visitColumn") && !exists("flow.metadata.study.dateColumn"))
    stop("ICS study metadata must include either visit or date column")

if (exists("flow.metadata.study.visitColumn")) {
    grouping_cols = c(flow.metadata.study.participantColumn, flow.metadata.study.visitColumn)
} else {
    grouping_cols = c(flow.metadata.study.participantColumn, flow.metadata.study.dateColumn)
}
cat("Grouping by:", grouping_cols, "\n")

result <- positivity(labkey.data, grouping_cols)

write.table(result, file = "${tsvout:FCSAnalyses}", sep = "\t", qmethod = "double", col.names=NA)


