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
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetFValSeries', @newname = 'FValSeries', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries1', @newname = 'ObsSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries2', @newname = 'ObsSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetObsSeries3', @newname = 'ObsSeries3', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries1', @newname = 'ModelPosSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries2', @newname = 'ModelPosSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelPosSeries3', @newname = 'ModelPosSeries3', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries1', @newname = 'ModelNegSeries1', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries2', @newname = 'ModelNegSeries2', @objtype = 'COLUMN'
EXEC sp_rename @objname = 'ms2.PeptideProphetSummaries.PepProphetModelNegSeries3', @newname = 'ModelNegSeries3', @objtype = 'COLUMN'
GO

ALTER TABLE ms2.PeptideProphetSummaries ADD
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL
GO
