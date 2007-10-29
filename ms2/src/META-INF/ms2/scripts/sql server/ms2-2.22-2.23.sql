/*
 * Copyright (c) 2007 LabKey Software, Inc.
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

-- Use fn_dropifexists to make GO index drop function more reliable
DROP PROCEDURE prot.drop_go_indexes
GO

CREATE PROCEDURE prot.drop_go_indexes AS
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Constraint', 'PK_GoTerm'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_Name'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'IX_GoTerm_TermType'
    EXEC core.fn_dropifexists 'goterm', 'prot', 'Index', 'UQ_GoTerm_Acc'

    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Constraint', 'PK_GoTerm2Term'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId'
    EXEC core.fn_dropifexists 'goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R'

    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Constraint', 'PK_GoGraphPath'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id'
    EXEC core.fn_dropifexists 'gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance'

    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId'
    EXEC core.fn_dropifexists 'gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId'

    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym'
    EXEC core.fn_dropifexists 'gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym'
GO
