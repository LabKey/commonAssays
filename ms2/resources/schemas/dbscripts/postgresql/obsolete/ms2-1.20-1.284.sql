/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
-- add most common ncbi Taxonomy id's

CREATE TEMPORARY TABLE idents
(	Identifier varchar(50) not null,
	CommonName varchar(20) null,
	Genus varchar(100) not null,
	Species varchar(100) not null,
	OrgId int null,
	IdentId int null,
	IdentTypeId int null
)
;
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('chicken', 'Gallus', 'gallus', '9031');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('chimp', 'Pan', 'troglodytes', '9598');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('cow', 'Bos', 'taurus', '9913');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('dog', 'Canis', 'familiaris', '9615');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('ecoli', 'Escherichia', 'coli', '562');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('fruit fly', 'Drosophila', 'melanogaster', '7227');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('horse', 'Equus', 'caballus', '9796');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('human', 'Homo', 'sapiens', '9606');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('mouse', 'Mus', 'musculus', '10090');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('nematode', 'Caenorhabditis', 'elegans', '6239');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('pig', 'Sus', 'scrofa', '9823');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('rat', 'Rattus', 'norvegicus', '10116');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('yeast', 'Saccharomyces', 'cerevisiae', '4932');
INSERT INTO idents (CommonName, Genus, Species, Identifier)
	VALUES ('zebrafish', 'Danio', 'rerio', '7955');
;
UPDATE idents
	SET  IdentTypeId = (SELECT identtypeid FROM prot.ProtIdentTypes WHERE name='NCBI Taxonomy')
;
INSERT INTO prot.ProtOrganisms (CommonName, Genus, Species)
SELECT   CommonName, Genus, Species FROM idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtOrganisms PO INNER JOIN idents i ON (PO.Genus = i.Genus AND PO.Species = i.Species))
;
INSERT INTO prot.ProtIdentifiers (Identifier, IdentTypeId, entrydate)
	SELECT   Identifier, IdentTypeId , now() FROM idents
	WHERE NOT EXISTS
		(SELECT * FROM prot.ProtIdentifiers PI INNER JOIN idents i ON (PI.Identifier = i.Identifier AND PI.IdentTypeId = i.IdentTypeId))
;
UPDATE idents
	SET OrgId = PO.OrgId
	FROM prot.ProtOrganisms PO
	WHERE idents.Genus = PO.Genus AND idents.Species = PO.Species
;

UPDATE idents
	SET IdentId = PI.IdentId
	FROM prot.ProtIdentifiers PI
	WHERE idents.Identifier = PI.Identifier and idents.IdentTypeId = PI.IdentTypeId
;
UPDATE prot.ProtOrganisms
	SET IdentId = i.IdentID
	FROM idents i
	WHERE i.OrgId = prot.ProtOrganisms.OrgId
;
--SELECT i.*, PO.orgid, PO.IdentID, PI.IdentId
-- FROM idents i
--inner join prot.ProtOrganisms PO ON (i.genus = PO.genus and i.species = PO.species)
--inner join prot.ProtIdentifiers PI ON (i.Identifier = PI.Identifier AND i.IdentTypeID = PI.IdentTypeId)


drop table idents;

DROP VIEW ms2.MS2Peptides;

ALTER TABLE ms2.ms2peptidesdata
    ADD COLUMN RowId BIGSERIAL NOT NULL,
    ADD CONSTRAINT UQ_PeptidesData UNIQUE (RowId);

CREATE TABLE ms2.MS2ProteinProphetFiles
(
	RowId SERIAL NOT NULL,
	FilePath VARCHAR(255) NOT NULL,
    Run INT NOT NULL,
    Container EntityId NOT NULL,
    UploadCompleted BOOLEAN NOT NULL DEFAULT '0',
    MinProbSeries BYTEA NULL,
    SensitivitySeries BYTEA NULL,
    ErrorSeries BYTEA NULL,
    PredictedNumberCorrectSeries BYTEA NULL,
    PredictedNumberIncorrectSeries BYTEA NULL,


	CONSTRAINT PK_MS2ProteinProphetFiles PRIMARY KEY (RowId),
    CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run),
    CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
);

CREATE TABLE ms2.MS2ProteinGroups
(
	RowId SERIAL NOT NULL,
	GroupProbability REAL NOT NULL,
	ProteinProphetFileId INT NOT NULL,
	GroupNumber INT NOT NULL,
	IndistinguishableCollectionId INT NOT NULL,
	UniquePeptidesCount INT NOT NULL,
	TotalNumberPeptides INT NOT NULL,
	PctSpectrumIds REAL NOT NULL,
	PercentCoverage REAL NOT NULL,
    ProteinProbability REAL NOT NULL DEFAULT 0,

	CONSTRAINT PK_MS2ProteinGroups PRIMARY KEY (RowId),
	CONSTRAINT UQ_MS2ProteinGroups UNIQUE (GroupNumber, ProteinProphetFileId, IndistinguishableCollectionId),

	CONSTRAINT FK_MS2ProteinGroup_MS2ProteinProphetFileId FOREIGN KEY (ProteinProphetFileId) REFERENCES ms2.MS2ProteinProphetFiles(RowId)
);

CREATE TABLE ms2.MS2ProteinGroupMemberships
(
	ProteinGroupId INT NOT NULL,
	SeqId INT NOT NULL,
	Probability REAL NOT NULL,

	CONSTRAINT PK_MS2ProteinGroupMemberships PRIMARY KEY (ProteinGroupId, SeqId),
	CONSTRAINT FK_MS2ProteinGroupMemberships_ProtSequences  FOREIGN KEY (SeqId)       REFERENCES prot.ProtSequences (SeqId),
	CONSTRAINT FK_MS2ProteinGroupMembership_MS2ProteinGroups   FOREIGN KEY (ProteinGroupId)       REFERENCES ms2.MS2ProteinGroups (RowId)
);

CREATE TABLE ms2.ms2peptidememberships
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
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE ms2.Quantitation
(
	PeptideId BIGINT NOT NULL,
    LightFirstscan INT NOT NULL,
    LightLastscan INT NOT NULL,
    LightMass REAL NOT NULL,
    HeavyFirstscan INT NOT NULL,
    HeavyLastscan INT NOT NULL,
    HeavyMass REAL NOT NULL,
    Ratio VARCHAR(20) NOT NULL,
    Heavy2lightRatio VARCHAR(20) NOT NULL,
    LightArea REAL NOT NULL,
    HeavyArea REAL NOT NULL,
    DecimalRatio REAL NOT NULL,

    CONSTRAINT PK_Quantitation PRIMARY KEY (PeptideId),
    CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
);

CREATE INDEX IX_MS2PeptidesData_Fraction ON ms2.MS2PeptidesData(Fraction);

CREATE OR REPLACE VIEW ms2.ms2SimplePeptides AS
 SELECT frac.run, run.description AS rundescription, pep.fraction, pep.scan, pep.charge, pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
        CASE
            WHEN pep.mass = 0::double precision THEN 0::double precision
            ELSE abs(1000000::double precision * abs(pep.deltamass - round(pep.deltamass::double precision)) / (pep.mass + ((pep.charge - 1)::numeric * 1.007276)::double precision))
        END AS fractionaldeltamassppm,
        CASE
            WHEN pep.mass = 0::double precision THEN 0::double precision
            ELSE abs(1000000::double precision * pep.deltamass / (pep.mass + ((pep.charge - 1)::numeric * 1.007276)::double precision))
        END AS deltamassppm,
        CASE
            WHEN pep.charge = 0 THEN 0::double precision
            ELSE (pep.mass + pep.deltamass + ((pep.charge - 1)::numeric * 1.007276)::double precision) / pep.charge::double precision
        END AS mz, pep.peptideprophet, pep.peptide, pep.proteinhits, pep.protein, pep.prevaa, pep.trimmedpeptide, pep.nextaa, ltrim(rtrim((pep.prevaa::text || pep.trimmedpeptide::text) || pep.nextaa::text)) AS strippedpeptide, pep.sequenceposition, pep.seqid, pep.rowid, quant.*
   FROM ms2.ms2peptidesdata pep
   JOIN ms2.ms2fractions frac ON pep.fraction = frac.fraction
   JOIN ms2.ms2runs run ON frac.run = run.run
   LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid;

CREATE OR REPLACE VIEW ms2.ms2peptides AS
 SELECT pep.*, seq.description, seq.bestgenename AS genename
   FROM ms2.ms2SimplePeptides pep
   LEFT JOIN prot.protsequences seq ON seq.seqid = pep.seqid;

CREATE TABLE ms2.ProteinQuantitation
(
  ProteinGroupId INT NOT NULL,
  RatioMean REAL NOT NULL,
  RatioStandardDev REAL NOT NULL,
  RatioNumberPeptides INT NOT NULL,
  Heavy2LightRatioMean REAL NOT NULL,
  Heavy2LightRatioStandardDev REAL NOT NULL,
  CONSTRAINT PK_ProteinQuantitation PRIMARY KEY (ProteinGroupId),
  CONSTRAINT FK_ProteinQuantitation_ProteinGroup FOREIGN KEY (ProteinGroupId) REFERENCES ms2.MS2ProteinGroups (RowId)
);

CREATE VIEW ms2.ProteinGroupsWithQuantitation AS
  SELECT *
    FROM ms2.ms2proteingroups
    LEFT JOIN ms2.proteinquantitation ON ProteinGroupId = RowId;

ALTER TABLE ms2.MS2Runs
    ADD COLUMN HasPeptideProphet BOOLEAN NOT NULL DEFAULT '0';

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

DROP VIEW ms2.MS2ExperimentRuns;

-- No changes to this view, but we need to rebuild it since we added a column to MS2Runs
CREATE OR REPLACE VIEW ms2.MS2ExperimentRuns AS
SELECT ms2.MS2Runs.*, exp.ExperimentRun.RowId as ExperimentRunRowId, exp.Protocol.Name As ProtocolName
FROM ms2.MS2Runs
LEFT OUTER JOIN exp.ExperimentRun ON exp.ExperimentRun.LSID=ms2.MS2Runs.ExperimentRunLSID
LEFT OUTER JOIN exp.Protocol ON exp.Protocol.LSID=exp.ExperimentRun.ProtocolLSID;