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
-- Index to speed up determining which SeqIds came from a given FASTA file (e.g., MS2 showAllProteins.view)
CREATE INDEX IX_ProteinSequences_SeqId ON prot.ProteinSequences(SeqId)
GO

-- Update the sequence stored in prot.ProtSequences with the sequence from prot.ProteinSequences
-- in cases where the ProtSequences one was stored incorrectly

-- For performance, first create a SeqId -> SequenceId temporary lookup table
CREATE TABLE prot._collapseseqids (SeqId INT NOT NULL PRIMARY KEY, SequenceId INT)
GO

INSERT INTO prot._collapseseqids
	SELECT SeqId, MIN(SequenceId)
		FROM prot.ProteinSequences
		WHERE SeqId IS NOT NULL
		GROUP BY SeqId
		ORDER BY SeqId
GO

-- Update the "bad" sequences
UPDATE prot.ProtSequences
    SET prot.ProtSequences.ProtSequence = ps.Sequence
    FROM prot.ProteinSequences ps INNER JOIN prot._collapseseqids c
        ON (ps.SequenceId = c.SequenceId)
    WHERE prot.ProtSequences.SeqId = c.SeqId
        AND prot.ProtSequences.ProtSequence LIKE '%[^A-Za-z]%'
GO

-- Drop the temporary table
DROP TABLE prot._collapseseqids
GO
