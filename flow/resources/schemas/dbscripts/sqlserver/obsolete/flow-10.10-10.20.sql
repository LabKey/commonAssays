/*
 * Copyright (c) 2010-2011 LabKey Corporation
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


/* flow-10.10-10.11.sql */

/**
 * the query to find the in use statistic/graph/keyword ids is way to expensive
 * so keep track of the in us attributes per container/type
 *
 * there three tables are basically a materialized view over flow.Object and
 * the respective data table (statistic,keyword,graph) 
 */

CREATE TABLE flow.StatisticAttr
(
  container ENTITYID NOT NULL,
  id INT NOT NULL,
  CONSTRAINT "PK_StatistiAttr" UNIQUE (container, id)
)
go

INSERT INTO flow.StatisticAttr (container, id)
SELECT DISTINCT OBJ.container, PROP.statisticid AS id
FROM flow.object OBJ INNER JOIN flow.statistic PROP ON OBJ.rowid = PROP.objectid
go



CREATE TABLE flow.KeywordAttr
(
  container ENTITYID NOT NULL,
  id INT NOT NULL,
  CONSTRAINT "PK_KeywordAttr" UNIQUE (container, id)
)
go

INSERT INTO flow.KeywordAttr (container, id)
SELECT DISTINCT OBJ.container, PROP.keywordid AS id
FROM flow.object OBJ INNER JOIN
  flow.keyword PROP ON OBJ.rowid = PROP.objectid
go



CREATE TABLE flow.GraphAttr
(
  container ENTITYID NOT NULL,
  id INT NOT NULL,
  CONSTRAINT "PK_GraphAttr" UNIQUE (container, id)
)
go

INSERT INTO flow.GraphAttr (container, id)
SELECT DISTINCT OBJ.container, PROP.graphid AS id
FROM flow.object OBJ INNER JOIN
  flow.graph PROP ON OBJ.rowid = PROP.objectid
go