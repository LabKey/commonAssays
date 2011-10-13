##
#  Copyright (c) 2010 LabKey Corporation
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
# normalization-parameters: ${normalization-parameters}

NCDF<-FALSE
USEMPI<-FALSE
USEMULTICORE<-FALSE
if(USEMPI){
    require(Rmpi)
}
if (NCDF) {
    require(ncdfFlow)
}
if(USEMULTICORE){
    require(multicore)
}

require(flowWorkspace)

# for md5sum
require(tools)

# Some hard-coded variables that should be passed in by the calling process.
workspacePath <- "${workspace-path}"
fcsFileDir <- "${fcsfile-directory}"
#workspacefile <- dir(path=pathtoxml,pattern="xml")


# A legacy flowWorkspace parameter.. should always be true
EXECUTENOW <- TRUE

# The group to import .. should be a homogeneous group (ie. 'All Samples' group is probably not a good idea)
GROUP <- "${group-names}"

# Keywords that you should use to annotate the samples
Keywords <- c("Stim","EXPERIMENT NAME","Sample Order")

# Directory to export the results
outputDir <- "${output-directory}"


# open the workspace
print(paste("opening workspace", workspacePath, "..."))
ws <- openWorkspace(workspacePath)

# parse the workspace
print(paste("parsing workspace", workspacePath, "..."))
G <- parseWorkspace(ws, path=fcsFileDir, isNcdf=NCDF, execute=EXECUTENOW, name=GROUP)

# export the required files
print(paste("exporting workspace", workspacePath, "to", outputDir, "..."))
ExportTSVAnalysis(x=G, Keywords=Keywords, EXPORT=outputDir)

