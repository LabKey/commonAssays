/*
 * Copyright (c) 2014 LabKey Corporation
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

-- Rename ProbeId to FeatureId
EXEC sp_rename 'microarray.FeatureAnnotation.ProbeId', 'FeatureId', 'COLUMN';

EXEC sp_rename 'microarray.featureannotation.UQ_FeatureAnnotation_ProbeId_FeatureAnnotationSetId', 'UQ_FeatureAnnotation_FeatureId_FeatureAnnotationSetId';


-- Restore original GEOMicroarray FeatureAnnotation columns
ALTER TABLE microarray.FeatureAnnotation ADD UniGeneId NVARCHAR(2000);
ALTER TABLE microarray.FeatureAnnotation ADD GeneId NVARCHAR(2000);
ALTER TABLE microarray.FeatureAnnotation ADD AccessionId NVARCHAR(128);
ALTER TABLE microarray.FeatureAnnotation ADD ReqSeqProteinId NVARCHAR(2000);
ALTER TABLE microarray.FeatureAnnotation ADD ReqSeqTranscriptId NVARCHAR(2000);

CREATE INDEX IX_FeatureAnnotation_FeatureAnnotationSetId
   ON microarray.FeatureAnnotation (FeatureAnnotationSetId);

