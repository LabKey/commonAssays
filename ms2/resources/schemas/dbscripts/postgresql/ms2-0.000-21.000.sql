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

/**** Runs                                           */
CREATE TABLE ms2.Runs
(
    _ts TIMESTAMP DEFAULT now(),
    Run SERIAL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,
    EntityId ENTITYID NOT NULL,
    Description VARCHAR(300),
    Path VARCHAR(500),
    FileName VARCHAR(300),
    Status VARCHAR(200),
    StatusId INT NOT NULL DEFAULT 0,
    Type VARCHAR(30),
    SearchEngine VARCHAR(20),
    MassSpecType VARCHAR(200),
    SearchEnzyme VARCHAR(50),
    Deleted BOOLEAN NOT NULL DEFAULT '0',
    ExperimentRunLSID LSIDType NULL,
    HasPeptideProphet BOOLEAN NOT NULL DEFAULT '0',
    PeptideCount INT NOT NULL DEFAULT 0,    -- Store peptide and spectrum counts with each run to make computing stats much faster
    SpectrumCount INT NOT NULL DEFAULT 0,
    NegativeHitCount INT NOT NULL DEFAULT 0,
    
    CONSTRAINT PK_MS2Runs PRIMARY KEY (Run)
);

-- Create indexes on ms2 Runs table to support common operations in MS2Manager

CREATE INDEX MS2Runs_Stats ON ms2.Runs(PeptideCount, SpectrumCount, Deleted, StatusId);
CREATE INDEX MS2Runs_ExperimentRunLSID ON ms2.Runs(ExperimentRunLSID);
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container);

UPDATE ms2.Runs SET Type = 'LegacyComet' WHERE Type = 'Comet';

ALTER TABLE ms2.Runs ADD COLUMN MascotFile VARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD COLUMN DistillerRawFile VARCHAR(500) NULL;

-- Issue 27667 - Importing failed due to data type length limitation
ALTER TABLE ms2.Runs ALTER COLUMN mascotfile TYPE TEXT;

CREATE TABLE ms2.Fractions
(
    Fraction SERIAL,
    Run INT NOT NULL,
    Description VARCHAR(300),
    FileName VARCHAR(300),
    HydroB0 REAL,
    HydroB1 REAL,
    HydroR2 REAL,
    HydroSigma REAL,
    PepXmlDataLSID LSIDType NULL,
    MzXmlURL VARCHAR(400) NULL,

    CONSTRAINT PK_MS2Fractions PRIMARY KEY (Fraction)
);

CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);
CREATE INDEX IX_Fractions_MzXMLURL ON ms2.fractions(mzxmlurl);

ALTER TABLE ms2.Fractions ADD COLUMN ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS1ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS2ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS3ScanCount INT;
ALTER TABLE ms2.Fractions ADD COLUMN MS4ScanCount INT;


CREATE TABLE ms2.Modifications
(
    Run INT NOT NULL,
    AminoAcid VARCHAR (1) NOT NULL,
    MassDiff REAL NOT NULL,
    Variable BOOLEAN NOT NULL,
    Symbol VARCHAR (1) NOT NULL,

    CONSTRAINT PK_MS2Modifications PRIMARY KEY (Run, AminoAcid, Symbol)
);

-- Store MS2 Spectrum data in separate table to improve performance of upload and MS2Peptides queries
CREATE TABLE ms2.SpectraData
(
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Spectrum BYTEA NOT NULL,

    CONSTRAINT PK_MS2SpectraData PRIMARY KEY (Fraction, Scan)
);

CREATE TABLE ms2.PeptidesData
(
    RowId BIGSERIAL NOT NULL,
    Fraction INT NOT NULL,
    Scan INT NOT NULL,
    Charge SMALLINT NOT NULL,
    Score1 REAL NOT NULL DEFAULT 0,
    Score2 REAL NOT NULL DEFAULT 0,
    Score3 REAL NOT NULL DEFAULT 0,
    Score4 REAL NULL,
    Score5 REAL NULL,
    IonPercent REAL NOT NULL,
    Mass FLOAT8 NOT NULL,
    DeltaMass REAL NOT NULL,
    PeptideProphet REAL NOT NULL,
    Peptide VARCHAR (200) NOT NULL,
    PrevAA CHAR(1) NOT NULL DEFAULT '',
    TrimmedPeptide VARCHAR(200) NOT NULL DEFAULT '',
    NextAA CHAR(1) NOT NULL DEFAULT '',
    ProteinHits SMALLINT NOT NULL,
    SequencePosition INT NOT NULL DEFAULT 0,
    Protein VARCHAR(100) NOT NULL,
    SeqId INT NULL,
    RetentionTime REAL NULL,
    PeptideProphetErrorRate REAL NULL,
    EndScan INT NULL,

    CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId)
);

CREATE INDEX IX_MS2PeptidesData_Protein ON ms2.PeptidesData (Protein);
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId);
CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON ms2.PeptidesData(TrimmedPeptide);
CREATE INDEX IX_MS2PeptidesData_Peptide ON ms2.PeptidesData(Peptide);

ALTER TABLE ms2.peptidesdata ALTER score1 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score1 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score2 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score2 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score3 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score3 DROP DEFAULT;

ALTER TABLE ms2.PeptidesData ADD COLUMN score6 REAL;

ALTER TABLE ms2.PeptidesData ADD COLUMN QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD COLUMN HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD COLUMN Decoy boolean NOT NULL DEFAULT FALSE;

ALTER TABLE ms2.PeptidesData ALTER COLUMN PeptideProphet DROP NOT NULL;

ALTER TABLE ms2.peptidesdata
  ADD CONSTRAINT FK_ms2PeptidesData_ProtSequences FOREIGN KEY (seqid) REFERENCES prot.sequences (seqid);

CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy, QueryNumber);

CREATE TABLE ms2.ProteinProphetFiles
(
    RowId SERIAL NOT NULL,
    FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    UploadCompleted BOOLEAN NOT NULL DEFAULT '0',
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,
    PredictedNumberCorrectSeries BYTEA NULL,
    PredictedNumberIncorrectSeries BYTEA NULL,

    CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
);

ALTER TABLE ms2.proteinprophetfiles ALTER COLUMN FilePath TYPE VARCHAR(512);

CREATE TABLE ms2.ProteinGroups
(
    RowId SERIAL NOT NULL,
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
    CONSTRAINT UQ_MS2ProteinGroups UNIQUE (ProteinProphetFileId, GroupNumber, IndistinguishableCollectionId),
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
    PeptideId int8 NOT NULL,
    ProteinGroupId int4 NOT NULL,
    NSPAdjustedProbability float4 NOT NULL,
    Weight float4 NOT NULL,
    NondegenerateEvidence bool NOT NULL,
    EnzymaticTermini int4 NOT NULL,
    SiblingPeptides float4 NOT NULL,
    SiblingPeptidesBin int4 NOT NULL,
    Instances int4 NOT NULL,
    ContributingEvidence bool NOT NULL,
    CalcNeutralPepMass float4 NOT NULL,

    CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
    CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.PeptidesData (rowid),
    CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ProteinGroups (rowid)
);

-- Index to speed up deletes from PeptidesData
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.PeptideMemberships(PeptideId);

CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId);

CREATE TABLE ms2.Quantitation
(
    PeptideId BIGINT NOT NULL,
    LightFirstscan INT NOT NULL,
    LightLastscan INT NOT NULL,
    LightMass REAL NOT NULL,
    HeavyFirstscan INT NOT NULL,
    HeavyLastscan INT NOT NULL,
    HeavyMass REAL NOT NULL,
    Ratio VARCHAR(20) NULL,                -- q3 does not generate string representations of ratios
    Heavy2lightRatio VARCHAR(20) NULL,     -- q3 does not generate string representations of ratios.
    LightArea REAL NOT NULL,
    HeavyArea REAL NOT NULL,
    DecimalRatio REAL NOT NULL,
    QuantId INT NOT NULL,                  -- QuantId must be non-null; eventually (PeptideId, QuantId) should become a compound PK

    CONSTRAINT PK_Quantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.PeptidesData(RowId)
);

ALTER TABLE ms2.Quantitation ADD COLUMN Invalidated BOOLEAN;

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

CREATE TABLE ms2.PeptideProphetSummaries
(
    Run INT NOT NULL,
    FValSeries BYTEA NULL,
    ObsSeries1 BYTEA NULL,
    ObsSeries2 BYTEA NULL,
    ObsSeries3 BYTEA NULL,
    ModelPosSeries1 BYTEA NULL,
    ModelPosSeries2 BYTEA NULL,
    ModelPosSeries3 BYTEA NULL,
    ModelNegSeries1 BYTEA NULL,
    ModelNegSeries2 BYTEA NULL,
    ModelNegSeries3 BYTEA NULL,
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,

    CONSTRAINT PK_PeptideProphetSummmaries PRIMARY KEY (Run)
);

-- Add a quantitation summary table
CREATE TABLE ms2.QuantSummaries
(
    QuantId SERIAL NOT NULL,
    Run INTEGER NOT NULL,
    AnalysisType VARCHAR(20) NOT NULL,
    AnalysisTime TIMESTAMP NULL,
    Version VARCHAR(80) NULL,
    LabeledResidues VARCHAR(20) NULL,
    MassDiff VARCHAR(80) NULL,
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
);

ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass1 TO AbsoluteIntensity1;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass2 TO AbsoluteIntensity2;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass3 TO AbsoluteIntensity3;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass4 TO AbsoluteIntensity4;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass5 TO AbsoluteIntensity5;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass6 TO AbsoluteIntensity6;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass7 TO AbsoluteIntensity7;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass8 TO AbsoluteIntensity8;

ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN TargetMass9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN AbsoluteIntensity9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN Normalized9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN TargetMass10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN AbsoluteIntensity10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD COLUMN Normalized10 REAL;

CREATE TABLE ms2.iTraqProteinQuantitation
(
    ProteinGroupId BIGINT NOT NULL,
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
);

ALTER TABLE ms2.itraqproteinquantitation ADD COLUMN Ratio9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD COLUMN Error9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD COLUMN Ratio10 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD COLUMN Error10 REAL;

CREATE TABLE ms2.ExpressionData (
    RowId SERIAL,
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
