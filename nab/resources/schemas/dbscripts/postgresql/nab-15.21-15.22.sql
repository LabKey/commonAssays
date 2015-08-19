ALTER TABLE nab.welldata ALTER COLUMN SpecimenLsid DROP NOT NULL;
ALTER TABLE nab.welldata ALTER COLUMN RunDataId DROP NOT NULL;
ALTER TABLE nab.welldata ADD COLUMN PlateNumber INT;
ALTER TABLE nab.welldata ADD COLUMN PlateVirusName VARCHAR(100);

ALTER TABLE nab.dilutiondata ALTER COLUMN Dilution TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN PercentNeutralization TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN NeutralizationPlusMinus TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Min TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Max TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN Mean TYPE DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ALTER COLUMN StdDev TYPE DOUBLE PRECISION;

ALTER TABLE nab.dilutiondata ADD COLUMN WellgroupName VARCHAR(100);
ALTER TABLE nab.dilutiondata ADD COLUMN ReplicateName VARCHAR(100);
ALTER TABLE nab.dilutiondata ADD COLUMN RunDataId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN MinDilution DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ADD COLUMN MaxDilution DOUBLE PRECISION;
ALTER TABLE nab.dilutiondata ADD COLUMN PlateNumber INT;
ALTER TABLE nab.dilutiondata ADD COLUMN RunId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN ProtocolId INT;
ALTER TABLE nab.dilutiondata ADD COLUMN Container ENTITYID NOT NULL;

ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_experimentrun FOREIGN KEY (RunId)
  REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.dilutiondata ADD CONSTRAINT fk_dilutiondata_rundataid FOREIGN KEY (RunDataId)
  REFERENCES nab.nabspecimen (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

SELECT core.executeJavaUpgradeCode('upgradeDilutionAssayWithNewTables');