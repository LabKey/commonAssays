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

/* ms2-11.20-11.21.sql */

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

/* ms2-11.21-11.22.sql */

ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass1 TO AbsoluteIntensity1;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass2 TO AbsoluteIntensity2;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass3 TO AbsoluteIntensity3;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass4 TO AbsoluteIntensity4;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass5 TO AbsoluteIntensity5;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass6 TO AbsoluteIntensity6;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass7 TO AbsoluteIntensity7;
ALTER TABLE ms2.iTraqPeptideQuantitation RENAME COLUMN AbsoluteMass8 TO AbsoluteIntensity8;