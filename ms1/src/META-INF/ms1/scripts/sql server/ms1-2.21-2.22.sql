/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

/* SQL Server Version */

/* drop existing tables */
drop table ms1.PeaksToFamilies
go
drop table ms1.Peaks
go
drop table ms1.PeakFamilies
go
drop table ms1.CalibrationParams
go
drop table ms1.Scans
go
drop table ms1.PeaksFiles
go
drop table ms1.Features
go
drop table ms1.FeaturesFiles
go


/* recreate with changes */
CREATE TABLE ms1.PeaksFiles
(
    PeaksFileID INT IDENTITY NOT NULL,
    ExpDataFileID INT NOT NULL,
    MzXmlURL NVARCHAR(800) NULL,
    Description NVARCHAR(255) NULL,
    Imported BIT DEFAULT 0,

    CONSTRAINT PK_PeaksFiles PRIMARY KEY (PeaksFileID)
)
GO

CREATE INDEX IDX_PeaksFiles_ExpData ON ms1.PeaksFiles(ExpDataFileID)
GO

CREATE TABLE ms1.Scans
(
    PeaksFileID INT NOT NULL,
    Scan INT NOT NULL,
    RetentionTime FLOAT NULL,
    ObservedDuration FLOAT NULL,

    CONSTRAINT PK_Scans PRIMARY KEY (PeaksFileID,Scan),
    CONSTRAINT FK_Scans_PeaksFiles FOREIGN KEY (PeaksFileID) REFERENCES ms1.PeaksFiles(PeaksFileID)
)
GO

CREATE TABLE ms1.CalibrationParams
(
    PeaksFileID INT NOT NULL,
	Scan INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value FLOAT NOT NULL,

    CONSTRAINT PK_CalibrationParams PRIMARY KEY (PeaksFileID,Scan,Name),
    CONSTRAINT FK_CalibrationParms_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
)
GO

CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyID INT IDENTITY NOT NULL,
    PeaksFileID INT NULL,
    Scan INT NULL,
    MZ FLOAT NULL,
    Charge TINYINT NULL,

    CONSTRAINT PK_PeakFamilies PRIMARY KEY (PeakFamilyID)
)
GO

CREATE INDEX IDX_PeakFamilies_Scans ON ms1.PeakFamilies(PeaksFileID,Scan)
GO

CREATE TABLE ms1.Peaks
(
    PeakID INT IDENTITY NOT NULL,
    PeaksFileID INT NOT NULL,
    Scan INT NOT NULL,
    MZ FLOAT NULL,
    Frequency FLOAT NULL,
    Amplitude FLOAT NULL,
    Phase FLOAT NULL,
    Decay FLOAT NULL,
    Error FLOAT NULL,
    Area FLOAT NULL,

    CONSTRAINT PK_Peaks PRIMARY KEY NONCLUSTERED (PeakID),
    CONSTRAINT FK_Peaks_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
)
GO

CREATE CLUSTERED INDEX IDX_Peaks_Scans ON ms1.Peaks(PeaksFileID,Scan)
GO

CREATE TABLE ms1.PeaksToFamilies
(
    PeakID INT NOT NULL,
    PeakFamilyID INT NOT NULL,

    CONSTRAINT PK_PeaksToFamilies PRIMARY KEY (PeakID,PeakFamilyID),
    CONSTRAINT FK_PeaksToFamilies_Peaks FOREIGN KEY (PeakID) REFERENCES ms1.Peaks(PeakID),
    CONSTRAINT FK_PeaksToFamilies_PeakFamilies FOREIGN KEY (PeakFamilyID) REFERENCES ms1.PeakFamilies(PeakFamilyID)
)
GO

CREATE TABLE ms1.FeaturesFiles
(
	FeaturesFileID INT IDENTITY NOT NULL,
    ExpDataFileID INT NOT NULL,
    MzXmlURL NVARCHAR(800) NULL,

    CONSTRAINT PK_FeaturesFiles PRIMARY KEY (FeaturesFileID)
)
GO

CREATE INDEX IDX_FeaturesFiles_ExpData ON ms1.FeaturesFiles(ExpDataFileID)
GO

CREATE TABLE ms1.Features
(
    FeatureID INT IDENTITY NOT NULL,
	FeaturesFileID INT,
    Scan INT NULL,
    Time FLOAT NULL,
    MZ FLOAT NULL,
    AccurateMZ BIT NULL,
    Mass FLOAT NULL,
    Intensity FLOAT NULL,
    Charge TINYINT NULL,
    ChargeStates TINYINT NULL,
    KL FLOAT NULL,
    Background FLOAT NULL,
    Median FLOAT NULL,
    Peaks TINYINT NULL,
    ScanFirst INT NULL,
    ScanLast INT NULL,
    ScanCount INT NULL,
    TotalIntensity FLOAT NULL,
    Description NVARCHAR(300) NULL,

    /* extra cols for ceaders-sinai */
    MS2Scan INT NULL,
    MS2ConnectivityProbability FLOAT NULL,

    CONSTRAINT PK_Features PRIMARY KEY (FeatureID),
    CONSTRAINT FK_Features_FeaturesFiles FOREIGN KEY (FeaturesFileID) REFERENCES ms1.FeaturesFiles(FeaturesFileID)
)
GO

CREATE INDEX IDX_Features_FeaturesFiles ON ms1.Features(FeaturesFileID)
GO