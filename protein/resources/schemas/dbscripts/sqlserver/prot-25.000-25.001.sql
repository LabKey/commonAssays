-- These columns are unused and, due to data type differences, are problematic for SQL Server -> PostgreSQL migration
EXEC core.fn_dropifexists 'InfoSources', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'AnnotationTypes', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'IdentTypes', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'Organisms', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'Sequences', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'Identifiers', 'prot', 'COLUMN', 'Deleted';
EXEC core.fn_dropifexists 'Annotations', 'prot', 'COLUMN', 'Deleted';
