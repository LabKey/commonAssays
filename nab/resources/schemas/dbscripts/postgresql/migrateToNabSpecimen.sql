/*
 * Copyright (c) 2013 LabKey Corporation
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


