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
UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100
GO

-- Previous to CPAS 1.5, some runs ended up with PeptideCount = 0 & SpectrumCount = 0; this corrects those runs.
-- Use old names here to allow running this easily on 1.6 installations.

/* 7/7/08: COMMENT OUT THIS UPDATE AS PART OF VIEW REFACTORING
UPDATE ms2.MS2Runs SET
    PeptideCount = (SELECT COUNT(*) AS PepCount FROM ms2.MS2Peptides pep WHERE pep.run = ms2.MS2Runs.run),
    SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM ms2.MS2Spectra spec WHERE spec.run = ms2.MS2Runs.run)
WHERE (PeptideCount = 0)
*/

-- Index to speed up deletes from MS2PeptidesData.  Use old names here to allow running this on 1.6 installations.
IF NOT EXISTS (SELECT * FROM dbo.sysindexes WHERE name = 'IX_MS2PeptideMemberships_PeptideId' AND id = object_id('ms2.MS2PeptideMemberships'))
    CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.MS2PeptideMemberships(PeptideId)
GO

-- Simplify MS2 table names
EXEC sp_rename 'ms2.MS2Fractions', 'Fractions'
EXEC sp_rename 'ms2.MS2History', 'History'
EXEC sp_rename 'ms2.MS2Modifications', 'Modifications'
EXEC sp_rename 'ms2.MS2PeptideMemberships', 'PeptideMemberships'
EXEC sp_rename 'ms2.MS2PeptidesData', 'PeptidesData'
EXEC sp_rename 'ms2.MS2ProteinGroupMemberships', 'ProteinGroupMemberships'
EXEC sp_rename 'ms2.MS2ProteinGroups', 'ProteinGroups'
EXEC sp_rename 'ms2.MS2ProteinProphetFiles', 'ProteinProphetFiles'
EXEC sp_rename 'ms2.MS2Runs', 'Runs'
EXEC sp_rename 'ms2.MS2SpectraData', 'SpectraData'
GO

-- Rebuild all views to point to the right table names
-- More accurate column name
EXEC sp_rename
    @objname = 'ms2.Runs.SampleEnzyme',
    @newname = 'SearchEnzyme',
    @objtype = 'COLUMN'
GO

-- Rebuild view
-- Bug 2195 restructure prot.FastaSequences
set nocount on
go
declare @errsave int
select @errsave=0
exec sp_rename 'prot.FastaSequences', 'FastaSequences_old'

CREATE TABLE prot.FastaSequences (
    FastaId int NOT NULL ,
    LookupString varchar (200) NOT NULL ,
    SeqId int NULL
     )
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

INSERT INTO prot.FastaSequences (FastaId, LookupString,SeqId)
SELECT FastaId, LookupString,SeqId FROM prot.FastaSequences_old ORDER BY FastaId, LookupString
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY CLUSTERED(FastaId,LookupString)
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

ALTER TABLE prot.FastaSequences WITH NOCHECK ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId)
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId)
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId)
select @errsave = CASE WHEN @@ERROR >0 THEN @@ERROR ELSE @errsave END

if (@errsave =0)
    DROP TABLE prot.FastaSequences_old
go

set nocount off
go

--Bug 2193
CREATE  INDEX IX_SequencesSource ON prot.Sequences(SourceId)
GO
if exists (select * from dbo.sysindexes where name = 'IX_SeqHash' and id = object_id('prot.Sequences'))
drop index prot.Sequences.IX_SeqHash
GO
