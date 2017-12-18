/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

/* nab-0.00-15.20.sql */

/* nab-11.10-11.20.sql */

-- Set all of the NAb run properties that control the calculations to be not shown in update views
UPDATE exp.propertydescriptor SET showninupdateview = 0 WHERE propertyid IN
(
	SELECT pd.propertyid
	FROM exp.propertydescriptor pd, exp.propertydomain propdomain, exp.domaindescriptor dd
	WHERE
		(LOWER(pd.name) LIKE 'cutoff%' OR lower(pd.name) LIKE 'curvefitmethod' ) AND
		pd.propertyid = propdomain.propertyid AND
		dd.domainid = propdomain.domainid AND
		domainuri IN
		(
			-- Find all the NAb run domain URIs
			SELECT dd.domainuri
			FROM exp.object o, exp.objectproperty op, exp.protocol p, exp.domaindescriptor dd
			WHERE o.objecturi = p.lsid AND op.objectid = o.objectid AND op.stringvalue = dd.domainuri AND p.lsid LIKE '%:NabAssayProtocol.%' AND dd.domainuri LIKE '%:AssayDomain-Run.%'
		)
);

GO

/* nab-12.30-13.10.sql */

CREATE SCHEMA NAb;
GO

CREATE TABLE NAb.CutoffValue
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    NAbSpecimenId INT NOT NULL,
    Cutoff REAL,
    Point REAL,
    PointOORIndicator NVARCHAR(20),

    IC_Poly REAL,
    IC_PolyOORIndicator NVARCHAR(20),
    IC_4pl REAL,
    IC_4plOORIndicator NVARCHAR(20),
    IC_5pl REAL,
    IC_5plOORIndicator NVARCHAR(20),

    CONSTRAINT PK_NAb_CutoffValue PRIMARY KEY (RowId)
);

CREATE TABLE NAb.NAbSpecimen
(
    RowId INT IDENTITY (1, 1) NOT NULL,
    DataId INT,
    RunId INT NOT NULL,
    SpecimenLSID LSIDtype NOT NULL,
    FitError REAL,
    WellgroupName NVARCHAR(100),

    AUC_poly REAL,
    PositiveAUC_Poly REAL,
    AUC_4pl REAL,
    PositiveAUC_4pl REAL,
    AUC_5pl REAL,
    PositiveAUC_5pl REAL,

    -- For legacy migration purposes
    ObjectUri NVARCHAR(300),
    ObjectId INT NOT NULL,
    ProtocolId INT,

    CONSTRAINT PK_NAb_Specimen PRIMARY KEY (RowId),
    CONSTRAINT FK_NAbSpecimen_ExperimentRun FOREIGN KEY (RunId)
      REFERENCES Exp.ExperimentRun (RowId)
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT FK_NAbSpecimen_SpecimenLSID FOREIGN KEY (SpecimenLSID)
      REFERENCES Exp.Material (LSID)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IDX_NAbSpecimen_RunId ON NAb.NAbSpecimen(RunId);
CREATE INDEX IDX_NAbSpecimen_ObjectId ON NAb.NAbSpecimen(ObjectId);
CREATE INDEX IDX_NAbSpecimen_DataId ON NAb.NAbSpecimen(DataId);

ALTER TABLE NAb.CutoffValue ADD CONSTRAINT FK_CutoffValue_NAbSpecimen FOREIGN KEY (NAbSpecimenId)
        REFERENCES NAb.NAbSpecimen (rowid);
ALTER TABLE NAb.NAbSpecimen ADD CONSTRAINT FK_NAbSpecimen_ProtocolId FOREIGN KEY (ProtocolId)
        REFERENCES Exp.Protocol (rowid);

/* Script to migrate existing Nab assay data from Object Properities to NabSpecimen and CutoffValue tables */

delete from nab.CutoffValue;
delete from nab.NAbSpecimen;

INSERT INTO nab.NAbSpecimen (DataId, RunID, ProtocolID, SpecimenLSID, FitError, WellGroupName, AUC_Poly, AUC_5PL, AUC_4PL, PositiveAUC_Poly, PositiveAUC_5PL, PositiveAUC_4PL, ObjectURI, ObjectId)
SELECT * FROM (
	SELECT
		(SELECT RowId FROM exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI and parent.ObjectId = o.OwnerObjectId) AS DataId,
		(SELECT RunId FROM exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI and parent.ObjectId = o.OwnerObjectId) AS RunId,
		(SELECT p.RowId FROM exp.ExperimentRun er, exp.Protocol p, exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI and parent.ObjectId = o.OwnerObjectId AND er.RowId = d.RunId AND p.LSID = er.ProtocolLSID) AS ProtocolId,

		(SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:SpecimenLsid' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS SpecimenLSID,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:Fit+Error' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS FitError,
		(SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:WellgroupName' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS WellGroupName,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:AUC_poly' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS AUC_Poly,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:AUC_5pl' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS AUC_5PL,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:AUC_4pl' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS AUC_4PL,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:PositiveAUC_poly' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS PositiveAUC_Poly,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:PositiveAUC_5pl' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS PositiveAUC_5PL,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:PositiveAUC_4pl' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS PositiveAUC_4PL,
		ObjectURI,
		ObjectId
	FROM exp.Object o WHERE ObjectURI LIKE '%AssayRunNabDataRow%') x
	WHERE specimenlsid IS NOT NULL AND DataId IS NOT NULL AND RunID IS NOT NULL AND ProtocolID IS NOT NULL;

INSERT INTO nab.CutoffValue (NAbSpecimenId, Cutoff, Point)
	SELECT s.RowId, CAST (SUBSTRING(pd.PropertyURI, CHARINDEX(':Point+IC', pd.PropertyURI) + 9, 2) AS INT), op.FloatValue
	FROM nab.NAbSpecimen s, exp.PropertyDescriptor pd, exp.ObjectProperty op, exp.Object o
	WHERE pd.PropertyId = op.PropertyId AND op.ObjectId = o.ObjectId AND o.ObjectURI = s.ObjectURI AND pd.PropertyURI LIKE '%:NabProperty.%:Point+IC%' AND pd.PropertyURI NOT LIKE '%OORIndicator';

UPDATE nab.CutoffValue SET
	PointOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Point+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + 'OORIndicator'),
	IC_4PL = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE ('%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_4pl')),
	IC_4PLOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_4plOORIndicator'),
	IC_5PL = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_5pl'),
	IC_5PLOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_5plOORIndicator'),
	IC_Poly = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_poly'),
	IC_PolyOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' + CAST(CAST(Cutoff AS INT) AS NVARCHAR) + '_polyOORIndicator');

GO

-- Change keyPropertyName in study.dataset
UPDATE study.DataSet SET KeyPropertyName = 'RowId' WHERE ProtocolId IN (SELECT ProtocolId FROM nab.NAbSpecimen);

-- Remove stuff that we moved from properties
DELETE FROM exp.ObjectProperty
    WHERE ObjectId IN (SELECT ObjectID FROM nab.NabSpecimen) AND
      (PropertyId IN
        (SELECT PropertyId FROM exp.PropertyDescriptor pd
			WHERE
            (pd.PropertyURI LIKE '%:SpecimenLsid' OR
             pd.PropertyURI LIKE '%:Fit+Error' OR
             pd.PropertyURI LIKE '%:WellgroupName' OR
             pd.PropertyURI LIKE '%:AUC_poly' OR
             pd.PropertyURI LIKE '%:AUC_5pl' OR
             pd.PropertyURI LIKE '%:AUC_4pl' OR
             pd.PropertyURI LIKE '%:PositiveAUC_poly' OR
             pd.PropertyURI LIKE '%:PositiveAUC_5pl' OR
             pd.PropertyURI LIKE '%:PositiveAUC_4pl' OR
             pd.PropertyURI LIKE '%:Point+IC' OR
             pd.PropertyURI LIKE '%:Point+ICOORIndicator' OR
             pd.PropertyURI LIKE '%:Curve+IC%_4pl%' OR
             pd.PropertyURI LIKE '%:Curve+IC%_4plOORIndicator'OR
             pd.PropertyURI LIKE '%:Curve+IC%_5pl%'OR
             pd.PropertyURI LIKE '%:Curve+IC%_5plOORIndicator'OR
             pd.PropertyURI LIKE '%:Curve+IC%_poly' OR
             pd.PropertyURI LIKE '%:Curve+IC%_polyOORIndicator')));

-- remove leftover object properties
DELETE FROM exp.ObjectProperty
    WHERE ObjectId IN (SELECT ObjectId FROM exp.Object WHERE ObjectURI LIKE 'urn:lsid:%AssayRunNabDataRow.%') AND
      (PropertyId IN (SELECT PropertyId FROM exp.PropertyDescriptor WHERE
        (PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
         PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
         PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
         PropertyURI LIKE '%:NabProperty%:AUC%' OR
         PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
         PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
         PropertyURI LIKE '%:NabProperty%:Curve+IC%')));

-- remove property descriptors we don't use anymore
DELETE FROM exp.PropertyDescriptor
    WHERE Container IN (SELECT Container FROM exp.ExperimentRun er, nab.NabSpecimen ns WHERE ns.RunId = er.RowId) AND
	  (PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
	   PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
	   PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
	   PropertyURI LIKE '%:NabProperty%:AUC%' OR
	   PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
	   PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
	   PropertyURI LIKE '%:NabProperty%:Curve+IC%');

/* nab-13.10-13.20.sql */

EXEC core.fn_dropifexists 'NAbSpecimen', 'nab', 'INDEX', 'IDX_NAbSpecimen_ProtocolId';
CREATE INDEX IDX_NAbSpecimen_ProtocolId ON nab.NAbSpecimen(ProtocolId);

EXEC core.fn_dropifexists 'CutoffValue', 'nab', 'INDEX', 'IDX_CutoffValue_NabSpecimenId';
CREATE INDEX IDX_CutoffValue_NabSpecimenId ON nab.cutoffvalue(NabSpecimenId);

/* nab-14.20-14.30.sql */

ALTER TABLE NAb.NAbSpecimen ADD VirusLsid LSIDtype;

GO

CREATE SCHEMA nabvirus;

GO

/* nab-14.30-14.31.sql */

-- Rename NAb schema to nab. See #21853.
CREATE SCHEMA nab_xxx;
GO
ALTER SCHEMA nab_xxx TRANSFER NAb.CutoffValue;
ALTER SCHEMA nab_xxx TRANSFER NAb.NAbSpecimen;
DROP SCHEMA NAb;
GO

CREATE SCHEMA nab;
GO
ALTER SCHEMA nab TRANSFER nab_xxx.CutoffValue;
ALTER SCHEMA nab TRANSFER nab_xxx.NAbSpecimen;
DROP SCHEMA nab_xxx;
GO

/* nab-15.10-15.11.sql */

EXEC core.fn_dropifexists 'NAbSpecimen', 'nab', 'INDEX', 'idx_nabspecimen_specimenlsid';

CREATE INDEX IDX_NAbSpecimen_SpecimenLSID ON nab.NAbSpecimen(SpecimenLSID);

/* nab-15.20-15.30.sql */

/* nab-15.20-15.21.sql */

CREATE TABLE NAb.DilutionData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  Dilution REAL,
  DilutionOrder INT,
  PercentNeutralization REAL,
  NeutralizationPlusMinus REAL,
  Min REAL,
  Max REAL,
  Mean REAL,
  StdDev REAL,

  CONSTRAINT PK_NAb_DilutionData PRIMARY KEY (RowId)

);

CREATE TABLE NAb.WellData
(
  RowId INT IDENTITY (1, 1) NOT NULL,
  RunId INT NOT NULL,
  SpecimenLsid LSIDtype NOT NULL,
  RunDataId INT NOT NULL,
  DilutionDataId INT,
  ProtocolId INT,
  "Row" INT,
  "Column" INT,
  Value REAL,
  ControlWellgroup NVARCHAR(100),
  VirusWellgroup NVARCHAR(100),
  SpecimenWellgroup NVARCHAR(100),
  ReplicateWellgroup NVARCHAR(100),
  ReplicateNumber INT,
  Container EntityId NOT NULL,

  CONSTRAINT PK_NAb_WellData PRIMARY KEY (RowId),
  CONSTRAINT FK_WellData_ExperimentRun FOREIGN KEY (RunId)
    REFERENCES Exp.ExperimentRun (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_SpecimenLSID FOREIGN KEY (SpecimenLSID)
    REFERENCES Exp.Material (LSID)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_RunDataId FOREIGN KEY (RunDataId)
    REFERENCES NAb.NAbSpecimen (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT FK_WellData_DilutionDataId FOREIGN KEY (DilutionDataId)
    REFERENCES NAb.DilutionData (RowId)
    ON UPDATE NO ACTION ON DELETE NO ACTION

);

CREATE INDEX IDX_WellData_RunId ON NAb.WellData(RunId);

/* nab-15.21-15.22.sql */

ALTER TABLE nab.WellData ALTER COLUMN SpecimenLsid LSIDType NULL;
ALTER TABLE nab.WellData ALTER COLUMN RunDataId INTEGER NULL;
ALTER TABLE nab.WellData ADD PlateNumber INT;
ALTER TABLE nab.WellData ADD PlateVirusName NVARCHAR(100);

ALTER TABLE nab.DilutionData ALTER COLUMN Dilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN PercentNeutralization DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN NeutralizationPlusMinus DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Min DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Max DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN Mean DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ALTER COLUMN StdDev DOUBLE PRECISION;

ALTER TABLE nab.DilutionData ADD WellgroupName NVARCHAR(100);
ALTER TABLE nab.DilutionData ADD ReplicateName NVARCHAR(100);
ALTER TABLE nab.DilutionData ADD RunDataId INT;
ALTER TABLE nab.DilutionData ADD MaxDilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ADD MinDilution DOUBLE PRECISION;
ALTER TABLE nab.DilutionData ADD PlateNumber INT;
ALTER TABLE nab.DilutionData ADD RunId INT;
ALTER TABLE nab.DilutionData ADD ProtocolId INT;
ALTER TABLE nab.DilutionData ADD Container ENTITYID NOT NULL;

ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_ExperimentRun FOREIGN KEY (RunId)
  REFERENCES Exp.ExperimentRun (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.DilutionData ADD CONSTRAINT FK_DilutionData_RunDataId FOREIGN KEY (RunDataId)
  REFERENCES nab.NAbSpecimen (RowId)
  ON UPDATE NO ACTION ON DELETE NO ACTION;

CREATE INDEX IDX_DilutionData_RunId ON nab.DilutionData(RunId);

EXEC core.executeJavaUpgradeCode 'upgradeDilutionAssayWithNewTables';

/* nab-15.22-15.23.sql */

ALTER TABLE nab.NabSpecimen ALTER COLUMN FitError DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_poly DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_Poly DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_4pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_4pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_5pl DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_5pl DOUBLE PRECISION;

ALTER TABLE nab.CutoffValue ALTER COLUMN Cutoff DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN Point DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_Poly DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_4pl DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_5pl DOUBLE PRECISION;