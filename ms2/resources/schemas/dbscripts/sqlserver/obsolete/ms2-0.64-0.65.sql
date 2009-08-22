/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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
-- Rename IX index name, failing silently 
CREATE PROCEDURE RenameIndex(@owner AS VARCHAR(20), @tableName AS VARCHAR(100), @oldName AS VARCHAR(100), @newName AS VARCHAR(100)) AS
	IF (SELECT COUNT(*) FROM sysindexes WHERE NAME = @oldName) > 0
		BEGIN
			DECLARE @fullOldName VARCHAR(200)
			SET @fullOldName = @owner + '.' + @tableName + '.' + @oldName
			EXEC sp_rename @fullOldName, @newName
		END
GO


EXEC RenameIndex 'dbo', 'MS2PeptidesData', 'MS2PeptidesData_Protein', 'IX_MS2PeptidesData_Protein'
GO

DROP PROCEDURE RenameIndex
GO

-- Rename an object, failing silently
CREATE PROCEDURE RenameObject(@oldName AS VARCHAR(200), @newName AS VARCHAR(200)) AS
    IF OBJECT_ID(@oldName) IS NOT NULL
        EXEC sp_rename @oldName, @newName
GO

-- Fix a few PK & FK names
EXEC RenameObject 'PK_MS2Peptides', 'PK_MS2PeptidesData'
EXEC RenameObject 'PK_Organism', 'PK_ProtOrganisms'
EXEC RenameObject 'FK_ProtSequences_Organism', 'FK_ProtSequences_ProtOrganisms'
EXEC RenameObject 'FK_Organism_ProtIdentifiers', 'FK_ProtOrganisms_ProtIdentifiers'
GO

DROP PROCEDURE RenameObject
GO
