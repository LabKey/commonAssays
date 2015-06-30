/*
 * Copyright (c) 2015 LabKey Corporation
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

/* elispotlk-15.10-15.12.sql */

CREATE SCHEMA elispotlk;
CREATE SCHEMA elispotantigen;

CREATE TABLE elispotlk.rundata
(
    RowId SERIAL NOT NULL,
    RunId INT NOT NULL,
    SpecimenLsid lsidtype NOT NULL,
    AntigenLsid lsidtype,
    SpotCount REAL,
    WellgroupName VARCHAR(4000),
    WellgroupLocation VARCHAR(4000),
    NormalizedSpotCount REAL,
    AntigenWellgroupName VARCHAR(4000),
    Analyte VARCHAR(4000),
    Activity DOUBLE PRECISION,
    Intensity DOUBLE PRECISION,

    ObjectUri VARCHAR(300),
    ObjectId INT NOT NULL,

    CONSTRAINT pk_elispot_rundata PRIMARY KEY (RowId),
    CONSTRAINT fk_elispotrundata_experimentrun FOREIGN KEY (RunId)
      REFERENCES exp.experimentrun (RowId) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT fk_elispotrundata_specimenlsid FOREIGN KEY (Specimenlsid)
      REFERENCES exp.material (Lsid)
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX idx_elispotrundata_runid ON elispotlk.rundata(RunId);


INSERT INTO elispotlk.rundata (RunId, Specimenlsid, SpotCount, WellgroupName, WellgroupLocation, NormalizedSpotCount, AntigenWellgroupName, ObjectUri, ObjectId)
SELECT
    RunId,SpecimenLSID, SpotCount, WellGroupName, WellgroupLocation, NormalizedSpotCount,
    CASE WHEN AntigenWellgroupNameTemp IS NULL THEN AntigenName ELSE AntigenWellgroupNameTemp END AS AntigenWellgroupName,
    ObjectURI, ObjectId
FROM (
	SELECT
		(SELECT RunId FROM exp.Data d, exp.Object parent WHERE d.LSID = parent.ObjectURI and parent.ObjectId = o.OwnerObjectId) AS RunId,

		(SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:SpecimenLsid' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS SpecimenLSID,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:SpotCount' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS SpotCount,
		(SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:WellgroupName' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS WellGroupName,
		(SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:WellgroupLocation' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS WellgroupLocation,
		(SELECT FloatValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
			WHERE pd.PropertyURI LIKE '%:NormalizedSpotCount' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS NormalizedSpotCount,
        (SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
            WHERE pd.PropertyURI LIKE '%:AntigenWellgroupName' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS AntigenWellgroupNameTemp,
        (SELECT StringValue FROM exp.ObjectProperty op, exp.PropertyDescriptor pd
            WHERE pd.PropertyURI LIKE '%:AntigenName' AND op.PropertyId = pd.PropertyId AND op.ObjectId = o.ObjectId) AS AntigenName,
		ObjectURI,
		ObjectId
	FROM exp.Object o WHERE ObjectURI LIKE '%ElispotAssayDataRow%') x
	WHERE specimenlsid IS NOT NULL AND RunID IS NOT NULL;

SELECT core.executeJavaUpgradeCode('migrateToElispotTables');

/* elispotlk-15.12-15.13.sql */

ALTER TABLE elispotlk.rundata ADD COLUMN Cytokine VARCHAR(4000);

/* elispotlk-15.13-15.14.sql */

ALTER TABLE elispotlk.rundata ADD COLUMN SpotSize REAL;