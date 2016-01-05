-- This index should replace the one created in ms2-15.30-15.31.sql.  There is no need to create the older one first.
EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX', 'UQ_MS2PeptidesData_FractionScanCharge';
CREATE UNIQUE CLUSTERED INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy, QueryNumber);