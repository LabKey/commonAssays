-- Rename the randomly assigned PK names to simply 'PK_<tablename>'
CREATE PROCEDURE ChangeBogusIndexName(@owner AS VARCHAR(20), @type AS VARCHAR(10), @tableName AS VARCHAR(100)) AS
	DECLARE @bogusName varchar(200)
	SET @bogusName = (SELECT name FROM sysobjects WHERE (name LIKE @type + '\_\_' + @tableName + '\_\_%' ESCAPE '\'))

    IF @bogusName IS NOT NULL
        BEGIN
    	    DECLARE @newName VARCHAR(200)
    	    SET @bogusName = @owner + '.' + @bogusName
	        SET @newName = @type + '_' + @tableName
    	    EXEC sp_rename @bogusName, @newName
	    END
GO

EXEC ChangeBogusIndexName 'dbo', 'PK', 'ProteinDataBases'
EXEC ChangeBogusIndexName 'dbo', 'PK', 'ProteinSequences'
EXEC ChangeBogusIndexName 'dbo', 'PK', 'ProtSprotOrgMap'
EXEC ChangeBogusIndexName 'exp', 'PK', 'DataInput'
GO

DROP PROCEDURE ChangeBogusIndexName
GO


-- Rename an object, failing silently
CREATE PROCEDURE RenameObject(@oldName AS VARCHAR(200), @newName AS VARCHAR(200)) AS
    IF OBJECT_ID(@oldName) IS NOT NULL
        EXEC sp_rename @oldName, @newName
GO

-- Rename all PKs to 'PK_<tableName>' convention
EXEC RenameObject 'exp.PK__ProtocolApplication', 'PK_ProtocolApplication'
EXEC RenameObject 'MS2Fractions_PK', 'PK_MS2Fractions'
EXEC RenameObject 'MS2History_PK', 'PK_MS2History'
EXEC RenameObject 'MS2Modifications_PK', 'PK_MS2Modifications'
EXEC RenameObject 'MS2Peptides_PK', 'PK_MS2Peptides'
EXEC RenameObject 'MS2Runs_PK', 'PK_MS2Runs'
EXEC RenameObject 'MS2SpectraData_PK', 'PK_MS2SpectraData'
GO


-- Rename ProteinSequences UNIQUE CONSTRAINT
EXEC RenameObject 'ProteinSequences_AK', 'UQ_ProteinSequences_DataBaseId_LookupString'
GO


-- Add underlines to protein FK names
EXEC RenameObject 'FKProtAnnotationsProtIdentifiers', 'FK_ProtAnnotations_ProtIdentifiers'
EXEC RenameObject 'FKProtAnnotationsProtSeqSources', 'FK_ProtAnnotations_ProtSeqSources'
EXEC RenameObject 'FKProtAnnotationsProtSequences', 'FK_ProtAnnotations_ProtSequences'
EXEC RenameObject 'FKProtIdentifiersProtIdentTypes', 'FK_ProtIdentifiers_ProtIdentTypes'
EXEC RenameObject 'FKProtIdentifiersProtSeqSources', 'FK_ProtIdentifiers_ProtSeqSources'
EXEC RenameObject 'FKProtIdentifiersProtSequences', 'FK_ProtIdentifiers_ProtSequences'
EXEC RenameObject 'FKProtSequencesOrganism', 'FK_ProtSequences_Organism'
EXEC RenameObject 'FKProtSequencesProtSeqSources', 'FK_ProtSequences_ProtSeqSources'
GO


DROP PROCEDURE RenameObject
GO


-- Now rename IX indexes: MS2PeptidesData_Protein, protindexes... but how?
-- For example, 'MS2PeptidesData_Protein' to 'IX_MS2PeptidesData_Protein'
-- All exp.IDX_ indexes to exp.IX_

IF OBJECT_ID('ProteinDBs', 'V') IS NOT NULL
	DROP VIEW ProteinDBs
IF OBJECT_ID('Proteins', 'V') IS NOT NULL
	DROP VIEW Proteins
IF OBJECT_ID('Runs', 'V') IS NOT NULL
	DROP VIEW Runs
GO

-- ProteinDataBases with number of runs
CREATE VIEW ProteinDBs AS
	SELECT ProteinDataBases.ProteinDataBase, ProteinDataBases.DataBaseId, ProteinDataBases.Loaded, X.Runs
	FROM ProteinDataBases LEFT OUTER JOIN
		(SELECT DataBaseId, COUNT(Run) AS Runs
		FROM MS2Runs
		GROUP BY DataBaseId) X ON X.DataBaseId = ProteinDataBases.DataBaseId
GO
