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
-- More accurate column name
EXEC sp_rename
    @objname = 'ms2.Runs.SampleEnzyme',
    @newname = 'SearchEnzyme',
    @objtype = 'COLUMN'
GO

-- Rebuild view
DROP VIEW ms2.ExperimentRuns
GO

CREATE VIEW ms2.ExperimentRuns AS
    SELECT ms2.Runs.*, exp.ExperimentRun.RowId AS ExperimentRunRowId, exp.Protocol.Name AS ProtocolName
    FROM ms2.Runs
        LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.Runs.ExperimentRunLSID
        LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID
GO
