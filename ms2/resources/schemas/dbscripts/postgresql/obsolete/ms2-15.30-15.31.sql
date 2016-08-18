/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
ALTER TABLE ms2.Runs ADD COLUMN MascotFile VARCHAR(300) NULL;
ALTER TABLE ms2.Runs ADD COLUMN DistillerRawFile VARCHAR(500) NULL;


ALTER TABLE ms2.PeptidesData ADD COLUMN QueryNumber int NULL;
ALTER TABLE ms2.PeptidesData ADD COLUMN HitRank int NOT NULL DEFAULT 1;
ALTER TABLE ms2.PeptidesData ADD COLUMN Decoy boolean NOT NULL DEFAULT FALSE;

SELECT core.fn_dropifexists('PeptidesData','ms2', 'INDEX','UQ_MS2PeptidesData_FractionScanCharge');
CREATE UNIQUE INDEX UQ_MS2PeptidesData_FractionScanCharge ON ms2.PeptidesData(Fraction, Scan, EndScan, Charge, HitRank, Decoy);