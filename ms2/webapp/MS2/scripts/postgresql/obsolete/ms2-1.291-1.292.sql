-- Index to speed up determining which SeqIds came from a given FASTA file (e.g., MS2 showAllProteins.view)
CREATE INDEX IX_ProteinSequences_SeqId ON prot.ProteinSequences(SeqId);

-- Update the sequence stored in prot.ProtSequences with the sequence from prot.ProteinSequences
-- in cases where the ProtSequences one was stored incorrectly

-- For performance, first create a SeqId -> SequenceId temporary lookup table
CREATE TABLE prot._collapseseqids (SeqId INT NOT NULL PRIMARY KEY, SequenceId INT);

INSERT INTO prot._collapseseqids
	SELECT SeqId, MIN(SequenceId)
		FROM prot.ProteinSequences
		WHERE SeqId IS NOT NULL
		GROUP BY SeqId
		ORDER BY SeqId;

-- Update the "bad" sequences
UPDATE prot.ProtSequences
    SET ProtSequence = ps.Sequence
    FROM prot.ProteinSequences ps INNER JOIN prot._collapseseqids c
        ON (ps.SequenceId = c.SequenceId)
    WHERE prot.ProtSequences.SeqId = c.SeqId
        AND prot.ProtSequences.ProtSequence SIMILAR TO '%[^A-Za-z]%';

-- Drop the temporary table
DROP TABLE prot._collapseseqids;
