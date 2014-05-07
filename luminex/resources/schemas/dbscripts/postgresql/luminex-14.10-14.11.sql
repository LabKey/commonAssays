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

ALTER TABLE luminex.GuideSet ADD COLUMN ValueBased BOOLEAN;
UPDATE luminex.GuideSet SET ValueBased = FALSE;
ALTER TABLE luminex.GuideSet ALTER COLUMN ValueBased SET NOT NULL;

ALTER TABLE luminex.GuideSet ADD COLUMN EC504PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN EC504PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN EC505PLAverage REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN EC505PLStdDev REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN AUCAverage REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN AUCStdDev REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN MaxFIAverage REAL;
ALTER TABLE luminex.GuideSet ADD COLUMN MaxFIStdDev REAL;