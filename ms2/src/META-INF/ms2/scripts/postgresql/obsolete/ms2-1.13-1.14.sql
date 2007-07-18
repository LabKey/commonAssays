-- Add a column for the hash of the file

ALTER TABLE prot.proteindatabases
    ADD COLUMN FileChecksum varchar(50) NULL;
