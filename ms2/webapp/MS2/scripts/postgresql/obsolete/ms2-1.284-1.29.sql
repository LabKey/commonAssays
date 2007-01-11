-- Refactor indexes and fix foreign keys on developer machines
DROP INDEX ms2.IX_MS2PeptidesData_Fraction;

ALTER TABLE ms2.MS2Peptidememberships
	DROP CONSTRAINT fk_MS2Peptidemembership_MS2PeptidesData,
	DROP CONSTRAINT fk_ms2peptidemembership_ms2proteingroup;

ALTER TABLE ms2.Quantitation
    DROP CONSTRAINT FK_Quantitation_MS2PeptidesData;

ALTER TABLE ms2.MS2PeptidesData
    DROP CONSTRAINT PK_MS2PeptidesData,
    DROP CONSTRAINT UQ_PeptidesData;

CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.MS2PeptidesData(Fraction, Scan, Charge);

ALTER TABLE ms2.MS2PeptidesData
	ADD CONSTRAINT PK_MS2PeptidesData PRIMARY KEY (RowId);

ALTER TABLE ms2.MS2Peptidememberships
	ADD CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid),
	ADD CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid);

ALTER TABLE ms2.Quantitation
	ADD CONSTRAINT FK_Quantitation_MS2PeptidesData FOREIGN KEY (PeptideId) REFERENCES ms2.MS2PeptidesData(RowId);
