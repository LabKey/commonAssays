/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
CREATE PROCEDURE core.Rename_Primary_Key(@SchemaName VARCHAR(255), @TableName VARCHAR(255)) AS
    DECLARE @name VARCHAR(200)
    DECLARE @sql VARCHAR(4000)

    SELECT @name = so.name FROM sysobjects so INNER JOIN sysobjects parent ON parent.id = so.parent_obj WHERE so.xtype = 'PK' AND parent.xtype = 'U' AND parent.name = @TableName
    SELECT @sql = 'EXEC sp_rename ''' + @SchemaName + '.' + @name + ''', ''PK_' + @TableName + ''', ''OBJECT'''
    EXEC sp_sqlexec @sql
GO

-- Replace auto-generated primary key names with standard names
EXEC core.Rename_Primary_Key 'prot', 'GoTerm'
EXEC core.Rename_Primary_Key 'prot', 'GoTerm2Term'
EXEC core.Rename_Primary_Key 'prot', 'GoGraphPath'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotationSet'
EXEC core.Rename_Primary_Key 'prot', 'CustomAnnotation'
GO

DROP PROCEDURE core.Rename_Primary_Key
GO

-- Create functions to drop & create all GO indexes.  This helps with load performance.
CREATE PROCEDURE prot.drop_go_indexes AS
    ALTER TABLE prot.goterm DROP CONSTRAINT PK_GoTerm
    DROP INDEX prot.GoTerm.IX_GoTerm_Name
    DROP INDEX prot.GoTerm.IX_GoTerm_TermType
    DROP INDEX prot.GoTerm.UQ_GoTerm_Acc

    ALTER TABLE prot.goterm2term DROP CONSTRAINT PK_GoTerm2Term
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term2Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_term1_2_Id
    DROP INDEX prot.goterm2term.IX_GoTerm2Term_relationshipTypeId
    DROP INDEX prot.goterm2term.UQ_GoTerm2Term_1_2_R

    ALTER TABLE prot.gographpath DROP CONSTRAINT PK_GoGraphPath
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term2Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_term1_2_Id
    DROP INDEX prot.gographpath.IX_GoGraphPath_t1_distance

    DROP INDEX prot.GoTermDefinition.IX_GoTermDefinition_dbXrefId
    DROP INDEX prot.GoTermDefinition.UQ_GoTermDefinition_termId

    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_SynonymTypeId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_TermId
    DROP INDEX prot.GoTermSynonym.IX_GoTermSynonym_termSynonym
    DROP INDEX prot.GoTermSynonym.UQ_GoTermSynonym_termId_termSynonym
GO

CREATE PROCEDURE prot.create_go_indexes AS
    ALTER TABLE prot.goterm ADD CONSTRAINT pk_goterm PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm_Name ON prot.GoTerm(name)
    CREATE INDEX IX_GoTerm_TermType ON prot.GoTerm(termtype)
    CREATE UNIQUE INDEX UQ_GoTerm_Acc ON prot.GoTerm(acc)

    ALTER TABLE prot.goterm2term ADD CONSTRAINT pk_goterm2term PRIMARY KEY (id)
    CREATE INDEX IX_GoTerm2Term_term1Id ON prot.GoTerm2Term(term1Id)
    CREATE INDEX IX_GoTerm2Term_term2Id ON prot.GoTerm2Term(term2Id)
    CREATE INDEX IX_GoTerm2Term_term1_2_Id ON prot.GoTerm2Term(term1Id,term2Id)
    CREATE INDEX IX_GoTerm2Term_relationshipTypeId ON prot.GoTerm2Term(relationshipTypeId)
    CREATE UNIQUE INDEX UQ_GoTerm2Term_1_2_R ON prot.GoTerm2Term(term1Id,term2Id,relationshipTypeId)

    ALTER TABLE prot.gographpath ADD CONSTRAINT pk_gographpath PRIMARY KEY (id)
    CREATE INDEX IX_GoGraphPath_term1Id ON prot.GoGraphPath(term1Id)
    CREATE INDEX IX_GoGraphPath_term2Id ON prot.GoGraphPath(term2Id)
    CREATE INDEX IX_GoGraphPath_term1_2_Id ON prot.GoGraphPath(term1Id,term2Id)
    CREATE INDEX IX_GoGraphPath_t1_distance ON prot.GoGraphPath(term1Id,distance)

    CREATE INDEX IX_GoTermDefinition_dbXrefId ON prot.GoTermDefinition(dbXrefId)
    CREATE UNIQUE INDEX UQ_GoTermDefinition_termId ON prot.GoTermDefinition(termId)

    CREATE INDEX IX_GoTermSynonym_SynonymTypeId ON prot.GoTermSynonym(synonymTypeId)
    CREATE INDEX IX_GoTermSynonym_TermId ON prot.GoTermSynonym(termId)
    CREATE INDEX IX_GoTermSynonym_termSynonym ON prot.GoTermSynonym(termSynonym)
    CREATE UNIQUE INDEX UQ_GoTermSynonym_termId_termSynonym ON prot.GoTermSynonym(termId,termSynonym)
GO
