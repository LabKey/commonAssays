/*
 * Copyright (c) 2011 LabKey Corporation
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

-- Add the Unknown type for a titration
ALTER TABLE luminex.DataRow ADD WellRole NVARCHAR(50)
GO

UPDATE luminex.DataRow SET WellRole = 'Standard' WHERE UPPER(Type) LIKE 'S%' OR UPPER(Type) LIKE 'ES%'
GO
UPDATE luminex.DataRow SET WellRole = 'Control' WHERE UPPER(Type) LIKE 'C%'
GO
UPDATE luminex.DataRow SET WellRole = 'Background' WHERE UPPER(Type) LIKE 'B%'
GO
UPDATE luminex.DataRow SET WellRole = 'Unknown' WHERE UPPER(Type) LIKE 'U%'
GO
