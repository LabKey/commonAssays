/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- This index overlaps with ix_annotations_annotident
DROP INDEX prot.ix_annotations_identid;
-- This index overlaps with uq_customannotationset
DROP INDEX prot.ix_customannotationset_container;
-- This index overlaps with ix_gographpath_term1_2_id and IX_GoGraphPath_t1_distance
DROP INDEX prot.ix_gographpath_term1id;
-- This index overlaps with uq_goterm2term_1_2_r
DROP INDEX prot.ix_goterm2term_term1_2_id;
-- This index overlaps with uq_goterm2term_1_2_r
DROP INDEX prot.ix_goterm2term_term1id;
-- This index overlaps with uq_gotermsynonym_termid_termsynonym
DROP INDEX prot.ix_gotermsynonym_termid;

-- Create functions to drop & create all GO indexes. This helps with load performance.
CREATE OR REPLACE FUNCTION prot.create_go_indexes() RETURNS void AS $$
BEGIN
    ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
    CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name);
    CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype);
    CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc);

    ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
    CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
    CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
    CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId);

    ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);
    CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
    CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
    CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

    CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId);
    CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId);

    CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
    CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION prot.drop_go_indexes() RETURNS void AS $$
BEGIN
    ALTER TABLE prot.goterm DROP CONSTRAINT IF EXISTS pk_goterm;
    DROP INDEX IF EXISTS prot.IX_GoTerm_Name;
    DROP INDEX IF EXISTS prot.IX_GoTerm_TermType;
    DROP INDEX IF EXISTS prot.UQ_GoTerm_Acc;

    ALTER TABLE prot.goterm2term DROP CONSTRAINT IF EXISTS pk_goterm2term;
    DROP INDEX IF EXISTS prot.IX_GoTerm2Term_term2Id;
    DROP INDEX IF EXISTS prot.IX_GoTerm2Term_relationshipTypeId;
    DROP INDEX IF EXISTS prot.UQ_GoTerm2Term_1_2_R;

    ALTER TABLE prot.gographpath DROP CONSTRAINT IF EXISTS pk_gographpath;
    DROP INDEX IF EXISTS prot.IX_GoGraphPath_term2Id;
    DROP INDEX IF EXISTS prot.IX_GoGraphPath_term1_2_Id;
    DROP INDEX IF EXISTS prot.IX_GoGraphPath_t1_distance;

    DROP INDEX IF EXISTS prot.IX_GoTermDefinition_dbXrefId;
    DROP INDEX IF EXISTS prot.UQ_GoTermDefinition_termId;

    DROP INDEX IF EXISTS prot.IX_GoTermSynonym_SynonymTypeId;
    DROP INDEX IF EXISTS prot.IX_GoTermSynonym_termSynonym;
    DROP INDEX IF EXISTS prot.UQ_GoTermSynonym_termId_termSynonym;
END;
$$ LANGUAGE plpgsql;
