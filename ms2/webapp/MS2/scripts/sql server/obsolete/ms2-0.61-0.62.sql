if NOT EXISTS (select * from systypes where name ='LSIDtype')
    exec sp_addtype 'LSIDtype', 'nvarchar(300)'
GO

ALTER TABLE dbo.MS2Runs ADD ApplicationLSID LSIDType NULL
GO

ALTER TABLE ProtAnnotations DROP CONSTRAINT FKProtAnnotationsProtSequences
GO
ALTER TABLE ProtAnnotations ADD CONSTRAINT FKProtAnnotationsProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId)
GO
ALTER TABLE ProtOrganisms DROP CONSTRAINT FK_Organism_ProtIdentifiers 
GO
ALTER TABLE ProtOrganisms ADD CONSTRAINT FK_Organism_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES ProtIdentifiers (IdentId) 
GO

ALTER TABLE MS2PeptidesData ADD SeqId INT NULL
GO