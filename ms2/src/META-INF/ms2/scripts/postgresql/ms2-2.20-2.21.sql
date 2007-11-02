select core.fn_dropifexists('ProteinGroupMembers', 'cabig', 'VIEW', NULL);
select core.fn_dropifexists('protsequences', 'cabig', 'VIEW', NULL);

ALTER TABLE prot.organisms ALTER COLUMN CommonName TYPE varchar(100);