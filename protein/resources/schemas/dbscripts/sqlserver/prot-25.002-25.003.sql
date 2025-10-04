-- This index overlaps with IX_Annotations_AnnotIdent
DROP INDEX IX_Annotations_IdentId ON prot.Annotations;
-- This index overlaps with IX_GoGraphPath_term1_2_Id and IX_GoGraphPath_t1_distance
DROP INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath;
-- This index overlaps with UQ_CustomAnnotationSet
DROP INDEX IX_CustomAnnotationSet_Container ON prot.CustomAnnotationSet;
-- This index overlaps with UQ_GoTerm2Term_1_2_R
DROP INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term;
-- This index overlaps with UQ_GoTerm2Term_1_2_R
DROP INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term;
-- This index overlaps with UQ_GoTermSynonym_termId_termSynonym
DROP INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym;

GO

ALTER PROCEDURE prot.create_go_indexes AS
BEGIN
    ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name)
    CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype)
    CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc)

    ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id)
    CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId)
    CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId)

    ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id)
    CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id)
    CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id)
    CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance)

    CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId)
    CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId)

    CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId)
    CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym)
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
END

GO

ALTER PROCEDURE prot.drop_go_indexes AS
BEGIN
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Constraint', 'PK_GoTerm'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_Name'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_TermType'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'UQ_GoTerm_Acc'

    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Constraint', 'PK_GoTerm2Term'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R'

    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Constraint', 'PK_GoGraphPath'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance'

    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId'
    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId'

    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym';
END

GO
