/*
 * Copyright (c) 2009 LabKey Corporation
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

ALTER TABLE luminex.DataRow ADD SpecimenID NVARCHAR(50)
GO

-- Copy anything in the Description column that might be a specimen id to the SpecimenID column
UPDATE luminex.DataRow SET SpecimenID = Description WHERE patindex('% %', Description) = 0 AND patindex('%,%', Description) = 0
GO