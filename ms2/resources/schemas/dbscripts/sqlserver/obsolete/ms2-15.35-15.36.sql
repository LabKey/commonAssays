/*
 * Copyright (c) 2016 LabKey Corporation
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
-- Create a new set of properties for Mascot settings
INSERT INTO prop.propertysets (category, objectid, userid) SELECT 'MascotConfig' AS Category, EntityId, -1 AS UserId FROM core.containers WHERE parent IS NULL;

-- Migrate existing Mascot settings
UPDATE prop.properties SET "set" = (SELECT MAX("set") FROM prop.propertysets)
  WHERE name LIKE 'Mascot%' AND "set" = (SELECT "set" FROM prop.propertysets WHERE category = 'SiteConfig' AND userid = -1 AND objectid = (SELECT entityid FROM core.containers WHERE parent IS NULL));
