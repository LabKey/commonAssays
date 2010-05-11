/*
 * Copyright (c) 2008 LabKey Corporation
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

/* ms1-2.30-2.31.sql */

EXEC core.fn_dropifexists 'PeaksToFamilies', 'ms1', 'INDEX','IDX_PeaksToFamilies_PeakId'
GO

CREATE INDEX IDX_PeaksToFamilies_PeakId ON ms1.PeaksToFamilies(PeakId)
GO

/* ms1-2.31-2.32.sql */

IF NOT EXISTS (SELECT * FROM information_schema.columns
WHERE column_name ='MS2Charge'
AND table_name = 'Features'
AND table_schema = 'ms1')
BEGIN
	ALTER TABLE ms1.Features
	ADD MS2Charge TINYINT NULL
END 
GO

/* Back-fill the MS2Charge column for all existing features */
UPDATE ms1.Features
SET MS2Charge=
(SELECT TOP 1 pd1.Charge
FROM ms2.PeptidesData AS pd1
INNER JOIN ms2.Fractions AS fr1 ON (pd1.Fraction=fr1.Fraction)
INNER JOIN ms2.Runs AS r1 ON (fr1.Run=r1.Run)
INNER JOIN ms1.Files AS fi1 ON (fi1.MzXmlUrl=fr1.MzXmlUrl)
INNER JOIN ms1.Features AS fe1 ON (fe1.FileId=fi1.FileId AND fe1.MS2Scan=pd1.Scan)
WHERE r1.Deleted=0 AND ms1.Features.FeatureId=fe1.FeatureId
AND r1.Container=
(SELECT d2.Container FROM exp.Data AS d2 INNER JOIN ms1.Files AS fi2 ON (fi2.ExpDataFileId=d2.RowId)
INNER JOIN ms1.Features AS fe2 ON (fi2.FileId=fe2.FileId) WHERE fe2.FeatureId=ms1.Features.FeatureId)
ORDER BY pd1.PeptideProphet DESC)
WHERE MS2Scan IS NOT NULL AND MS2Charge IS NULL
GO
