
CREATE TABLE nab.dilutiondata
(
  RowId SERIAL NOT NULL,
  Dilution REAL,
  DilutionOrder INT,
  PercentNeutralization REAL,
  NeutralizationPlusMinus REAL,
  Min REAL,
  Max REAL,
  Mean REAL,
  StdDev REAL,

  CONSTRAINT pk_dilutiondata PRIMARY KEY (RowId)

);

CREATE TABLE nab.welldata
(
  RowId SERIAL NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid lsidtype NOT NULL,
  RunDataId INT NOT NULL,
  DilutionDataId INT,
  ProtocolId INT,
  "Row" INT,
  "Column" INT,
  Value REAL,
  ControlWellgroup VARCHAR(100),
  VirusWellgroup VARCHAR(100),
  SpecimenWellgroup VARCHAR(100),
  ReplicateWellgroup VARCHAR(100),
  ReplicateNumber INT,
  Container ENTITYID NOT NULL,

  CONSTRAINT pk_welldata PRIMARY KEY (RowId),
  CONSTRAINT fk_welldata_experimentrun FOREIGN KEY (RunId)
    REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_specimenlsid FOREIGN KEY (SpecimenLsid)
    REFERENCES exp.material (Lsid)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_rundataid FOREIGN KEY (RunDataId)
    REFERENCES nab.nabspecimen (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_welldata_dilutiondataid FOREIGN KEY (DilutionDataId)
    REFERENCES nab.dilutiondata (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION

);

CREATE INDEX idx_welldata_runid ON nab.welldata(RunId);
