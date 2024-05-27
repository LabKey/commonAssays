/*
 * Copyright (c) 2019 LabKey Corporation
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

CREATE SCHEMA ms2;
GO

CREATE TABLE ms2.Runs
(
    -- standard fields
    _ts TIMESTAMP,
    Run INT IDENTITY(1, 1),
    CreatedBy USERID,
    Created DATETIME,
    ModifiedBy USERID,
    Modified DATETIME,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID DEFAULT NEWID(),
    Description NVARCHAR(300),
    Path NVARCHAR(500),
    FileName NVARCHAR(300),
    Status NVARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Type NVARCHAR(30),
    SearchEngine NVARCHAR(20),
    MassSpecType NVARCHAR(200),
    SearchEnzyme NVARCHAR(50),
    Deleted BIT NOT NULL DEFAULT 0,
    ExperimentRunLSID LSIDType NULL,
    HasPeptideProphet BIT NOT NULL DEFAULT '0',
    PeptideCount INT NOT NULL DEFAULT 0,
    SpectrumCount INT NOT NULL DEFAULT 0,
    NegativeHitCount INT NOT NULL DEFAULT 0,      -- Store reverse peptide counts to enable scoring analysis UI.

    CONSTRAINT PK_MS2Runs PRIMARY KEY (Run)
);

-- Create indexes on ms2 Runs table to support common operations in MS2Manager
CREATE INDEX MS2Runs_Stats ON ms2.Runs (PeptideCount, SpectrumCount, Deleted, StatusId);
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID);
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

UPDATE ms2.Runs SET Type = 'LegacyComet' WHERE Type = 'Comet';

ALTER TABLE ms2.Runs ADD MascotFile NVARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD DistillerRawFile NVARCHAR(500) NULL;

-- Issue 27667 - Importing failed due to data type length limitation
ALTER TABLE ms2.Runs ALTER COLUMN mascotfile NVARCHAR(MAX);

CREATE TABLE ms2.Fractions
(
    Fraction INT IDENTITY(1,1),
    Run INT NOT NULL,
    Description NVARCHAR(300),
    FileName NVARCHAR(300),
    HydroB0 REAL,
    HydroB1 REAL,
    HydroR2 REAL,
    HydroSigma REAL,
    PepXmlDataLSID LSIDType NULL,
    MzXmlURL VARCHAR(400) NULL,

    CONSTRAINT PK_MS2Fractions PRIMARY KEY (Fraction)
);

CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run, Fraction);
CREATE INDEX IX_Fractions_MzXMLURL ON ms2.fractions(mzxmlurl);

ALTER TABLE ms2.Fractions ADD ScanCount INT
ALTER TABLE ms2.Fractions ADD MS1ScanCount INT
ALTER TABLE ms2.Fractions ADD MS2ScanCount INT
ALTER TABLE ms2.Fractions ADD MS3ScanCount INT
ALTER TABLE ms2.Fractions ADD MS4ScanCount INT

UPDATE ms2.fractions SET MzXmlUrl = 'file:///' + substring(MzXmlUrl, 7, 400) WHERE MzXmlUrl LIKE 'file:/_%' AND MzXmlUrl NOT LIKE 'file:///%' AND MzXmlUrl IS NOT NULL;

CREATE TABLE ms2.Modifications
(
    Run INT NOT NULL,
    AminoAcid VARCHAR (1) NOT NULL,
    MassDiff REAL NOT NULL,
    Variable BIT NOT NULL,
    Symbol VARCHAR (1) NOT NULL,

    CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
);

-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE ms2.SpectraData
(
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Spectrum IMAGE NOT NULL,

    CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
);

CREATE TABLE ms2.PeptidesData
(
    RowId BIGINT IDENTITY (1, 1) NOT NULL,
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Charge TINYINT NOT NULL,
    Score1 REAL NULL,
    Score2 REAL NULL,
    Score3 REAL NULL,
    Score4 REAL NULL,
    Score5 REAL NULL,
    IonPercent REAL NOT NULL,
    Mass FLOAT NOT NULL,    -- Store mass as high-precision real
    DeltaMass REAL NOT NULL,
    PeptideProphet REAL NOT NULL,
    Peptide VARCHAR (200) NOT NULL,
    PrevAA CHAR(1) NOT NULL DEFAULT '',
    TrimmedPeptide VARCHAR(200) NOT NULL DEFAULT '',
    NextAA CHAR(1) NOT NULL DEFAULT '',
    ProteinHits SMALLINT NOT NULL,
    SequencePosition INT NOT NULL DEFAULT 0,
    Protein VARCHAR (100) NOT NULL,
    SeqId INT NULL,
    RetentionTime REAL NULL,
    PeptideProphetErrorRate REAL NULL,
    EndScan INT NULL,

    CONSTRAINT PK_MS2PeptidesData PRIMARY KEY NONCLUSTERED (RowId)
);

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.PeptidesData(Protein);

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId);

CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON ms2.PeptidesData(TrimmedPeptide);
CREATE INDEX IX_MS2PeptidesData_Peptide ON ms2.PeptidesData(Peptide);

ALTER TABLE ms2.PeptidesData ADD score6 REAL;
ALTER TABLE ms2.PeptidesData ADD QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD Decoy bit NOT NULL DEFAULT 0;

ALTER TABLE ms2.PeptidesData ALTER COLUMN PeptideProphet REAL NULL;

ALTER TABLE ms2.peptidesdata
  ADD CONSTRAINT FK_ms2PeptidesData_ProtSequences FOREIGN KEY (seqid) REFERENCES prot.sequences (seqid);

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy, QueryNumber);

CREATE TABLE ms2.PeptideProphetSummaries
(
    Run INT NOT NULL,
    FValSeries IMAGE NULL,
    ObsSeries1 IMAGE NULL,
    ObsSeries2 IMAGE NULL,
    ObsSeries3 IMAGE NULL,
    ModelPosSeries1 IMAGE NULL,
    ModelPosSeries2 IMAGE NULL,
    ModelPosSeries3 IMAGE NULL,
    ModelNegSeries1 IMAGE NULL,
    ModelNegSeries2 IMAGE NULL,
    ModelNegSeries3 IMAGE NULL,
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (Run)
);

CREATE TABLE ms2.ProteinProphetFiles
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    UploadCompleted BIT DEFAULT 0 NOT NULL,
    MinProbSeries IMAGE NULL,
    SensitivitySeries IMAGE NULL,
    ErrorSeries IMAGE NULL,
    PredictedNumberCorrectSeries IMAGE NULL,
    PredictedNumberIncorrectSeries IMAGE NULL,

    CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
);

ALTER TABLE ms2.proteinprophetfiles ALTER COLUMN FilePath NVARCHAR(512);

CREATE TABLE ms2.ProteinGroups
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    GroupProbability REAL NOT NULL,
    ProteinProphetFileId INT NOT NULL,
    GroupNumber INT NOT NULL,
    IndistinguishableCollectionId INT NOT NULL,
    UniquePeptidesCount INT NOT NULL,
    TotalNumberPeptides INT NOT NULL,
    PctSpectrumIds REAL NULL,
    PercentCoverage REAL NULL,
    ProteinProbability REAL NOT NULL DEFAULT 0,
    ErrorRate REAL NULL,

    CONSTRAINT PK_MS2ProteinGroups PRIMARY KEY (RowId),
    CONSTRAINT UQ_MS2ProteinGroups UNIQUE NONCLUSTERED (ProteinProphetFileId, GroupNumber, IndistinguishableCollectionId),
    CONSTRAINT FK_MS2ProteinGroup_MS2ProteinProphetFileId FOREIGN KEY (ProteinProphetFileId) REFERENCES ms2.ProteinProphetFiles(RowId)
);

CREATE TABLE ms2.ProteinGroupMemberships
(
    ProteinGroupId INT NOT NULL,
    SeqId INT NOT NULL,
    Probability REAL NOT NULL,

    CONSTRAINT PK_MS2ProteinGroupMemberships PRIMARY KEY (ProteinGroupId, SeqId),
    CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId),
    CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups (RowId)
);

CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability);

CREATE TABLE ms2.PeptideMemberships
(
    PeptideId BIGINT NOT NULL,
    ProteinGroupId INT NOT NULL,
    NSPAdjustedProbability REAL NOT NULL,
    Weight REAL NOT NULL,
    NondegenerateEvidence BIT NOT NULL,
    EnzymaticTermini INT NOT NULL,
    SiblingPeptides REAL NOT NULL,
    SiblingPeptidesBin INT NOT NULL,
    Instances INT NOT NULL,
    ContributingEvidence BIT NOT NULL,
    CalcNeutralPepMass REAL NOT NULL,

    CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
    CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.PeptidesData (rowid),
    CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ProteinGroups (rowid)
);

-- Index to speed up deletes from MS2PeptidesData.
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.PeptideMemberships(PeptideId);
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)

CREATE TABLE ms2.Quantitation
(
    PeptideId BIGINT NOT NULL,
    LightFirstscan INT NOT NULL,
    LightLastscan INT NOT NULL,
    LightMass REAL NOT NULL,
    HeavyFirstscan INT NOT NULL,
    HeavyLastscan INT NOT NULL,
    HeavyMass REAL NOT NULL,
    Ratio VARCHAR(20) NULL,             -- q3 does not generate string representations of ratios
    Heavy2lightRatio VARCHAR(20) NULL,  -- q3 does not generate string representations of ratios
    LightArea REAL NOT NULL,
    HeavyArea REAL NOT NULL,
    DecimalRatio REAL NOT NULL,
    QuantId INT NOT NULL,               -- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK

    CONSTRAINT PK_Quantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
);

ALTER TABLE ms2.Quantitation ADD Invalidated BIT;
GO

CREATE TABLE ms2.ProteinQuantitation
(
    ProteinGroupId INT NOT NULL,
    RatioMean REAL NOT NULL,
    RatioStandardDev REAL NOT NULL,
    RatioNumberPeptides INT NOT NULL,
    Heavy2LightRatioMean REAL NOT NULL,
    Heavy2LightRatioStandardDev REAL NOT NULL,

    CONSTRAINT PK_ProteinQuantitation PRIMARY KEY (ProteinGroupId),
    CONSTRAINT FK_ProteinQuantitation_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups (RowId)
);

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId);

-- Add a quantitation summary table
CREATE TABLE ms2.QuantSummaries
(
    QuantId INT IDENTITY(1,1) NOT NULL,
    Run INT NOT NULL,
    AnalysisType NVARCHAR(20) NOT NULL,
    AnalysisTime DATETIME NULL,
    Version NVARCHAR(80) NULL,
    LabeledResidues NVARCHAR(20) NULL,
    MassDiff NVARCHAR(80) NULL,
    MassTol REAL NOT NULL,
    SameScanRange CHAR(1) NULL,
    XpressLight INT NULL,

    CONSTRAINT PK_QuantSummaries PRIMARY KEY (QuantId),
    CONSTRAINT FK_QuantSummaries_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.Runs (Run),
    -- Current restrictions allow only one quantitation analysis per run
    CONSTRAINT UQ_QuantSummaries UNIQUE (Run)
);

CREATE TABLE ms2.PeptideProphetData
(
    PeptideId BIGINT NOT NULL,
    ProphetFVal REAL NOT NULL,
    ProphetDeltaMass REAL NULL,
    ProphetNumTrypticTerm INT NULL,
    ProphetNumMissedCleav INT NULL,

    CONSTRAINT PK_PeptideProphetData PRIMARY KEY (PeptideId),
    CONSTRAINT FK_PeptideProphetData_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
);

UPDATE exp.DataInput SET Role = 'Spectra' WHERE Role = 'mzXML';

GO

CREATE TABLE ms2.iTraqPeptideQuantitation
(
    PeptideId BIGINT NOT NULL,
    TargetMass1 REAL,
    AbsoluteMass1 REAL,
    Normalized1 REAL,
    TargetMass2 REAL,
    AbsoluteMass2 REAL,
    Normalized2 REAL,
    TargetMass3 REAL,
    AbsoluteMass3 REAL,
    Normalized3 REAL,
    TargetMass4 REAL,
    AbsoluteMass4 REAL,
    Normalized4 REAL,
    TargetMass5 REAL,
    AbsoluteMass5 REAL,
    Normalized5 REAL,
    TargetMass6 REAL,
    AbsoluteMass6 REAL,
    Normalized6 REAL,
    TargetMass7 REAL,
    AbsoluteMass7 REAL,
    Normalized7 REAL,
    TargetMass8 REAL,
    AbsoluteMass8 REAL,
    Normalized8 REAL,

    CONSTRAINT PK_iTraqPeptideQuantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_iTraqPeptideQuantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
)
GO

EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass1', 'AbsoluteIntensity1', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass2', 'AbsoluteIntensity2', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass3', 'AbsoluteIntensity3', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass4', 'AbsoluteIntensity4', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass5', 'AbsoluteIntensity5', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass6', 'AbsoluteIntensity6', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass7', 'AbsoluteIntensity7', 'COLUMN'
EXEC sp_rename 'ms2.iTraqPeptideQuantitation.AbsoluteMass8', 'AbsoluteIntensity8', 'COLUMN'
GO

ALTER TABLE ms2.itraqpeptidequantitation ADD TargetMass9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD AbsoluteIntensity9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD Normalized9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD TargetMass10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD AbsoluteIntensity10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD Normalized10 REAL;

CREATE TABLE ms2.iTraqProteinQuantitation
(
    ProteinGroupId INT NOT NULL,
    Ratio1 REAL,
    Error1 REAL,
    Ratio2 REAL,
    Error2 REAL,
    Ratio3 REAL,
    Error3 REAL,
    Ratio4 REAL,
    Error4 REAL,
    Ratio5 REAL,
    Error5 REAL,
    Ratio6 REAL,
    Error6 REAL,
    Ratio7 REAL,
    Error7 REAL,
    Ratio8 REAL,
    Error8 REAL,

    CONSTRAINT PK_iTraqProteinQuantitation PRIMARY KEY (ProteinGroupId),
    CONSTRAINT FK_iTraqProteinQuantitation_ProteinGroups FOREIGN KEY (ProteinGroupId) REFERENCES ms2.ProteinGroups(RowId)
)
GO

ALTER TABLE ms2.itraqproteinquantitation ADD Ratio9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Error9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Ratio10 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Error10 REAL;

CREATE TABLE ms2.ExpressionData (
    RowId INT IDENTITY (1,1) NOT NULL,
    Value REAL,
    SeqId INT NOT NULL,
    SampleId INT NOT NULL,
    DataId INT NOT NULL,

    CONSTRAINT PK_ExpressionData PRIMARY KEY (RowId),
    CONSTRAINT FK_ExpressionData_SeqId FOREIGN KEY (SeqId) REFERENCES prot.sequences (SeqId),
    CONSTRAINT FK_ExpressionData_SampleId FOREIGN KEY (SampleId) REFERENCES exp.material (RowId),
    CONSTRAINT FK_ExpressionData_DataId FOREIGN KEY (DataId) REFERENCES exp.data (RowId)
);

CREATE INDEX IX_ExpressionData_SeqId ON ms2.ExpressionData(SeqId);
CREATE INDEX IX_ExpressionData_SampleId ON ms2.ExpressionData(SampleId);

ALTER TABLE ms2.ExpressionData ADD CONSTRAINT UQ_ExpressionData_DataId_SeqId_SampleId UNIQUE (DataId, SeqId, SampleId);

CREATE TABLE ms2.FastaRunMapping (
    Run INT NOT NULL,
    FastaId INT NOT NULL,

    CONSTRAINT PK_FastaRunMapping PRIMARY KEY (Run, FastaId),
    CONSTRAINT FK_FastaRunMapping_Run FOREIGN KEY (Run) REFERENCES ms2.Runs (Run),
    CONSTRAINT FK_FastaRunMapping_FastaId FOREIGN KEY (FastaId) REFERENCES prot.FastaFiles (FastaId)
);

CREATE INDEX IX_FastaRunMapping_FastaId ON ms2.FastaRunMapping(FastaId);
