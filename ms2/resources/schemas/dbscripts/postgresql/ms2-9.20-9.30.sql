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

/* ms2-9.20-9.21.sql */

UPDATE prot.InfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp.pl?gene={}'
    WHERE Name = 'GeneCards';

/* ms2-9.21-9.22.sql */

ALTER TABLE ms2.peptidesdata ALTER score1 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score1 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score2 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score2 DROP DEFAULT;

ALTER TABLE ms2.peptidesdata ALTER score3 DROP NOT NULL;
ALTER TABLE ms2.peptidesdata ALTER score3 DROP DEFAULT;