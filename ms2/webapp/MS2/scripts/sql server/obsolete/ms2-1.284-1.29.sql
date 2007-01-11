-- Refactor indexes and fix foreign keys on developer machines
DROP INDEX ms2.MS2PeptidesData.IX_MS2PeptidesData_Fraction
GO

ALTER TABLE ms2.MS2Peptidememberships DROP
	CONSTRAINT fk_MS2Peptidemembership_MS2PeptidesData,
	CONSTRAINT fk_ms2peptidemembership_ms2proteingroup
GO

ALTER TABLE ms2.Quantitation DROP
    CONSTRAINT FK_Quantitation_MS2PeptidesData
GO

ALTER TABLE ms2.MS2PeptidesData DROP
    CONSTRAINT PK_MS2PeptidesData,
    CONSTRAINT UQ_PeptidesData
GO

CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData(Fraction, Scan, Charge)
GO

ALTER TABLE ms2.MS2PeptidesData ADD
	CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId)
GO

ALTER TABLE ms2.MS2Peptidememberships ADD
	CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid),
	CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid)
GO

ALTER TABLE ms2.Quantitation ADD
	CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId)
GO
