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
exec core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
go
	create index IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId)
go

exec core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId'
go
	create index IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId)
go

exec core.fn_dropifexists 'Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction'
go
	create index IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction)
go

ALTER TABLE ms2.ProteinGroups
    ADD ErrorRate REAL NULL
GO

ALTER TABLE ms2.PeptidesData
    ADD PeptideProphetErrorRate REAL NULL
GO

DROP VIEW ms2.SimplePeptides
DROP VIEW ms2.Peptides
DROP VIEW ms2.ProteinGroupsWithQuantitation
GO

CREATE VIEW ms2.SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, LEFT(frac.FileName, CHARINDEX('.', frac.FileName) - 1) AS FractionName, Scan,
    RetentionTime, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank,
    Score1 As Hyper, Score2 As Next, Score3 As B, Score4 As Y, Score5 As Expect, Score1 As Ion, Score2 As "Identity", Score3 AS Homology,
    IonPercent, pep.Mass, DeltaMass, (pep.Mass + DeltaMass) AS PrecursorMass, ABS(DeltaMass - ROUND(DeltaMass, 0)) AS FractionalDeltaMass,
    CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * ABS(DeltaMass - ROUND(DeltaMass, 0)) / (pep.Mass + (Charge - 1) * 1.007276)) END AS FractionalDeltaMassPPM,
	CASE WHEN pep.Mass = 0 THEN 0 ELSE ABS(1000000 * DeltaMass / (pep.Mass + (Charge - 1) * 1.007276)) END AS DeltaMassPPM,
    CASE WHEN Charge = 0 THEN 0 ELSE (pep.Mass + DeltaMass + (Charge - 1) * 1.007276) / Charge END AS MZ, PeptideProphet, PeptideProphetErrorRate, Peptide, ProteinHits,
    Protein, PrevAA, TrimmedPeptide, NextAA, LTRIM(RTRIM(PrevAA + TrimmedPeptide + NextAA)) AS StrippedPeptide,	SequencePosition, pep.SeqId, pep.RowId,
    quant.DecimalRatio, quant.Heavy2LightRatio, quant.HeavyArea, quant.HeavyFirstScan, quant.HeavyLastScan, quant.HeavyMass, quant.LightArea, quant.LightFirstScan, quant.LightLastScan, quant.LightMass, quant.Ratio,
    proph.ProphetFVal, proph.ProphetDeltaMass, proph.ProphetNumTrypticTerm, proph.ProphetNumMissedCleav
    FROM ms2.PeptidesData pep
        INNER JOIN
            ms2.Fractions frac ON pep.Fraction = frac.Fraction
        INNER JOIN
            ms2.Runs run ON frac.Run = run.Run
    LEFT JOIN ms2.quantitation quant ON pep.rowid=quant.peptideid
    LEFT JOIN ms2.peptideprophetdata proph ON pep.rowid=proph.peptideid
GO

CREATE VIEW ms2.Peptides AS
    SELECT pep.*, seq.description, seq.bestgenename AS genename
    FROM ms2.SimplePeptides pep
    LEFT JOIN prot.sequences seq ON seq.seqid = pep.seqid
GO

CREATE VIEW ms2.ProteinGroupsWithQuantitation AS
    SELECT * FROM ms2.ProteinGroups LEFT JOIN ms2.ProteinQuantitation ON ProteinGroupId = RowId
GO
exec core.fn_dropifexists 'ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId'
go
exec core.fn_dropifexists 'PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId'
go
exec core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId'
go
exec core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
go
-- redundant after the restructure of the UQ constraint
exec core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId'
go

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
go
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
go

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)

go
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
GO

-- make PPfileid the left-most column in the index so that results by run can be found
alter table ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
go

alter table ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE  NONCLUSTERED
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
go

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences
GO

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid)
GO

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval)
GO

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource)
GO

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId)
GO

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident)
GO

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid)
GO

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid)
GO

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId)
GO

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid)
GO

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId)
GO

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container
GO

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO'
GO
