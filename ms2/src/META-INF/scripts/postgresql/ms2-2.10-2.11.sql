-- Add Phenyx-specific scores
DROP VIEW ms2.Peptides;
DROP VIEW ms2.SimplePeptides;

CREATE VIEW ms2.SimplePeptides AS
    SELECT frac.run, run.description AS rundescription, pep.fraction, substring(frac.filename, 1, position('.' in frac.filename)-1) as fractionname, pep.scan, pep.retentiontime, pep.charge,
    pep.score1 AS rawscore, pep.score2 AS diffscore, pep.score3 AS zscore, pep.score1 AS spscore, pep.score2 AS deltacn, pep.score3 AS xcorr, pep.score4 AS sprank, pep.score1 AS hyper, pep.score2 AS "next", pep.score3 AS b, pep.score4 AS y, pep.score5 AS expect, pep.score1 AS ion, pep.score2 AS identity, pep.score3 AS homology, pep.score1 AS origscore,
    pep.ionpercent, pep.mass, pep.deltamass, pep.mass + pep.deltamass AS precursormass, abs(pep.deltamass - round(pep.deltamass::double precision)) AS fractionaldeltamass,
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

-- Replace auto-generated GO primary key names with standard names
ALTER TABLE prot.goterm DROP CONSTRAINT goterm_pkey;
ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
ALTER TABLE prot.goterm2term DROP CONSTRAINT goterm2term_pkey;
ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
ALTER TABLE prot.gographpath DROP CONSTRAINT gographpath_pkey;
ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);

-- -- Replace auto-generated custom annotation primary key names with standard names.  Dependent FK must be dropped and added.
ALTER TABLE prot.customannotation DROP CONSTRAINT FK_CustomAnnotation_CustomAnnotationSetId;
ALTER TABLE prot.customannotation DROP CONSTRAINT customannotation_pkey;
ALTER TABLE prot.customannotation ADD CONSTRAINT pk_customannotation PRIMARY KEY (CustomAnnotationId);
ALTER TABLE prot.customannotationset DROP CONSTRAINT customannotationset_pkey;
ALTER TABLE prot.customannotationset ADD CONSTRAINT pk_customannotationset PRIMARY KEY (CustomAnnotationSetId);
ALTER TABLE prot.customannotation ADD CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId);

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE FUNCTION prot.drop_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm DROP CONSTRAINT pk_goterm;
        DROP INDEX prot.IX_GoTerm_Name;
        DROP INDEX prot.IX_GoTerm_TermType;
        DROP INDEX prot.UQ_GoTerm_Acc;

        ALTER TABLE prot.goterm2term DROP CONSTRAINT pk_goterm2term;
        DROP INDEX prot.IX_GoTerm2Term_term1Id;
        DROP INDEX prot.IX_GoTerm2Term_term2Id;
        DROP INDEX prot.IX_GoTerm2Term_term1_2_Id;
        DROP INDEX prot.IX_GoTerm2Term_relationshipTypeId;
        DROP INDEX prot.UQ_GoTerm2Term_1_2_R;

        ALTER TABLE prot.gographpath DROP CONSTRAINT pk_gographpath;
        DROP INDEX prot.IX_GoGraphPath_term1Id;
        DROP INDEX prot.IX_GoGraphPath_term2Id;
        DROP INDEX prot.IX_GoGraphPath_term1_2_Id;
        DROP INDEX prot.IX_GoGraphPath_t1_distance;

        DROP INDEX prot.IX_GoTermDefinition_dbXrefId;
        DROP INDEX prot.UQ_GoTermDefinition_termId;

        DROP INDEX prot.IX_GoTermSynonym_SynonymTypeId;
        DROP INDEX prot.IX_GoTermSynonym_TermId;
        DROP INDEX prot.IX_GoTermSynonym_termSynonym;
        DROP INDEX prot.UQ_GoTermSynonym_termId_termSynonym;
    END;
	' LANGUAGE plpgsql;


CREATE FUNCTION prot.create_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name);
        CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype);
        CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc);

        ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id);
        CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
        CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id);
        CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
        CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId);

        ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);
        CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
        CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
        CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
        CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

        CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId);
        CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId);

        CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
        CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId);
        CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
        CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
    END;
    ' LANGUAGE plpgsql;
