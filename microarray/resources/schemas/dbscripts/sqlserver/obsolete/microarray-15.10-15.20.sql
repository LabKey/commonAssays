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

/* microarray-15.10-15.11.sql */

ALTER TABLE microarray.FeatureData ADD CONSTRAINT UQ_FeatureData_DataId_FeatureId_SampleId UNIQUE (DataId, FeatureId, SampleId);

/* microarray-15.11-15.12.sql */

DROP INDEX IX_FeatureData_DataId ON microarray.FeatureData;