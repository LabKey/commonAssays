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

-- Use fn_dropifexists to increase reliability
CREATE OR REPLACE FUNCTION prot.drop_go_indexes() RETURNS void AS $$
BEGIN
    PERFORM core.fn_dropifexists('goterm', 'prot', 'Constraint', 'pk_goterm');
    PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_Name');
    PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_TermType');
    PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'UQ_GoTerm_Acc');

    PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Constraint', 'pk_goterm2term');
    PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id');
    PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId');
    PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R');

    PERFORM core.fn_dropifexists('gographpath', 'prot', 'Constraint', 'pk_gographpath');
    PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id');
    PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id');
    PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance');

    PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId');
    PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId');

    PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId');
    PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym');
    PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym');
END;
$$ LANGUAGE plpgsql;
