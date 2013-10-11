##
#  Copyright (c) 2010-2013 LabKey Corporation
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

# workspace-path: ${workspace-path}
# fcsfile-directory: ${fcsfile-directory}
# run-name: ${run-name}
# group-names: ${group-names}
# perform-normalization: ${perform-normalization}
# normalization-reference: ${normalization-reference}
# normalization-subsets: ${normalization-subsets}
# normalization-parameters: ${normalization-parameters}

USEMPI<-FALSE
if(USEMPI){
    require(Rmpi)
}
NCDF<-require(ncdfFlow)
USEMULTICORE<-require(multicore)

# for md5sum
library(tools)

library(hexbin)
library(flowStats)
library(flowWorkspace)


# Some hard-coded variables that should be passed in by the calling process.
workspacePath <- "${workspace-path}"
if (!file.exists(workspacePath)) stop(paste("workspace doesn't eixst: ", workspacePath))

fcsFileDir <- "${fcsfile-directory}"
if (!file.exists(fcsFileDir)) stop(paste("FCS directory doesn't eixst: ", fcsFileDir))


# A legacy flowWorkspace parameter.. should always be true
EXECUTENOW <- TRUE

# The group to import .. should be a homogeneous group (ie. 'All Samples' group is probably not a good idea)
GROUP <- "${group-names}"

# Keywords that you should use to annotate the samples
Keywords <- c("Stim","EXPERIMENT NAME","Sample Order")

# Directory to export the results
rAnalysisDir <- "${r-analysis-directory}"
if (!file.exists(rAnalysisDir)) {
    dir.create(rAnalysisDir, recursive=TRUE)
}
if (!file.exists(rAnalysisDir)) stop(paste("Output directory doesn't eixst: ", rAnalysisDir))

normalizedDir <- "${normalized-directory}"
if (!file.exists(normalizedDir)) {
    dir.create(normalizedDir, recursive=TRUE)
}
if (!file.exists(normalizedDir)) stop(paste("Output directory doesn't eixst: ", normalizedDir))

# open the workspace
cat("opening workspace", workspacePath, "...\n")
ws <- openWorkspace(workspacePath)

# parse the workspace
if (length(GROUP) > 0) {
    cat("parsing workspace", workspacePath, "and loading group", GROUP, "...\n")
} else {
    cat("parsing workspace", workspacePath, "...\n")
}
system.time(G <- parseWorkspace(ws, path=fcsFileDir, isNcdf=NCDF, execute=EXECUTENOW, name=GROUP))
cat("finished parsing", length(G), "samples \n")

# export the required files
cat("exporting R analysis ", workspacePath, "to", rAnalysisDir, "...\n")
system.time(ExportTSVAnalysis(x=G, Keywords=Keywords, EXPORT=rAnalysisDir))
cat("finished exporting analysis.\n")

# perform normalization if requested
if (${perform-normalization}) {
    includedGates <- paste("/", ${normalization-subsets}, sep="")
    excludedGates <- flowWorkspace:::.includedGate2ExcludedGate(G, includedGates)
    cat("subsets selected for normalization:", includedGates, "\n")
    cat("... excluded internal gate ids:", excludedGates, "\n")

    excludedDims <- flowWorkspace:::.includedChannel2ExcludedChannel(G, ${normalization-parameters})
    cat("channels selected for normalization:", ${normalization-parameters}, "\n")
    cat("... excluded channels:", excludedDims, "\n")

    cat("performing normalization...\n")
    system.time(N <- flowStats:::normalizeGatingSet(G, bwFac=2, target="${normalization-reference}", skipdims=excludedDims, skipgates=excludedGates))
    cat("finished normalizing", length(N), "samples\n")

    cat("exporting normalized analysis", workspacePath, "to", normalizedDir, "...\n")
    system.time(ExportTSVAnalysis(x=N, Keywords=Keywords, EXPORT=normalizedDir))
    cat("finished exporting normalized analysis.\n")
}

