/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

/* flow-14.10-14.11.sql */

-- add original keyword id
ALTER TABLE flow.keyword
    ADD COLUMN OriginalKeywordId INT;

-- copy keywordid to originalkeywordid, then update keywordid to the preferred keywordattr.id
UPDATE flow.keyword SET OriginalKeywordId = KeywordId;
UPDATE flow.keyword SET KeywordId = (
    SELECT id FROM flow.keywordattr WHERE flow.keywordattr.rowid = keywordid
);

ALTER TABLE flow.keyword
    ALTER COLUMN OriginalKeywordId SET NOT NULL;

ALTER TABLE flow.keyword
    ADD CONSTRAINT FK_Keyword_OriginalKeywordId FOREIGN KEY (OriginalKeywordId) REFERENCES flow.keywordattr (rowid);


-- add original statistic id
ALTER TABLE flow.statistic
    ADD COLUMN OriginalStatisticId INT;

-- copy statisticid to originalstatisticid, then update statisticid to the preferred statisticattr.id
UPDATE flow.statistic SET OriginalStatisticId = StatisticId;
UPDATE flow.statistic SET StatisticId = (
    SELECT id FROM flow.statisticattr WHERE flow.statisticattr.rowid = statisticid
);

ALTER TABLE flow.statistic
    ALTER COLUMN OriginalStatisticId SET NOT NULL;

ALTER TABLE flow.statistic
    ADD CONSTRAINT FK_Statistic_OriginalStatisticId FOREIGN KEY (OriginalStatisticId) REFERENCES flow.statisticattr (rowid);


-- add original graph id
ALTER TABLE flow.graph
    ADD COLUMN OriginalGraphId INT;

-- copy graphid to originalgraphid, then update graphid to the preferred graphattr.id
UPDATE flow.graph SET OriginalGraphId = GraphId;
UPDATE flow.graph SET GraphId = (
    SELECT id FROM flow.graphattr WHERE flow.graphattr.rowid = graphid
);

ALTER TABLE flow.graph
    ALTER COLUMN OriginalGraphId SET NOT NULL;

ALTER TABLE flow.graph
    ADD CONSTRAINT FK_Statistic_OriginalGraphId FOREIGN KEY (OriginalGraphId) REFERENCES flow.graphattr (rowid);