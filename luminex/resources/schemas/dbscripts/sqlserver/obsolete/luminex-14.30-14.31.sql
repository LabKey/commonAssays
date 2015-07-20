/*
 * Copyright (c) 2014-2015 LabKey Corporation
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

ALTER TABLE luminex.GuideSet ADD EC504PLEnabled BIT;
ALTER TABLE luminex.GuideSet ADD EC505PLEnabled BIT;
ALTER TABLE luminex.GuideSet ADD AUCEnabled BIT;
ALTER TABLE luminex.GuideSet ADD MaxFIEnabled BIT;
GO
UPDATE luminex.GuideSet SET EC504PLEnabled=1, EC505PLEnabled=1, AUCEnabled=1, MaxFIEnabled=1;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC504PLEnabled BIT NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN EC505PLEnabled BIT NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN AUCEnabled BIT NOT NULL;
ALTER TABLE luminex.GuideSet ALTER COLUMN MaxFIEnabled BIT NOT NULL;
