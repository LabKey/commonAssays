select core.fn_dropifexists('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId');
create index IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId);

select core.fn_dropifexists('ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId');
create index IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId);

select core.fn_dropifexists('Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction');
create index IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);

ALTER TABLE ms2.ProteinGroups
    ADD COLUMN ErrorRate REAL NULL;

ALTER TABLE ms2.PeptidesData
    ADD COLUMN PeptideProphetErrorRate REAL NULL;

DROP VIEW ms2.Peptides;
DROP VIEW ms2.SimplePeptides;
DROP VIEW ms2.ProteinGroupsWithQuantitation;

CREATE VIEW ms2.SimplePeptides AS
    SELECT frac.run, run.description AS rundescription, pep.fraction, substring(frac.filename, 1, position('.' in frac.filename)-1) as fractionname, pep.scan,
    pep.retentiontime, pep.charge, pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
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
        END AS mz, pep.peptideprophet, pep.PeptideProphetErrorRate, pep.peptide, pep.proteinhits, pep.protein, pep.prevaa, pep.trimmedpeptide, pep.nextaa, ltrim(rtrim((pep.prevaa::text || pep.trimmedpeptide::text) || pep.nextaa::text)) AS strippedpeptide, pep.sequenceposition, pep.seqid, pep.rowid,
        quant.DecimalRatio, quant.Heavy2LightRatio, quant.HeavyArea, quant.HeavyFirstScan, quant.HeavyLastScan, quant.HeavyMass, quant.LightArea, quant.LightFirstScan, quant.LightLastScan, quant.LightMass, quant.Ratio,
        proph.ProphetFVal, proph.ProphetDeltaMass, proph.ProphetNumTrypticTerm, proph.ProphetNumMissedCleav
    FROM ms2.PeptidesData pep
    JOIN ms2.Fractions frac ON pep.Fraction = frac.Fraction
    JOIN ms2.Runs run ON frac.Run = run.Run
    LEFT JOIN ms2.Quantitation quant ON pep.rowid=quant.peptideid
    LEFT JOIN ms2.PeptideProphetdata proph ON pep.rowid=proph.peptideid;

CREATE VIEW ms2.Peptides AS
    SELECT pep.*, seq.Description, seq.BestGeneName AS GeneName
    FROM ms2.SimplePeptides pep
    LEFT JOIN prot.Sequences seq ON seq.SeqId = pep.SeqId;

CREATE VIEW ms2.ProteinGroupsWithQuantitation AS
    SELECT * FROM ms2.ProteinGroups LEFT JOIN ms2.ProteinQuantitation ON ProteinGroupId = RowId;
SELECT core.fn_dropifexists ('ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId')
;
SELECT core.fn_dropifexists ('PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId')
;
SELECT core.fn_dropifexists ('PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId')
;
SELECT core.fn_dropifexists ('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId')
;
-- redundant after the restructure of the UQ constraint
SELECT core.fn_dropifexists ('ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId')
;

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
;
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
;

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)

;
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
;

-- make PPfileid the left-most column in the index so that results by run can be found
alter table ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
;

alter table ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
;

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid);

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource);

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident);

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid);

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container;

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO';
