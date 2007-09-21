/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

CREATE TABLE ms1.Scans
(
    Scan INT NOT NULL PRIMARY KEY CLUSTERED,
    RetentionTime REAL NULL,
    ObservedDuration REAL NULL
)
GO

CREATE TABLE ms1.CalibrationParams
(
    Name NVARCHAR(255) NOT NULL PRIMARY KEY CLUSTERED,
	Scan INT NOT NULL FOREIGN KEY REFERENCES ms1.Scans(Scan),
    Value REAL NOT NULL
)
GO

CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    Scan INT NOT NULL FOREIGN KEY REFERENCES ms1.Scans(Scan),
    MZ REAL NULL,
    Charge TINYINT NULL
)
GO

CREATE TABLE ms1.Peaks
(
    PeakID INT IDENTITY NOT NULL PRIMARY KEY CLUSTERED,
    PeakFamilyID INT NOT NULL FOREIGN KEY REFERENCES ms1.PeakFamilies(PeakFamilyID),
    Frequency REAL NULL,
    Amplitude REAL NULL,
    Phase REAL NULL,
    Decay REAL NULL,
    Error REAL NULL,
    Area REAL NULL
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
    Time REAL NULL,
    MZ REAL NULL,
    AccurateMZ BIT NULL,
    Mass REAL NULL,
    Intensity REAL NULL,
    Charge TINYINT NULL,
    ChargeStates TINYINT NULL,
    KL REAL NULL,
    Background REAL NULL,
    Median REAL NULL,
    Peaks TINYINT NULL,
    ScanFirst INT NULL,
    ScanLast INT NULL,
    ScanCount INT NULL,
    TotalIntensity REAL NULL,
    Description NVARCHAR(300) NULL,

    /* extra cols for ceaders-sinai */
    MS2Scan INT NULL,
    MS2ConnectivityProbability REAL NULL,
)
GO

