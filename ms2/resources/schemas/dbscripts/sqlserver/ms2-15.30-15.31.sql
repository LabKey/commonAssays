ALTER TABLE ms2.Runs ADD MascotFile NVARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD DistillerRawFile NVARCHAR(500) NULL;


ALTER TABLE ms2.PeptidesData ADD QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD Decoy bit NOT NULL DEFAULT 0;

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'UQ_MS2PeptidesData_FractionScanCharge';
CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy);