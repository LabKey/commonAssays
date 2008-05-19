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

/* PostgreSQL Version */

/* drop existing tables */
drop table ms1.PeaksToFamilies;
drop table ms1.Peaks;
drop table ms1.PeakFamilies;
drop table ms1.CalibrationParams;
drop table ms1.Scans;
drop table ms1.PeaksFiles;
drop table ms1.Features;
drop table ms1.FeaturesFiles;


/* recreate with changes */
CREATE TABLE ms1.PeaksFiles
(
    PeaksFileID SERIAL NOT NULL,
    ExpDataFileID INT NOT NULL,
    MzXmlURL VARCHAR(800) NULL,
    Description VARCHAR(255) NULL,
    Imported BOOLEAN DEFAULT FALSE,

    CONSTRAINT PK_PeaksFiles PRIMARY KEY (PeaksFileID)
);

CREATE INDEX IDX_PeaksFiles_ExpData ON ms1.PeaksFiles(ExpDataFileID);

CREATE TABLE ms1.Scans
(
    PeaksFileID INT NOT NULL,
    Scan INT NOT NULL,
    RetentionTime DOUBLE PRECISION NULL,
    ObservedDuration DOUBLE PRECISION NULL,

    CONSTRAINT PK_Scans PRIMARY KEY (PeaksFileID,Scan),
    CONSTRAINT FK_Scans_PeaksFiles FOREIGN KEY (PeaksFileID) REFERENCES ms1.PeaksFiles(PeaksFileID)
);

CREATE TABLE ms1.CalibrationParams
(
    PeaksFileID INT NOT NULL,
	Scan INT NOT NULL,
    Name VARCHAR(255) NOT NULL,
    Value DOUBLE PRECISION NOT NULL,

    CONSTRAINT PK_CalibrationParams PRIMARY KEY (PeaksFileID,Scan,Name),
    CONSTRAINT FK_CalibrationParms_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
);

CREATE TABLE ms1.PeakFamilies
(
    PeakFamilyID SERIAL NOT NULL,
    PeaksFileID INT NULL,
    Scan INT NULL,
    MZ DOUBLE PRECISION NULL,
    Charge SMALLINT NULL,

    CONSTRAINT PK_PeakFamilies PRIMARY KEY (PeakFamilyID)
);

CREATE INDEX IDX_PeakFamilies_Scans ON ms1.PeakFamilies(PeaksFileID,Scan);

CREATE TABLE ms1.Peaks
(
    PeakID SERIAL NOT NULL,
    PeaksFileID INT NOT NULL,
    Scan INT NOT NULL,
    MZ DOUBLE PRECISION NULL,
    Frequency DOUBLE PRECISION NULL,
    Amplitude DOUBLE PRECISION NULL,
    Phase DOUBLE PRECISION NULL,
    Decay DOUBLE PRECISION NULL,
    Error DOUBLE PRECISION NULL,
    Area DOUBLE PRECISION NULL,

    CONSTRAINT PK_Peaks PRIMARY KEY (PeakID),
    CONSTRAINT FK_Peaks_Scans FOREIGN KEY (PeaksFileID,Scan) REFERENCES ms1.Scans(PeaksFileID,Scan)
);

CREATE INDEX IDX_Peaks_Scans ON ms1.Peaks(PeaksFileID,Scan);

CREATE TABLE ms1.PeaksToFamilies
(
    PeakID INT NOT NULL,
    PeakFamilyID INT NOT NULL,

    CONSTRAINT PK_PeaksToFamilies PRIMARY KEY (PeakID,PeakFamilyID),
    CONSTRAINT FK_PeaksToFamilies_Peaks FOREIGN KEY (PeakID) REFERENCES ms1.Peaks(PeakID),
    CONSTRAINT FK_PeaksToFamilies_PeakFamilies FOREIGN KEY (PeakFamilyID) REFERENCES ms1.PeakFamilies(PeakFamilyID)
);

CREATE TABLE ms1.FeaturesFiles
(
	FeaturesFileID SERIAL NOT NULL,
    ExpDataFileID INT NOT NULL,
    MzXmlURL VARCHAR(800) NULL,

    CONSTRAINT PK_FeaturesFiles PRIMARY KEY (FeaturesFileID)
);

CREATE INDEX IDX_FeaturesFiles_ExpData ON ms1.FeaturesFiles(ExpDataFileID);

CREATE TABLE ms1.Features
(
    FeatureID SERIAL NOT NULL,
	FeaturesFileID INT,
    Scan INT NULL,
    Time DOUBLE PRECISION NULL,
    MZ DOUBLE PRECISION NULL,
    AccurateMZ BIT NULL,
    Mass DOUBLE PRECISION NULL,
    Intensity DOUBLE PRECISION NULL,
    Charge SMALLINT NULL,
    ChargeStates SMALLINT NULL,
    KL DOUBLE PRECISION NULL,
    Background DOUBLE PRECISION NULL,
    Median DOUBLE PRECISION NULL,
    Peaks SMALLINT NULL,
    ScanFirst INT NULL,
    ScanLast INT NULL,
    ScanCount INT NULL,
    TotalIntensity DOUBLE PRECISION NULL,
    Description VARCHAR(300) NULL,

    /* extra cols for ceaders-sinai */
    MS2Scan INT NULL,
    MS2ConnectivityProbability DOUBLE PRECISION NULL,

    CONSTRAINT PK_Features PRIMARY KEY (FeatureID),
    CONSTRAINT FK_Features_FeaturesFiles FOREIGN KEY (FeaturesFileID) REFERENCES ms1.FeaturesFiles(FeaturesFileID)
);

CREATE INDEX IDX_Features_FeaturesFiles ON ms1.Features(FeaturesFileID);