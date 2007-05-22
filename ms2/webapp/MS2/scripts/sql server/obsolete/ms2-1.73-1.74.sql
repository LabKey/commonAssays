ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences
GO

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid)
GO

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval)
GO

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource)
GO

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId)
GO

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident)
GO

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid)
GO

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid)
GO

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId)
GO

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid)
GO

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId)
GO
