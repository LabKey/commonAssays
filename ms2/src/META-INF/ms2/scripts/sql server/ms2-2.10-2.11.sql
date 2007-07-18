DROP VIEW ms2.SimplePeptides
DROP VIEW ms2.Peptides
GO

CREATE VIEW ms2.SimplePeptides AS SELECT
    frac.Run, run.Description AS RunDescription, pep.Fraction, LEFT(frac.FileName, CHARINDEX('.', frac.FileName) - 1) AS FractionName, Scan,
    RetentionTime, Charge, Score1 As RawScore, Score2 As DiffScore, Score3 As ZScore, Score1 As SpScore, Score2 As DeltaCn, Score3 As XCorr, Score4 As SpRank, Score1 AS OrigScore,
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

CREATE PROCEDURE core.Rename_Primary_Key(@SchemaName VARCHAR(255), @TableName VARCHAR(255)) AS
    DECLARE @name VARCHAR(200)
    DECLARE @sql VARCHAR(4000)

    SELECT @name = so.name FROM sysobjects so INNER JOIN sysobjects parent ON parent.id = so.parent_obj WHERE so.xtype = 'PK' AND parent.xtype = 'U' AND parent.name = @TableName
    SELECT @sql = 'EXEC sp_rename ''' + @SchemaName + '.' + @name + ''', ''PK_' + @TableName + ''', ''OBJECT'''
    EXEC sp_sqlexec @sql
GO

-- Replace auto-generated primary key names with standard names
EXEC core.Rename_Primary_Key 'prot', 'GoTerm'
EXEC core.Rename_Primary_Key 'prot', 'GoTerm2Term'
EXEC core.Rename_Primary_Key 'prot', 'GoGraphPath'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotationSet'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotation'
GO

DROP PROCEDURE core.Rename_Primary_Key
GO

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE PROCEDURE prot.drop_go_indexes AS
    ALTER TABLE prot.goterm DROP CONSTRAINT PK_GoTerm
    DROP INDEX prot.GoTerm.IX_GoTerm_Name
    DROP INDEX prot.GoTerm.IX_GoTerm_TermType
    DROP INDEX prot.GoTerm.UQ_GoTerm_Acc

    ALTER TABLE prot.goterm2term DROP CONSTRAINT PK_GoTerm2Term
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term2Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1_2_Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_relationshipTypeId
    DROP INDEX prot.goterm2term.UQ_GoTerm2Term_1_2_R

    ALTER TABLE prot.gographpath DROP CONSTRAINT PK_GoGraphPath
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term2Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1_2_Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_t1_distance

    DROP INDEX prot.GoTermDefinition.IX_GoTermDefinition_dbXrefId
    DROP INDEX prot.GoTermDefinition.UQ_GoTermDefinition_termId

    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_SynonymTypeId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_TermId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_termSynonym
    DROP INDEX prot.GoTermSynonym.UQ_GoTermSynonym_termId_termSynonym
GO

CREATE PROCEDURE prot.create_go_indexes AS
    ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name)
    CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype)
    CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc)

    ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id)
    CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id)
    CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id)
    CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId)
    CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId)

    ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id)
    CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id)
    CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id)
    CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id)
    CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance)

    CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId)
    CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId)

    CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId)
    CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId)
    CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym)
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym)
GO
