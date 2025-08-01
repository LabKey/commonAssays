-- Move foreign key to the correct table. Issue 53523.
ALTER TABLE prot.Organisms DROP CONSTRAINT FK_ProtSequences_ProtOrganisms;
ALTER TABLE prot.Sequences ADD CONSTRAINT FK_ProtSequences_ProtOrganisms FOREIGN KEY (OrgId) REFERENCES prot.Organisms (OrgId);
