-- These columns are unused and, due to data type differences, are problematic for SQL Server -> PostgreSQL migration
ALTER TABLE prot.InfoSources DROP COLUMN Deleted;
ALTER TABLE prot.AnnotationTypes DROP COLUMN Deleted;
ALTER TABLE prot.IdentTypes DROP COLUMN Deleted;
ALTER TABLE prot.Organisms DROP COLUMN Deleted;
ALTER TABLE prot.Sequences DROP COLUMN Deleted;
ALTER TABLE prot.Identifiers DROP COLUMN Deleted;
ALTER TABLE prot.Annotations DROP COLUMN Deleted;
