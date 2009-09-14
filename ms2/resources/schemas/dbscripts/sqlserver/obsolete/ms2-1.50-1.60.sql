/*
 * Copyright (c) 2006-2009 LabKey Corporation
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
-- Simplify protein table names
EXEC sp_rename 'prot.ProtAnnotations', 'Annotations'
EXEC sp_rename 'prot.ProtAnnotationTypes', 'AnnotationTypes'
EXEC sp_rename 'prot.ProtAnnotInsertions', 'AnnotInsertions'
EXEC sp_rename 'prot.ProteinDatabases', 'FastaFiles'
EXEC sp_rename 'prot.ProteinSequences', 'FastaSequences'
EXEC sp_rename 'prot.ProtFastas', 'FastaLoads'
EXEC sp_rename 'prot.ProtIdentifiers', 'Identifiers'
EXEC sp_rename 'prot.ProtIdentTypes', 'IdentTypes'
EXEC sp_rename 'prot.ProtInfoSources', 'InfoSources'
EXEC sp_rename 'prot.ProtOrganisms', 'Organisms'
EXEC sp_rename 'prot.ProtSequences', 'Sequences'
EXEC sp_rename 'prot.ProtSProtOrgMap', 'SProtOrgMap'
GO

-- Rename some columns
EXEC sp_rename 'prot.FastaFiles.DataBaseId', 'FastaId', 'COLUMN'
EXEC sp_rename 'prot.FastaFiles.ProteinDataBase', 'FileName', 'COLUMN'
EXEC sp_rename 'ms2.MS2Runs.DataBaseId', 'FastaId', 'COLUMN'
EXEC sp_rename 'prot.FastaSequences.DataBaseId', 'FastaId', 'COLUMN'
GO

-- Drop obsolete table, PK and columns
DROP TABLE prot.ProteinNames
GO

ALTER TABLE prot.FastaSequences DROP CONSTRAINT PK_ProteinSequences
GO

-- On very old CPAS installations, IX_ProteinSequence includes SequenceMass.  Only in that case,
-- rebuild the index with two columns so we can drop SequenceMass
DECLARE @idxid int
DECLARE @objid int
DECLARE @name varchar

SELECT @objid = object_id('prot.FastaSequences')
SELECT @idxid = indexproperty(@objid, 'IX_proteinsequences', 'IndexId')

IF (col_name(@objid, indexkey_property(@objid, @idxid, 3, 'ColumnId')) IS NOT NULL)
	BEGIN
		DROP INDEX prot.FastaSequences.IX_ProteinSequences
		CREATE INDEX IX_ProteinSequences ON prot.FastaSequences (FastaId, LookupString)
	END
GO

ALTER TABLE prot.FastaSequences DROP
    COLUMN SequenceId,
    COLUMN SequenceMass,
    COLUMN Sequence
GO

-- Add column for retention time
ALTER TABLE ms2.MS2PeptidesData
    ADD RetentionTime REAL NULL
GO
