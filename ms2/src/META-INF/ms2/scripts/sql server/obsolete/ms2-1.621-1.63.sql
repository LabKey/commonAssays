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

--Bug 2193
CREATE  INDEX IX_SequencesSource ON prot.Sequences(SourceId)
GO
if exists (select * from dbo.sysindexes where name = 'IX_SeqHash' and id = object_id('prot.Sequences'))
drop index prot.Sequences.IX_SeqHash
GO


