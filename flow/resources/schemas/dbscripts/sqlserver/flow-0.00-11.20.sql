/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

/* flow-0.00-10.20.sql */

/* flow-0.00-8.10.sql */

CREATE SCHEMA flow;
GO

CREATE TABLE flow.Attribute
(
    RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256) COLLATE Latin1_General_BIN NOT NULL,

    CONSTRAINT PK_Attribute PRIMARY KEY (RowId),
    CONSTRAINT UQ_Attribute UNIQUE(Name)
);

CREATE TABLE flow.Object
(
    RowId INT IDENTITY(1,1) NOT NULL,
    Container ENTITYID NOT NULL,
    DataId INT,
    TypeId INT NOT NULL,
    Uri VARCHAR(400),
    CompId INT,
    ScriptId INT,
    FcsId INT,

    CONSTRAINT PK_Object PRIMARY KEY (RowId),
    CONSTRAINT UQ_Object UNIQUE(DataId),
    CONSTRAINT FK_Object_Data FOREIGN KEY(DataId) REFERENCES exp.Data(RowId)
);
CREATE INDEX flow_object_typeid ON flow.object (container, typeid);

CREATE TABLE flow.Keyword
(
    ObjectId INT NOT NULL,
    KeywordId INT NOT NULL,
    Value NTEXT,

    CONSTRAINT PK_Keyword PRIMARY KEY CLUSTERED (ObjectId, KeywordId),
    CONSTRAINT FK_Keyword_Object FOREIGN KEY(ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Keyword_Attribute FOREIGN KEY (KeywordId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Statistic
(
    ObjectId INT NOT NULL,
    StatisticId INT NOT NULL,
    Value FLOAT NOT NULL,

    CONSTRAINT PK_Statistic PRIMARY KEY CLUSTERED (ObjectId, StatisticId),
    CONSTRAINT FK_Statistic_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Statistic_Attribute FOREIGN KEY (StatisticId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Graph
(
    RowId INT IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    GraphId INT NOT NULL,
    Data IMAGE,

    CONSTRAINT PK_Graph PRIMARY KEY(RowId),
    CONSTRAINT UQ_Graph UNIQUE(ObjectId, GraphId),
    CONSTRAINT FK_Graph_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Graph_Attribute FOREIGN KEY (GraphId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Script
(
    RowId INT IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    Text NTEXT,

    CONSTRAINT PK_Script PRIMARY KEY(RowId),
    CONSTRAINT UQ_Script UNIQUE(ObjectId),
    CONSTRAINT FK_Script_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId)
);

/* flow-10.10-10.20.sql */

/**
 * the query to find the in use statistic/graph/keyword ids is way too expensive
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

/* flow-11.10-11.20.sql */

/* flow-11.10-11.19.sql */

-- KeywordAttr ------------------

-- Add RowId and Name to KeywordAttr
ALTER TABLE flow.KeywordAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

GO

ALTER TABLE flow.KeywordAttr DROP CONSTRAINT "PK_KeywordAttr";
ALTER TABLE flow.KeywordAttr ADD CONSTRAINT PK_KeywordAttr PRIMARY KEY (RowId);

-- Copy 'Attribute.Name' into 'KeywordAttr.Name'
UPDATE flow.KeywordAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = KeywordAttr.Id;

ALTER TABLE flow.KeywordAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL;
ALTER TABLE flow.KeywordAttr ADD CONSTRAINT UQ_KeywordAttr UNIQUE (Container, Name);

-- Drop the PK_Keyword for the next update and add it again afterwards.
ALTER TABLE flow.Keyword DROP CONSTRAINT PK_Keyword;
ALTER TABLE flow.Keyword DROP CONSTRAINT FK_Keyword_Attribute;

-- Change 'Keyword.KeywordId' to point at 'KeywordAttr.RowId'
UPDATE flow.Keyword
SET KeywordId =
  (SELECT KeywordAttr.RowId FROM flow.KeywordAttr, flow.Object
   WHERE KeywordAttr.Id = Keyword.KeywordId
   AND KeywordAttr.Container = Object.Container
   AND Object.RowId = Keyword.ObjectId);

-- Correct PK_Keyword will be added in flow-11.19-11.191 script
--ALTER TABLE flow.Keyword ADD CONSTRAINT PK_Keyword UNIQUE (ObjectId, KeywordId);
ALTER TABLE flow.Keyword ADD CONSTRAINT FK_Keyword_KeywordAttr FOREIGN KEY (KeywordId) REFERENCES flow.KeywordAttr (RowId);

-- Change meaning of 'KeywordAttr.Id' from FK Attribute.RowId to self KeywordAttr.RowId and equal to Keyword.KeywordId
-- When KeywordAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.KeywordAttr SET Id = RowId;


-- StatisticAttr ------------------

-- Add RowId and Name to StatisticAttr
ALTER TABLE flow.StatisticAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

GO

ALTER TABLE flow.StatisticAttr DROP CONSTRAINT "PK_StatistiAttr";
ALTER TABLE flow.StatisticAttr ADD CONSTRAINT PK_StatisticAttr PRIMARY KEY (RowId);

-- Copy 'Attribute.Name' into 'StatisticAttr.Name'
UPDATE flow.StatisticAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = StatisticAttr.Id;

ALTER TABLE flow.StatisticAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL;
ALTER TABLE flow.StatisticAttr ADD CONSTRAINT UQ_StatisticAttr UNIQUE (Container, Name);

-- Drop the PK_Statistic for the next update and add it again afterwards.
ALTER TABLE flow.Statistic DROP CONSTRAINT PK_Statistic;
ALTER TABLE flow.Statistic DROP CONSTRAINT FK_Statistic_Attribute;

-- Change 'Statistic.StatisticId' to point at 'StatisticAttr.RowId'
UPDATE flow.Statistic
SET StatisticId =
  (SELECT StatisticAttr.RowId FROM flow.StatisticAttr, flow.Object
   WHERE StatisticAttr.Id = Statistic.StatisticId
   AND StatisticAttr.Container = Object.Container
   AND Object.RowId = Statistic.ObjectId);

-- Correct PK_Statistic will be added in flow-11.19-11.191 script
--ALTER TABLE flow.Statistic ADD CONSTRAINT PK_Statistic UNIQUE (ObjectId, StatisticId);
ALTER TABLE flow.Statistic ADD CONSTRAINT FK_Statistic_StatisticAttr FOREIGN KEY (StatisticId) REFERENCES flow.StatisticAttr (RowId);

-- Change meaning of 'StatisticAttr.Id' from FK Attribute.RowId to self StatisticAttr.RowId and equal to Statistic.StatisticId
-- When StatisticAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.StatisticAttr SET Id = RowId;


-- GraphAttr ------------------

-- Add RowId and Name to GraphAttr
ALTER TABLE flow.GraphAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

GO

ALTER TABLE flow.GraphAttr DROP CONSTRAINT "PK_GraphAttr";
ALTER TABLE flow.GraphAttr ADD CONSTRAINT PK_GraphAttr PRIMARY KEY (RowId);

-- Copy 'Attribute.Name' into 'GraphAttr.Name'
UPDATE flow.GraphAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = GraphAttr.Id;

ALTER TABLE flow.GraphAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL;
ALTER TABLE flow.GraphAttr ADD CONSTRAINT UQ_GraphAttr UNIQUE (Container, Name);

-- Drop the PK_Graph for the next update and add it again afterwards.
ALTER TABLE flow.Graph DROP CONSTRAINT PK_Graph;
ALTER TABLE flow.Graph DROP CONSTRAINT UQ_Graph;
ALTER TABLE flow.Graph DROP CONSTRAINT FK_Graph_Attribute;

-- Change 'Graph.GraphId' to point at 'GraphAttr.RowId'
UPDATE flow.Graph
SET GraphId =
  (SELECT GraphAttr.RowId FROM flow.GraphAttr, flow.Object
   WHERE GraphAttr.Id = Graph.GraphId
   AND GraphAttr.Container = Object.Container
   AND Object.RowId = Graph.ObjectId);

-- Correct PK_Graph will be added in flow-11.19-11.191 script
--ALTER TABLE flow.Graph ADD CONSTRAINT PK_Graph UNIQUE (ObjectId, GraphId);
ALTER TABLE flow.Graph ADD CONSTRAINT FK_Graph_GraphAttr FOREIGN KEY (GraphId) REFERENCES flow.GraphAttr (RowId);

-- Change meaning of 'GraphAttr.Id' from FK Attribute.RowId to self GraphAttr.RowId and equal to Graph.GraphId
-- When GraphAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.GraphAttr SET Id = RowId;

/* flow-11.19-11.191.sql */

-- Change 'PK_*' unique constraints to actually be primary key constraint.
EXEC core.fn_dropifexists 'Keyword', 'flow', 'constraint', 'PK_Keyword';
ALTER TABLE flow.Keyword ADD CONSTRAINT PK_Keyword PRIMARY KEY CLUSTERED (ObjectId, KeywordId);

EXEC core.fn_dropifexists 'Statistic', 'flow', 'constraint', 'PK_Statistic';
ALTER TABLE flow.Statistic ADD CONSTRAINT PK_Statistic PRIMARY KEY CLUSTERED (ObjectId, StatisticId);

ALTER TABLE flow.Graph DROP COLUMN rowid;
EXEC core.fn_dropifexists 'Graph', 'flow', 'constraint', 'PK_Graph';
EXEC core.fn_dropifexists 'Graph', 'flow', 'constraint', 'UQ_Graph';
ALTER TABLE flow.Graph ADD CONSTRAINT PK_Graph PRIMARY KEY CLUSTERED (ObjectId, GraphId);

-- flow.Attribute table no longer used
EXEC core.fn_dropifexists 'Attribute', 'flow', 'TABLE', NULL;