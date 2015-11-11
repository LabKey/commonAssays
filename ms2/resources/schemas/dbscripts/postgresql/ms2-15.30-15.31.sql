ALTER TABLE ms2.Runs ADD COLUMN MascotFile VARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD COLUMN DistillerRawFile VARCHAR(500) NULL;


ALTER TABLE ms2.PeptidesData ADD COLUMN QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD COLUMN HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD COLUMN Decoy boolean NOT NULL DEFAULT FALSE;

SELECT core.fn_dropifexists('PeptidesData','ms2', 'INDEX','UQ_MS2PeptidesData_FractionScanCharge');
CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy);