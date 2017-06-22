/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

-- note : nab-16.20-nab-16.21 was backported to release 16.2 to fix issue : 27040
-- when scripts are consolidated for 16.3 we will need to account for both 16.20-16.30 and 16.21-16.30 upgrade paths
CREATE INDEX IDX_WellData_DilutionDataId ON nab.wellData(dilutionDataId);
SELECT core.executeJavaUpgradeCode('repairCrossPlateDilutionData');
