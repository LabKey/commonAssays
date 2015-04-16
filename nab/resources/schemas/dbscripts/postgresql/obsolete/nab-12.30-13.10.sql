/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

/* nab-12.30-12.31.sql */

CREATE SCHEMA nab;

CREATE TABLE nab.cutoffvalue
(
    rowid SERIAL NOT NULL,
    nabspecimenid INT NOT NULL,
    cutoff REAL,
    point REAL,
    pointoorindicator VARCHAR(20),
    
    ic_poly REAL,
    ic_polyoorindicator VARCHAR(20),
    ic_4pl REAL,
    ic_4ploorindicator VARCHAR(20),
    ic_5pl REAL,
    ic_5ploorindicator VARCHAR(20),

    CONSTRAINT pk_nab_cutoffvalue PRIMARY KEY (rowid)
);

CREATE TABLE nab.nabspecimen
(
    rowid SERIAL NOT NULL,
    dataid INT,
    runid INT NOT NULL,
    specimenlsid lsidtype NOT NULL,
    FitError REAL,
    WellgroupName VARCHAR(100),
    
    auc_poly REAL,
    positiveauc_poly REAL,
    auc_4pl REAL,
    positiveauc_4pl REAL,
    auc_5pl REAL,
    positiveauc_5pl REAL,

    -- For legacy migration purposes
    objecturi VARCHAR(300),
    objectid INT NOT NULL,
    protocolid INT,

    CONSTRAINT pk_nab_specimen PRIMARY KEY (rowid),
    CONSTRAINT fk_nabspecimen_experimentrun FOREIGN KEY (runid)
      REFERENCES exp.experimentrun (rowid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_nabspecimen_specimenlsid FOREIGN KEY (specimenlsid)
      REFERENCES exp.material (lsid)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX idx_nabspecimen_runid ON nab.nabspecimen(runid);
CREATE INDEX idx_nabspecimen_objectid ON nab.nabspecimen(objectid);
CREATE INDEX idx_nabspecimen_dataid ON nab.nabspecimen(dataid);

/* nab-12.31-12.32.sql */

ALTER TABLE nab.cutoffvalue ADD CONSTRAINT fk_cutoffvalue_nabspecimen FOREIGN KEY (nabspecimenid)
        REFERENCES nab.nabspecimen (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.nabspecimen ADD CONSTRAINT fk_nabspecimen_protocolid FOREIGN KEY (protocolid)
        REFERENCES exp.protocol (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;

/* nab-12.32-12.33.sql */

/* Script to migrate existing Nab assay data from Object Properities to NabSpecimen and CutoffValue tables */

delete from nab.cutoffvalue;
delete from nab.nabspecimen;

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
	SELECT s.RowId, CAST (substr(pd.PropertyURI, POSITION(':Point+IC' IN pd.PropertyURI) + 9, 2) AS INT), op.FloatValue
	FROM nab.NAbSpecimen s, exp.PropertyDescriptor pd, exp.ObjectProperty op, exp.Object o
	WHERE pd.PropertyId = op.PropertyId AND op.ObjectId = o.ObjectId AND o.ObjectURI = s.ObjectURI AND pd.PropertyURI LIKE '%:NabProperty.%:Point+IC%' AND pd.PropertyURI NOT LIKE '%OORIndicator';

UPDATE nab.CutoffValue SET
	PointOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Point+IC' || CAST(Cutoff AS INT) || 'OORIndicator'),
	IC_4PL = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_4pl'),
	IC_4PLOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_4plOORIndicator'),
	IC_5PL = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_5pl'),
	IC_5PLOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_5plOORIndicator'),
	IC_Poly = (SELECT op.FloatValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_poly'),
	IC_PolyOORIndicator = (SELECT op.StringValue FROM
		exp.ObjectProperty op,
		exp.PropertyDescriptor pd,
		nab.NAbSpecimen ns
		WHERE op.PropertyId = pd.PropertyId AND ns.ObjectId = op.ObjectId AND ns.RowId = NAbSpecimenID AND pd.PropertyURI LIKE '%:Curve+IC' || CAST(Cutoff AS INT) || '_polyOORIndicator');

SELECT core.executeJavaUpgradeCode('migrateToNabSpecimen');

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

/* nab-12.34-12.35.sql */

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