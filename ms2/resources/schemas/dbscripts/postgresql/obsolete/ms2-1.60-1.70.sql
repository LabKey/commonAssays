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
UPDATE ms2.ms2proteingroups SET pctspectrumids = pctspectrumids / 100, percentcoverage = percentcoverage / 100;

-- Previous to CPAS 1.5, some runs ended up with PeptideCount = 0 & SpectrumCount = 0; this corrects those runs.

/* 7/7/08: COMMENT OUT THIS UPDATE AS PART OF VIEW REFACTORING
UPDATE ms2.MS2Runs SET
    PeptideCount = (SELECT COUNT(*) AS PepCount FROM ms2.MS2Peptides pep WHERE pep.run = ms2.MS2Runs.run),
    SpectrumCount = (SELECT COUNT(*) AS SpecCount FROM ms2.MS2Spectra spec WHERE spec.run = ms2.MS2Runs.run)
WHERE (PeptideCount = 0);
*/

-- Index to speed up deletes from MS2PeptidesData
CREATE INDEX IX_MS2PeptideMemberships_PeptideId ON ms2.MS2PeptideMemberships(PeptideId);

-- Simplify MS2 table names
ALTER TABLE ms2.MS2Fractions RENAME TO Fractions;
ALTER TABLE ms2.MS2History RENAME TO History;
ALTER TABLE ms2.MS2Modifications RENAME TO Modifications;
ALTER TABLE ms2.MS2PeptideMemberships RENAME TO PeptideMemberships;
ALTER TABLE ms2.MS2PeptidesData RENAME TO PeptidesData;
ALTER TABLE ms2.MS2ProteinGroupMemberships RENAME TO ProteinGroupMemberships;
ALTER TABLE ms2.MS2ProteinGroups RENAME TO ProteinGroups;
ALTER TABLE ms2.MS2ProteinProphetFiles RENAME TO ProteinProphetFiles;
ALTER TABLE ms2.MS2Runs RENAME TO Runs;
ALTER TABLE ms2.MS2SpectraData RENAME TO SpectraData;

-- More accurate column name
ALTER TABLE ms2.Runs RENAME COLUMN SampleEnzyme TO SearchEnzyme;

-- Bug 2195 restructure prot.FastaSequences
    ALTER TABLE prot.FastaSequences RENAME TO FastaSequences_old;

CREATE TABLE prot.FastaSequences
(
    FastaId int NOT NULL,
    LookupString varchar (200) NOT NULL,
    SeqId int NULL
);

INSERT INTO prot.FastaSequences (FastaId, LookupString, SeqId)
    SELECT FastaId, LookupString,SeqId FROM prot.FastaSequences_old ORDER BY FastaId, LookupString;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT PK_FastaSequences PRIMARY KEY (FastaId,LookupString);
ALTER TABLE prot.FastaSequences ADD CONSTRAINT FK_FastaSequences_Sequences FOREIGN KEY (SeqId) REFERENCES prot.Sequences (SeqId);
CREATE INDEX IX_FastaSequences_FastaId_SeqId ON prot.FastaSequences(FastaId, SeqId);
CREATE INDEX IX_FastaSequences_SeqId ON prot.FastaSequences(SeqId);
DROP TABLE prot.FastaSequences_old;

--Bug 2193
CREATE INDEX IX_SequencesSource ON prot.Sequences(SourceId);
-- different name on pgsql
DROP INDEX prot.ix_protsequences_hash;
