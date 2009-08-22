/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
ALTER TABLE ms2.MS2ProteinProphetFiles DROP CONSTRAINT UQ_MS2ProteinProphetFiles;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN Run INT NOT NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD COLUMN Container EntityId NOT NULL;
ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT FK_MS2ProteinProphetFiles_MS2Runs FOREIGN KEY (Run) REFERENCES ms2.MS2Runs(Run);
ALTER TABLE ms2.MS2ProteinProphetFiles ADD CONSTRAINT UQ_MS2ProteinProphetFiles UNIQUE (Run);
