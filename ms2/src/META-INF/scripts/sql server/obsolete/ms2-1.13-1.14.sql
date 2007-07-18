-- Add a column for the hash of the file

ALTER TABLE prot.proteindatabases ADD
    FileChecksum varchar(50) NULL;
