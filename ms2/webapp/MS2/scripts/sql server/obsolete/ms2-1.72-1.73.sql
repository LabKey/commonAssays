exec core.fn_dropifexists 'ProteinQuantitation', 'ms2', 'INDEX', 'IX_ProteinQuantitation_ProteinGroupId'
go
exec core.fn_dropifexists 'PeptideMemberships', 'ms2', 'INDEX', 'IX_Peptidemembership_ProteingroupId'
go
exec core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'IX_PeptidesData_SeqId'
go
exec core.fn_dropifexists 'ProteinGroupMemberships', 'ms2', 'INDEX', 'IX_ProteinGroupMemberships_SeqId'
go
-- redundant after the restructure of the UQ constraint
exec core.fn_dropifexists 'ProteinGroups', 'ms2', 'INDEX', 'ProteinGroups.IX_ProteinGroups_ProteinProphetFileId'
go


CREATE INDEX IX_ProteinQuantitation_ProteinGroupId ON ms2.ProteinQuantitation(ProteinGroupId)
go
CREATE INDEX IX_Peptidemembership_ProteingroupId ON ms2.PeptideMemberships(ProteinGroupId)
go

-- this is not a declared fk
CREATE INDEX IX_PeptidesData_SeqId ON ms2.PeptidesData(SeqId)

go
CREATE  INDEX IX_ProteinGroupMemberships_SeqId ON ms2.ProteinGroupMemberships(SeqId, ProteinGroupId, Probability)
GO

-- make PPfileid the left-most column in the index so that results by run can be found
alter table ms2.ProteinGroups drop constraint UQ_MS2ProteinGroups
go

alter table ms2.ProteinGroups ADD CONSTRAINT UQ_MS2ProteinGroups UNIQUE  NONCLUSTERED
	(
		ProteinProphetFileId,
		GroupNumber,
		IndistinguishableCollectionId
	)
go

