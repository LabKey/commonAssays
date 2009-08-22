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
    ALTER TABLE prot.FastaSequences RENAME TO FastaSequences_old;

    CREATE TABLE prot.FastaSequences (
        FastaId int NOT NULL ,
        LookupString varchar (200) NOT NULL ,
        SeqId int NULL
         );

    INSERT INTO prot.FastaSequences (FastaId, LookupString,SeqId)
    SELECT FastaId, LookupString,SeqId FROM prot.FastaSequences_old ORDER BY FastaId, LookupString;

    ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY (FastaId,LookupString);
    ALTER TABLE prot.FastaSequences ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId);
    CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId);
    CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId);
    DROP TABLE prot.FastaSequences_old;

    --Bug 2193
    CREATE  INDEX IX_SequencesSource ON prot.Sequences(SourceId);
    -- different name on pgsql    
    DROP INDEX prot.ix_protsequences_hash;


