/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

/* microarray-13.30-13.31.sql */

CREATE TABLE microarray.FeatureAnnotationSet (
  RowId INT IDENTITY (1,1) NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID NOT NULL,
  Created DATETIME NOT NULL,
  ModifiedBy USERID NOT NULL,
  Modified DATETIME NOT NULL,

  "Name" NVARCHAR(255) NOT NULL,
  Vendor NVARCHAR(50),
  Description NVARCHAR(2000),

  CONSTRAINT PK_FeatureAnnotationSet PRIMARY KEY (RowId),
  CONSTRAINT FK_FeatureAnnotationSet_Container FOREIGN KEY (Container) REFERENCES core.Containers(EntityId)
);

CREATE TABLE microarray.FeatureAnnotation (
  RowId INT IDENTITY (1,1) NOT NULL,
  Container ENTITYID NOT NULL,
  CreatedBy USERID NOT NULL,
  Created DATETIME NOT NULL,
  ModifiedBy USERID NOT NULL,
  Modified DATETIME NOT NULL,

  FeatureAnnotationSetId INT NOT NULL,
  ProbeId NVARCHAR(128) NOT NULL,
  GeneSymbol NVARCHAR(2000),

  CONSTRAINT PK_FeatureAnnotation PRIMARY KEY (RowId),
  CONSTRAINT UQ_FeatureAnnotation_ProbeId_FeatureAnnotationSetId UNIQUE (ProbeId, FeatureAnnotationSetId),
  CONSTRAINT FK_FeatureAnnotation_FeatureAnnotationSetId FOREIGN KEY (FeatureAnnotationSetId) REFERENCES microarray.FeatureAnnotationSet(RowId)
);

CREATE TABLE microarray.FeatureData (
  RowId INT IDENTITY (1,1) NOT NULL,
  "Value" REAL,
  FeatureId INT NOT NULL,
  SampleId INT NOT NULL,
  DataId INT NOT NULL,

  CONSTRAINT PK_FeatureData Primary Key (RowId),
  CONSTRAINT FK_FeatureData_FeatureId FOREIGN KEY (FeatureId) REFERENCES microarray.FeatureAnnotation (RowId),
  CONSTRAINT FK_FeatureData_SampleId FOREIGN KEY (SampleId) REFERENCES exp.material (RowId),
  CONSTRAINT FK_FeatureData_DataId FOREIGN KEY (DataId) REFERENCES exp.data (RowId)
);

/* microarray-13.31-13.32.sql */

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

/* microarray-13.32-13.33.sql */

-- Issue 19548: Feature annotations in expression matrix are missing columns from GEOMicroarray
-- Fix typo: rename 'Req' -> 'Ref'
EXEC sp_rename 'microarray.FeatureAnnotation.ReqSeqProteinId', 'RefSeqProteinId', 'COLUMN';
EXEC sp_rename 'microarray.FeatureAnnotation.ReqSeqTranscriptId', 'RefSeqTranscriptId', 'COLUMN';