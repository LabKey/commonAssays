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


