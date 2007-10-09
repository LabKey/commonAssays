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

/* SQL Server Version */

/* The data model for the Peak.xml file changed, so we need to modify our tables accordingly */

/* the following tables need to cange in fairly radical ways, so just drop them and re-create */
/* drop existing tables that need to change */
EXEC core.fn_dropifexists 'PeaksToFamilies', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Peaks', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'PeakFamilies', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'CalibrationParams', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Calibrations', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Scans', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'PeaksFiles', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Features', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'FeaturesFiles', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'PeaksToFamilies', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'FilesToSoftware', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'FileSoftwareParams', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'SoftwareParams', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Software', 'ms1', 'TABLE'
GO
EXEC core.fn_dropifexists 'Files', 'ms1', 'TABLE'
GO

/* re-create them with changes, and add new tables */

/* table for storing information about the data files themselves (both features.tsv and peaks.xml) */
CREATE TABLE ms1.Files
(
    FileId INT IDENTITY NOT NULL,
    ExpDataFileId INT NOT NULL,
    Type TINYINT NOT NULL,
    MzXmlUrl NVARCHAR(800) NULL,
    Imported BIT NOT NULL DEFAULT 0,

    CONSTRAINT PK_Files PRIMARY KEY NONCLUSTERED (FileId)
)
GO
CREATE CLUSTERED INDEX IDX_Files_ExpDataFileId ON ms1.Files(ExpDataFileId)
GO

/* table for storing information about software packages used to produce those files */
CREATE TABLE ms1.Software
(
    SoftwareId INT IDENTITY NOT NULL,
    FileId INT NOT NULL,
    Name NVARCHAR(400) NOT NULL,
    Version NVARCHAR(16) NULL,
    Author NVARCHAR(800) NULL,

    CONSTRAINT PK_Software PRIMARY KEY NONCLUSTERED (SoftwareId),
    CONSTRAINT FK_Software_FileId FOREIGN KEY (FileId) REFERENCES ms1.Files
)
GO
CREATE CLUSTERED INDEX IDX_Software_FileId ON ms1.Software(FileId)
GO

/* table to record parameters used by software to produce data files */
CREATE TABLE ms1.SoftwareParams
(
    SoftwareId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value NVARCHAR(255) NULL,

    CONSTRAINT PK_FileSoftwareParams PRIMARY KEY (SoftwareId,Name),
    CONSTRAINT FK_FileSoftwareParams_SoftwareId FOREIGN KEY (SoftwareId) REFERENCES ms1.Software(SoftwareId)
)
GO

/* table to store information about scans */
CREATE TABLE ms1.Scans
(
    ScanId INT IDENTITY NOT NULL,
    FileId INT NOT NULL,
    Scan INT NOT NULL,
    RetentionTime FLOAT NULL,
    ObservationDuration FLOAT NULL,

    CONSTRAINT PK_Scans PRIMARY KEY NONCLUSTERED (ScanId),
    CONSTRAINT FK_Scans_FileId FOREIGN KEY (FileID) REFERENCES ms1.Files(FileId)
)
GO
CREATE CLUSTERED INDEX IDX_Scans_FileId ON ms1.Scans(FileId)
GO


/* table to store calibrations for each scan */
CREATE TABLE ms1.Calibrations
(
    ScanId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,
    Value FLOAT NOT NULL,

    CONSTRAINT PK_Calibrations PRIMARY KEY (ScanId,Name),
    CONSTRAINT FK_Calibrations_ScanId FOREIGN KEY (ScanId) REFERENCES ms1.Scans(ScanId)
)
GO

/* table to store information about peaks within a scan */
CREATE TABLE ms1.Peaks
(
    PeakId INT IDENTITY NOT NULL,
    ScanId INT NOT NULL,
    MZ FLOAT NULL,
    Intensity FLOAT NULL,
    Area FLOAT NULL,
    Error FLOAT NULL,
    Frequency FLOAT NULL,
    Phase FLOAT NULL,
    Decay FLOAT NULL,

    CONSTRAINT PK_Peaks PRIMARY KEY NONCLUSTERED (PeakId),
    CONSTRAINT FK_Peaks_ScanId FOREIGN KEY (ScanId) REFERENCES ms1.Scans(ScanId)
)
GO
CREATE CLUSTERED INDEX IDX_Peaks_ScanId ON ms1.Peaks(ScanId)
GO

/* table to store information about peak families, which are a set of related peaks */
CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyId INT IDENTITY NOT NULL,
    ScanId INT NULL,
    MzMono FLOAT NULL,
    Charge TINYINT NULL,

    CONSTRAINT PK_PeakFamilies PRIMARY KEY NONCLUSTERED (PeakFamilyId)
)
GO
CREATE CLUSTERED INDEX IDX_PeakFamilies_ScanId ON ms1.PeakFamilies(ScanId)
GO

/* table to link peak families to peaks (m:m) */
CREATE TABLE ms1.PeaksToFamilies
(
    PeakFamilyId INT NOT NULL,
    PeakId INT NOT NULL,

    CONSTRAINT PK_PeaksToFamilies PRIMARY KEY (PeakFamilyId,PeakId),
    CONSTRAINT FK_PeaksToFamilies_PeakId FOREIGN KEY (PeakId) REFERENCES ms1.Peaks(PeakId),
    CONSTRAINT FK_PeaksToFamilies_PeakFamilyId FOREIGN KEY (PeakFamilyId) REFERENCES ms1.PeakFamilies(PeakFamilyId)
)
GO

/* table to store information about features */
CREATE TABLE ms1.Features
(
    FeatureId INT IDENTITY NOT NULL,
	FileId INT,
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
    Peaks INT NULL,
    ScanFirst INT NULL,
    ScanLast INT NULL,
    ScanCount INT NULL,
    TotalIntensity FLOAT NULL,
    Description NVARCHAR(300) NULL,

    /* extra cols for ceaders-sinai */
    MS2Scan INT NULL,
    MS2ConnectivityProbability FLOAT NULL,

    CONSTRAINT PK_Features PRIMARY KEY NONCLUSTERED (FeatureId),
    CONSTRAINT FK_Features_FileId FOREIGN KEY (FileID) REFERENCES ms1.Files(FileId)
)
GO
CREATE CLUSTERED INDEX IDX_Features_FileId ON ms1.Features(FileId)
GO
