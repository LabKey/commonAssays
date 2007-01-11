ALTER TABLE ms2.MS2ProteinProphetFiles DROP CONSTRAINT UQ_MS2ProteinProphetFiles;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN Run INT NOT NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN Container EntityId NOT NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run);
ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run);
