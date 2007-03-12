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


