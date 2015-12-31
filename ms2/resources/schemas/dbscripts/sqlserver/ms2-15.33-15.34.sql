ALTER TABLE ms2.peptidesdata
  ADD CONSTRAINT FK_ms2PeptidesData_ProtSequences FOREIGN KEY (seqid) REFERENCES prot.sequences (seqid);
