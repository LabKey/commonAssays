/*
 * Copyright (c) 2007-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
-- Add Phenyx-specific scores
-- Replace auto-generated GO primary key names with standard names
ALTER TABLE prot.goterm DROP CONSTRAINT goterm_pkey;
ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
ALTER TABLE prot.goterm2term DROP CONSTRAINT goterm2term_pkey;
ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
ALTER TABLE prot.gographpath DROP CONSTRAINT gographpath_pkey;
ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);

-- -- Replace auto-generated custom annotation primary key names with standard names.  Dependent FK must be dropped and added.
ALTER TABLE prot.customannotation DROP CONSTRAINT FK_CustomAnnotation_CustomAnnotationSetId;
ALTER TABLE prot.customannotation DROP CONSTRAINT customannotation_pkey;
ALTER TABLE prot.customannotation ADD CONSTRAINT pk_customannotation PRIMARY KEY (CustomAnnotationId);
ALTER TABLE prot.customannotationset DROP CONSTRAINT customannotationset_pkey;
ALTER TABLE prot.customannotationset ADD CONSTRAINT pk_customannotationset PRIMARY KEY (CustomAnnotationSetId);
ALTER TABLE prot.customannotation ADD CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId);

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE FUNCTION prot.drop_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm DROP CONSTRAINT pk_goterm;
        DROP INDEX prot.IX_GoTerm_Name;
        DROP INDEX prot.IX_GoTerm_TermType;
        DROP INDEX prot.UQ_GoTerm_Acc;

        ALTER TABLE prot.goterm2term DROP CONSTRAINT pk_goterm2term;
        DROP INDEX prot.IX_GoTerm2Term_term1Id;
        DROP INDEX prot.IX_GoTerm2Term_term2Id;
        DROP INDEX prot.IX_GoTerm2Term_term1_2_Id;
        DROP INDEX prot.IX_GoTerm2Term_relationshipTypeId;
        DROP INDEX prot.UQ_GoTerm2Term_1_2_R;

        ALTER TABLE prot.gographpath DROP CONSTRAINT pk_gographpath;
        DROP INDEX prot.IX_GoGraphPath_term1Id;
        DROP INDEX prot.IX_GoGraphPath_term2Id;
        DROP INDEX prot.IX_GoGraphPath_term1_2_Id;
        DROP INDEX prot.IX_GoGraphPath_t1_distance;

        DROP INDEX prot.IX_GoTermDefinition_dbXrefId;
        DROP INDEX prot.UQ_GoTermDefinition_termId;

        DROP INDEX prot.IX_GoTermSynonym_SynonymTypeId;
        DROP INDEX prot.IX_GoTermSynonym_TermId;
        DROP INDEX prot.IX_GoTermSynonym_termSynonym;
        DROP INDEX prot.UQ_GoTermSynonym_termId_termSynonym;
    END;
	' LANGUAGE plpgsql;


CREATE FUNCTION prot.create_go_indexes() RETURNS void AS '
    BEGIN
        ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name);
        CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype);
        CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc);

        ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id);
        CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id);
        CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id);
        CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id);
        CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId);
        CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId);

        ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id);
        CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id);
        CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id);
        CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id);
        CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance);

        CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId);
        CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId);

        CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId);
        CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId);
        CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym);
        CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym);
    END;
    ' LANGUAGE plpgsql;
