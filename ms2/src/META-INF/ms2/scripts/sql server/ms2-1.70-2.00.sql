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
EXEC core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
GO
CREATE INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId)
GO

EXEC core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'IX_ProteinGroups_ProteinProphetFileId'
GO
CREATE INDEX IX_ProteinGroups_ProteinProphetFileId ON ms2.ProteinGroups(ProteinProphetFileId)
GO

EXEC core.fn_dropifexists 'Fractions', 'ms2', 'INDEX', 'IX_Fractions_Run_Fraction'
GO
CREATE INDEX IX_Fractions_Run_Fraction ON ms2.Fractions(Run,Fraction)
GO

ALTER TABLE ms2.ProteinGroups
    ADD ErrorRate REAL NULL
GO

ALTER TABLE ms2.PeptidesData
    ADD PeptideProphetErrorRate REAL NULL
GO

EXEC core.fn_dropifexists 'ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId'
GO
EXEC core.fn_dropifexists 'PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId'
GO
EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId'
GO
EXEC core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
GO
-- redundant after the restructure of the UQ constraint
EXEC core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId'
GO

CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
GO
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
GO

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)
GO
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
GO

-- make PPfileid the left-most column in the index so that results by run can be found
ALTER TABLE ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
GO

ALTER TABLE ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE  NONCLUSTERED
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
GO

ALTER TABLE prot.FastaSequences DROP CONSTRAINT pk_fastasequences
GO

ALTER TABLE prot.FastaSequences ADD CONSTRAINT pk_fastasequences PRIMARY KEY(lookupstring, fastaid)
GO

CREATE INDEX IX_Annotations_AnnotVal ON prot.Annotations(annotval)
GO

CREATE INDEX IX_FastaLoads_DBSource ON prot.fastaloads(dbsource)
GO

CREATE INDEX IX_AnnotationTypes_SourceId ON prot.annotationtypes(SourceId)
GO

CREATE INDEX IX_Annotations_AnnotIdent ON prot.annotations(annotident)
GO

CREATE INDEX IX_Annotations_annotsourceid ON prot.annotations(annotsourceid)
GO

CREATE INDEX IX_Identifiers_SourceId ON prot.Identifiers(sourceid)
GO

CREATE INDEX IX_Identifiers_SeqId ON prot.Identifiers(SeqId)
GO

CREATE INDEX IX_IdentTypes_cannonicalsourceid ON prot.IdentTypes(cannonicalsourceid)
GO

CREATE INDEX IX_Organisms_IdentId ON prot.Organisms(IdentId)
GO

ALTER TABLE ms2.ProteinProphetFiles DROP COLUMN Container
GO

UPDATE prot.InfoSources SET Url = 'http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?action=query&view=query&query={}'
    WHERE Name = 'GO'
GO
