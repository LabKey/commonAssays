/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
SELECT core.fn_dropifexists('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId');
CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId);

SELECT core.fn_dropifexists('ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId');
CREATE INDEX IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId);

SELECT core.fn_dropifexists('Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction');
CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction);

ALTER TABLE ms2.ProteinGroups
    ADD COLUMN ErrorRate REAL NULL;

ALTER TABLE ms2.PeptidesData
    ADD COLUMN PeptideProphetErrorRate REAL NULL;

SELECT core.fn_dropifexists ('ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId')
;
SELECT core.fn_dropifexists ('PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId')
;
SELECT core.fn_dropifexists ('PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId')
;
SELECT core.fn_dropifexists ('ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId')
;
-- redundant after the restructure of the UQ constraint
SELECT core.fn_dropifexists ('ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId')
;

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
;
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
;

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)

;
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
;

-- make PPfileid the left-most column in the index so that results by run can be found
ALTER TABLE ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
;

ALTER TABLE ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
;

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences;

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid);

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval);

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource);

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId);

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident);

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid);

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid);

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId);

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid);

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId);

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container;

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO';
