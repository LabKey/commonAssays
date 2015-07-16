/*
 * Copyright (c) 2011-2015 LabKey Corporation
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
