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

-- Use fn_dropifexists to make drop GO indexes function more reliable
CREATE OR REPLACE FUNCTION prot.drop_go_indexes() RETURNS void AS $$
    BEGIN
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Constraint', 'pk_goterm');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_Name');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'IX_GoTerm_TermType');
        PERFORM core.fn_dropifexists('goterm', 'prot', 'Index', 'UQ_GoTerm_Acc');

        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Constraint', 'pk_goterm2term');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term2Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_term1_2_Id');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'IX_GoTerm2Term_relationshipTypeId');
        PERFORM core.fn_dropifexists('goterm2term', 'prot', 'Index', 'UQ_GoTerm2Term_1_2_R');

        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Constraint', 'pk_gographpath');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term2Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_term1_2_Id');
        PERFORM core.fn_dropifexists('gographpath', 'prot', 'Index', 'IX_GoGraphPath_t1_distance');

        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'IX_GoTermDefinition_dbXrefId');
        PERFORM core.fn_dropifexists('gotermdefinition', 'prot', 'Index', 'UQ_GoTermDefinition_termId');

        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_SynonymTypeId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_TermId');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'IX_GoTermSynonym_termSynonym');
        PERFORM core.fn_dropifexists('gotermsynonym', 'prot', 'Index', 'UQ_GoTermSynonym_termId_termSynonym');
    END;
	$$ LANGUAGE plpgsql;
