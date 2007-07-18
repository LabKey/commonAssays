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

