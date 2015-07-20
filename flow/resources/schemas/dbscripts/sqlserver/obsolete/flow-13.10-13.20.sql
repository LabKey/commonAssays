/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

-- Drop the NOT NULL constraint on the keyword value column
ALTER TABLE flow.keyword ALTER COLUMN value NTEXT NULL;
GO

-- Keyword values are trimmed to null on import as of r27096.
UPDATE flow.keyword
SET value = NULL
WHERE 0 = LEN(LTRIM(RTRIM(CAST(value AS NVARCHAR(MAX)))));

-- Issue 17371: flow: population names may contain funny dash characters from mac workspaces
EXEC core.executeJavaUpgradeCode 'removeMacRomanDashes';

