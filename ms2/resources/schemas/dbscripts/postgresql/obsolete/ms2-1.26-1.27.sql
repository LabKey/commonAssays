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
ALTER TABLE ms2.ms2proteingroups ADD COLUMN proteinprobability REAL NOT NULL DEFAULT 0;

DROP TABLE ms2.ms2peptidememberships;

CREATE TABLE ms2.ms2peptidememberships
(
  peptideid int8 NOT NULL,
  proteingroupid int4 NOT NULL,
  nspadjustedprobability float4 NOT NULL,
  weight float4 NOT NULL,
  nondegenerateevidence bool NOT NULL,
  enzymatictermini int4 NOT NULL,
  siblingpeptides float4 NOT NULL,
  siblingpeptidesbin int4 NOT NULL,
  instances int4 NOT NULL,
  contributingevidence bool NOT NULL,
  calcneutralpepmass float4 NOT NULL,
  CONSTRAINT pk_ms2peptidememberships PRIMARY KEY (proteingroupid, peptideid),
  CONSTRAINT fk_ms2peptidemembership_ms2peptidesdata FOREIGN KEY (peptideid) REFERENCES ms2.ms2peptidesdata (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_ms2peptidemembership_ms2proteingroup FOREIGN KEY (proteingroupid) REFERENCES ms2.ms2proteingroups (rowid) ON UPDATE NO ACTION ON DELETE NO ACTION
); 