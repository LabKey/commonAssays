ALTER TABLE ms2.MS2ProteinProphetFiles DROP CONSTRAINT UQ_MS2ProteinProphetFiles
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ADD Run INT
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ADD Container EntityId
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ALTER COLUMN Run INT NOT NULL
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ALTER COLUMN Container EntityId NOT NULL
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run)
GO

ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run)
GO
