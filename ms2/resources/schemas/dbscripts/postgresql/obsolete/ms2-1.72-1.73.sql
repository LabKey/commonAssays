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
alter table ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
;

alter table ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
;

