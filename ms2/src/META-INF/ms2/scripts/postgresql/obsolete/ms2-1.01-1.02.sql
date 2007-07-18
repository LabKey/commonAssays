SET search_path TO prot,ms2,public;

CREATE INDEX ix_protorganisms_genus ON prot.protorganisms(genus);
CREATE INDEX ix_protorganisms_species ON prot.protorganisms(species);

