/*
 * Copyright (c) 2011 LabKey Corporation
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

CREATE TABLE luminex.Titration
(
    RowId INT IDENTITY(1,1) NOT NULL,
    RunId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Standard BIT NOT NULL,
    QCControl BIT NOT NULL,

    CONSTRAINT PK_Luminex_Titration PRIMARY KEY (RowId),
    CONSTRAINT FK_Luminex_Titration_RunId FOREIGN KEY (RunId) REFERENCES exp.ExperimentRun(RowId),
    CONSTRAINT UQ_Titration UNIQUE (Name, RunId)
)
GO

CREATE TABLE luminex.AnalyteTitration
(
    AnalyteId INT NOT NULL,
    TitrationId INT NOT NULL,

    CONSTRAINT PK_Luminex_AnalyteTitration PRIMARY KEY (AnalyteId, TitrationId)
)
GO

CREATE INDEX IX_LuminexTitration_RunId ON luminex.Titration (RunId)
GO

-- Assume a single titration for all existing data
INSERT INTO luminex.Titration
    (RunId, Name, Standard, QCControl)
    SELECT RowId, 'Standard', 1, 0 FROM exp.experimentrun WHERE protocollsid LIKE '%:LuminexAssayProtocol.%'
GO

-- Assign all existing analytes to the single titration created above
INSERT INTO luminex.AnalyteTitration
    (AnalyteId, TitrationId)
    SELECT a.RowId, t.RowId
        FROM luminex.analyte a, exp.data d, luminex.titration t, exp.protocolapplication pa
        WHERE a.dataid = d.rowid AND t.runid = pa.runid AND d.sourceapplicationid = pa.rowid
GO

ALTER TABLE luminex.datarow ADD TitrationId INT
GO

ALTER TABLE luminex.datarow ADD CONSTRAINT FK_Luminex_DataRow_TitrationId FOREIGN KEY (TitrationId) REFERENCES luminex.Titration(RowId)
GO

CREATE INDEX IX_LuminexDataRow_TitrationId ON luminex.DataRow (TitrationId)
GO

-- Assume that any existing data that has an expected concentration is part of the single standard titration
-- for that run
UPDATE luminex.datarow SET titrationid =
    (SELECT t.rowid FROM luminex.titration t, exp.data d, exp.protocolapplication pa
        WHERE pa.runid = t.runid AND d.sourceapplicationid = pa.rowid AND dataid = d.rowid)
    WHERE expconc IS NOT NULL
GO

-- Use the description as the titration name, grabbing an arbitrary one if they're not all the same
UPDATE luminex.titration SET name = (SELECT COALESCE(MIN(description), 'Standard') FROM luminex.datarow WHERE luminex.titration.rowid = titrationid)
GO
