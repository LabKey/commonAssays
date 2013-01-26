/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either Express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE SCHEMA NAb;
GO

CREATE TABLE NAb.CutoffValue
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    NAbSpecimenId INT NOT NULL,
    Cutoff REAL,
    Point REAL,
    PointOORIndicator NVARCHAR(20),

    IC_Poly REAL,
    IC_PolyOORIndicator NVARCHAR(20),
    IC_4pl REAL,
    IC_4plOORIndicator NVARCHAR(20),
    IC_5pl REAL,
    IC_5plOORIndicator NVARCHAR(20),

    CONSTRAINT PK_NAb_CutoffValue PRIMARY KEY (RowId)
);

CREATE TABLE NAb.NAbSpecimen
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    DataId INT,
    RunId INT NOT NULL,
    SpecimenLSID LSIDtype NOT NULL,
    FitError REAL,
    WellgroupName NVARCHAR(100),

    AUC_poly REAL,
    PositiveAUC_Poly REAL,
    AUC_4pl REAL,
    PositiveAUC_4pl REAL,
    AUC_5pl REAL,
    PositiveAUC_5pl REAL,

    -- For legacy migration purposes
    ObjectUri NVARCHAR(300),
    ObjectId INT NOT NULL,
    ProtocolId INT,

    CONSTRAINT PK_NAb_Specimen PRIMARY KEY (RowId),
    CONSTRAINT FK_NAbSpecimen_ExperimentRun FOREIGN KEY (RunId)
      REFERENCES Exp.ExperimentRun (RowId)
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT FK_NAbSpecimen_SpecimenLSID FOREIGN KEY (SpecimenLSID)
      REFERENCES Exp.Material (LSID)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IDX_NAbSpecimen_RunId ON NAb.NAbSpecimen(RunId);
CREATE INDEX IDX_NAbSpecimen_ObjectId ON NAb.NAbSpecimen(ObjectId);
CREATE INDEX IDX_NAbSpecimen_DataId ON NAb.NAbSpecimen(DataId);
