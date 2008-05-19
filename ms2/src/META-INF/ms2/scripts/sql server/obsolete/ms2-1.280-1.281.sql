/*
 * Copyright (c) 2006-2008 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
ALTER TABLE ms2.MS2Runs
    ADD HasPeptideProphet BIT NOT NULL DEFAULT '0'
GO

CREATE TABLE ms2.PeptideProphetSummaries
(
    RunId INT NOT NULL,
    PepProphetFValSeries IMAGE NULL,
    PepProphetObsSeries1 IMAGE NULL,
    PepProphetObsSeries2 IMAGE NULL,
    PepProphetObsSeries3 IMAGE NULL,
    PepProphetModelPosSeries1 IMAGE NULL,
    PepProphetModelPosSeries2 IMAGE NULL,
    PepProphetModelPosSeries3 IMAGE NULL,
    PepProphetModelNegSeries1 IMAGE NULL,
    PepProphetModelNegSeries2 IMAGE NULL,
    PepProphetModelNegSeries3 IMAGE NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (RunId)
)
GO

-- No changes to this view, but we need to rebuild it since we added a column to MS2Runs
DROP VIEW ms2.MS2ExperimentRuns
GO

CREATE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO
