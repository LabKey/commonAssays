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

CREATE TABLE ms1.PeaksFiles
(
    PeaksFileID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    ExpDataFileID INT NOT NULL,
    MzXmlURL NVARCHAR(800) NULL,
    Description NVARCHAR(255) NULL,
    Imported BIT DEFAULT 0
)
GO

CREATE TABLE ms1.Scans
(
    PeaksFileID INT NOT NULL FOREIGN KEY REFERENCES ms1.PeaksFiles(PeaksFileID),
    Scan INT NOT NULL,
    RetentionTime FLOAT NULL,
    ObservedDuration FLOAT NULL,
    
    CONSTRAINT PK_Scans PRIMARY KEY CLUSTERED (PeaksFileID,Scan)
)
GO

CREATE TABLE ms1.CalibrationParams
(
    PeaksFileID INT NOT NULL,
	Scan INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value FLOAT NOT NULL,

    CONSTRAINT PK_CalibrationParams PRIMARY KEY CLUSTERED (PeaksFileID,Scan,Name),
    CONSTRAINT FK_CalibrationParms_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
)
GO

CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    PeaksFileID INT NULL,
    Scan INT NULL,
    MZ FLOAT NULL,
    Charge TINYINT NULL
)
GO

CREATE TABLE ms1.Peaks
(
    PeakID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    PeaksFileID INT NOT NULL,
    Scan INT NOT NULL,
    MZ FLOAT NULL,
    Frequency FLOAT NULL,
    Amplitude FLOAT NULL,
    Phase FLOAT NULL,
    Decay FLOAT NULL,
    Error FLOAT NULL,
    Area FLOAT NULL,

    CONSTRAINT FK_Peaks_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
)
GO

CREATE TABLE ms1.PeaksToFamilies
(
    PeakID INT NOT NULL FOREIGN KEY REFERENCES ms1.Peaks(PeakID),
    PeakFamilyID INT NOT NULL FOREIGN KEY REFERENCES ms1.PeakFamilies(PeakFamilyID),
    CONSTRAINT PK_PeaksToFamilies PRIMARY KEY CLUSTERED (PeakID,PeakFamilyID) 
)
GO

CREATE TABLE ms1.FeaturesFiles
(
	FeaturesFileID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    ExpDataFileID INT NOT NULL,
    MzXmlURL NVARCHAR(800) NULL,
)
GO

CREATE TABLE ms1.Features
(
    FeatureID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
	FeaturesFileID INT FOREIGN KEY REFERENCES ms1.FeaturesFiles(FeaturesFileID),
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
    MS2ConnectivityProbability FLOAT NULL
)
GO

