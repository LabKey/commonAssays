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

CREATE SCHEMA ms1;

/* table for storing information about the data files themselves (both features.tsv and peaks.xml) */
CREATE TABLE ms1.Files
(
    FileId SERIAL NOT NULL,
    ExpDataFileId INT NOT NULL,
    Type SMALLINT NOT NULL,
    MzXmlUrl VARCHAR(800) NULL,
    Imported BOOLEAN NOT NULL DEFAULT FALSE,
    Deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT PK_Files PRIMARY KEY (FileId)
);
CREATE INDEX IDX_Files_ExpDataFileId ON ms1.Files(ExpDataFileId);

/* table for storing information about software packages used to produce those files */
CREATE TABLE ms1.Software
(
    SoftwareId SERIAL NOT NULL,
    FileId INT NOT NULL,
    Name VARCHAR(400) NOT NULL,
    Version VARCHAR(16) NULL,
    Author VARCHAR(800) NULL,

    CONSTRAINT PK_Software PRIMARY KEY (SoftwareId),
    CONSTRAINT FK_Software_FileId FOREIGN KEY (FileId) REFERENCES ms1.Files(FileId)
);
CREATE INDEX IDX_Software_FileId ON ms1.Software(FileId);

/* table to record parameters used by software to produce data files */
CREATE TABLE ms1.SoftwareParams
(
    SoftwareId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value VARCHAR(255) NULL,

    CONSTRAINT PK_FileSoftwareParams PRIMARY KEY (SoftwareId,Name),
    CONSTRAINT FK_FileSoftwareParams_SoftwareId FOREIGN KEY (SoftwareId) REFERENCES ms1.Software(SoftwareId)
);

/* table to store information about scans */
CREATE TABLE ms1.Scans
(
    ScanId SERIAL NOT NULL,
    FileId INT NOT NULL,
    Scan INT NOT NULL,
    RetentionTime DOUBLE PRECISION NULL,
    ObservationDuration DOUBLE PRECISION NULL,

    CONSTRAINT PK_Scans PRIMARY KEY (ScanId),
    CONSTRAINT FK_Scans_FileId FOREIGN KEY (FileID) REFERENCES ms1.Files(FileId)
);
CREATE INDEX IDX_Scans_FileId ON ms1.Scans(FileId);
CREATE INDEX IDX_Scans_Scan ON ms1.Scans(Scan);

/* table to store calibrations for each scan */
CREATE TABLE ms1.Calibrations
(
    ScanId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value DOUBLE PRECISION NOT NULL,

    CONSTRAINT PK_Calibrations PRIMARY KEY (ScanId,Name),
    CONSTRAINT FK_Calibrations_ScanId FOREIGN KEY (ScanId) REFERENCES ms1.Scans(ScanId)
);

/* table to store information about peaks within a scan */
CREATE TABLE ms1.Peaks
(
    PeakId SERIAL NOT NULL,
    ScanId INT NOT NULL,
    MZ DOUBLE PRECISION NULL,
    Intensity DOUBLE PRECISION NULL,
    Area DOUBLE PRECISION NULL,
    Error DOUBLE PRECISION NULL,
    Frequency DOUBLE PRECISION NULL,
    Phase DOUBLE PRECISION NULL,
    Decay DOUBLE PRECISION NULL,

    CONSTRAINT PK_Peaks PRIMARY KEY (PeakId),
    CONSTRAINT FK_Peaks_ScanId FOREIGN KEY (ScanId) REFERENCES ms1.Scans(ScanId)
);
CREATE INDEX IDX_Peaks_ScanId ON ms1.Peaks(ScanId);

/* table to store information about peak families, which are a set of related peaks */
CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyId SERIAL NOT NULL,
    ScanId INT NULL,
    MzMono DOUBLE PRECISION NULL,
    Charge SMALLINT NULL,

    CONSTRAINT PK_PeakFamilies PRIMARY KEY (PeakFamilyId)
);
CREATE INDEX IDX_PeakFamilies_ScanId ON ms1.PeakFamilies(ScanId);

/* table to link peak families to peaks (m:m) */
CREATE TABLE ms1.PeaksToFamilies
(
    PeakFamilyId INT NOT NULL,
    PeakId INT NOT NULL,

    CONSTRAINT PK_PeaksToFamilies PRIMARY KEY (PeakFamilyId,PeakId),
    CONSTRAINT FK_PeaksToFamilies_PeakId FOREIGN KEY (PeakId) REFERENCES ms1.Peaks(PeakId),
    CONSTRAINT FK_PeaksToFamilies_PeakFamilyId FOREIGN KEY (PeakFamilyId) REFERENCES ms1.PeakFamilies(PeakFamilyId)
);
CREATE INDEX IDX_PeaksToFamilies_PeakId ON ms1.PeaksToFamilies(PeakId);

/* table to store information about features */
CREATE TABLE ms1.Features
(
    FeatureId SERIAL NOT NULL,
    FileId INT,
    Scan INT NULL,
    Time DOUBLE PRECISION NULL,
    MZ DOUBLE PRECISION NULL,
    AccurateMZ BOOLEAN NULL,
    Mass DOUBLE PRECISION NULL,
    Intensity DOUBLE PRECISION NULL,
    Charge SMALLINT NULL,
    ChargeStates SMALLINT NULL,
    KL DOUBLE PRECISION NULL,
    Background DOUBLE PRECISION NULL,
    Median DOUBLE PRECISION NULL,
    Peaks INT NULL,
    ScanFirst INT NULL,
    ScanLast INT NULL,
    ScanCount INT NULL,
    TotalIntensity DOUBLE PRECISION NULL,
    Description VARCHAR(300) NULL,

    /* extra cols for ceders-sinai */
    MS2Scan INT NULL,
    MS2ConnectivityProbability DOUBLE PRECISION NULL,
    MS2Charge SMALLINT NULL,

    CONSTRAINT PK_Features PRIMARY KEY (FeatureId),
    CONSTRAINT FK_Features_FileId FOREIGN KEY (FileID) REFERENCES ms1.Files(FileId)
);
CREATE INDEX IDX_Features_FileId ON ms1.Features(FileId);
