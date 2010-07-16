/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
/* luminex-2.30-2.31.sql */

ALTER TABLE luminex.DataRow DROP CONSTRAINT FK_LuminexDataRow_AnalyteId
GO

ALTER TABLE luminex.DataRow ADD CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (AnalyteId) REFERENCES luminex.analyte(RowId)
GO

/* luminex-2.31-2.32.sql */

ALTER TABLE luminex.DataRow ADD ExtraSpecimenInfo NVARCHAR(50)
GO