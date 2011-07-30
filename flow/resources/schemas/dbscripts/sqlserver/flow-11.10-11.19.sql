/*
 * Copyright (c) 2011 LabKey Corporation
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

-- KeywordAttr ------------------

-- Add RowId and Name to KeywordAttr
ALTER TABLE flow.KeywordAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

ALTER TABLE flow.KeywordAttr DROP CONSTRAINT "PK_KeywordAttr";
ALTER TABLE flow.KeywordAttr ADD CONSTRAINT PK_KeywordAttr PRIMARY KEY (RowId);
GO

-- Copy 'Attribute.Name' into 'KeywordAttr.Name'
UPDATE flow.KeywordAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = KeywordAttr.Id
GO

ALTER TABLE flow.KeywordAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL
ALTER TABLE flow.KeywordAttr ADD CONSTRAINT UQ_KeywordAttr UNIQUE (Container, Name);
GO

-- Drop the PK_Keyword for the next update and add it again afterwards.
ALTER TABLE flow.Keyword DROP CONSTRAINT PK_Keyword;
ALTER TABLE flow.Keyword DROP CONSTRAINT FK_Keyword_Attribute;
GO

-- Change 'Keyword.KeywordId' to point at 'KeywordAttr.RowId'
UPDATE flow.Keyword
SET KeywordId =
  (SELECT KeywordAttr.RowId FROM flow.KeywordAttr, flow.Object
   WHERE KeywordAttr.Id = Keyword.KeywordId
   AND KeywordAttr.Container = Object.Container
   AND Object.RowId = Keyword.ObjectId)
GO

ALTER TABLE flow.Keyword ADD CONSTRAINT PK_Keyword UNIQUE (ObjectId, KeywordId);
ALTER TABLE flow.Keyword ADD CONSTRAINT FK_Keyword_KeywordAttr FOREIGN KEY (KeywordId) REFERENCES flow.KeywordAttr (RowId);
GO

-- Change meaning of 'KeywordAttr.Id' from FK Attribute.RowId to self KeywordAttr.RowId and equal to Keyword.KeywordId
-- When KeywordAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.KeywordAttr SET Id = RowId
GO


-- StatisticAttr ------------------

-- Add RowId and Name to StatisticAttr
ALTER TABLE flow.StatisticAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

ALTER TABLE flow.StatisticAttr DROP CONSTRAINT "PK_StatistiAttr";
ALTER TABLE flow.StatisticAttr ADD CONSTRAINT PK_StatisticAttr PRIMARY KEY (RowId);
GO

-- Copy 'Attribute.Name' into 'StatisticAttr.Name'
UPDATE flow.StatisticAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = StatisticAttr.Id
GO

ALTER TABLE flow.StatisticAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL
ALTER TABLE flow.StatisticAttr ADD CONSTRAINT UQ_StatisticAttr UNIQUE (Container, Name);
GO

-- Drop the PK_Statistic for the next update and add it again afterwards.
ALTER TABLE flow.Statistic DROP CONSTRAINT PK_Statistic;
ALTER TABLE flow.Statistic DROP CONSTRAINT FK_Statistic_Attribute;
GO

-- Change 'Statistic.StatisticId' to point at 'StatisticAttr.RowId'
UPDATE flow.Statistic
SET StatisticId =
  (SELECT StatisticAttr.RowId FROM flow.StatisticAttr, flow.Object
   WHERE StatisticAttr.Id = Statistic.StatisticId
   AND StatisticAttr.Container = Object.Container
   AND Object.RowId = Statistic.ObjectId)
GO

ALTER TABLE flow.Statistic ADD CONSTRAINT PK_Statistic UNIQUE (ObjectId, StatisticId);
ALTER TABLE flow.Statistic ADD CONSTRAINT FK_Statistic_StatisticAttr FOREIGN KEY (StatisticId) REFERENCES flow.StatisticAttr (RowId);
GO

-- Change meaning of 'StatisticAttr.Id' from FK Attribute.RowId to self StatisticAttr.RowId and equal to Statistic.StatisticId
-- When StatisticAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.StatisticAttr SET Id = RowId
GO


-- GraphAttr ------------------

-- Add RowId and Name to GraphAttr
ALTER TABLE flow.GraphAttr
    ADD RowId INT IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256);

ALTER TABLE flow.GraphAttr DROP CONSTRAINT "PK_GraphAttr";
ALTER TABLE flow.GraphAttr ADD CONSTRAINT PK_GraphAttr PRIMARY KEY (RowId);
GO

-- Copy 'Attribute.Name' into 'GraphAttr.Name'
UPDATE flow.GraphAttr
    SET Name = Attribute.Name
    FROM flow.Attribute
    WHERE Attribute.RowId = GraphAttr.Id
GO

ALTER TABLE flow.GraphAttr ALTER COLUMN Name NVARCHAR(256) NOT NULL
ALTER TABLE flow.GraphAttr ADD CONSTRAINT UQ_GraphAttr UNIQUE (Container, Name);
GO

-- Drop the PK_Graph for the next update and add it again afterwards.
ALTER TABLE flow.Graph DROP CONSTRAINT PK_Graph;
ALTER TABLE flow.Graph DROP CONSTRAINT FK_Graph_Attribute;
GO

-- Change 'Graph.GraphId' to point at 'GraphAttr.RowId'
UPDATE flow.Graph
SET GraphId =
  (SELECT GraphAttr.RowId FROM flow.GraphAttr, flow.Object
   WHERE GraphAttr.Id = Graph.GraphId
   AND GraphAttr.Container = Object.Container
   AND Object.RowId = Graph.ObjectId)
GO

ALTER TABLE flow.Graph ADD CONSTRAINT PK_Graph UNIQUE (ObjectId, GraphId);
ALTER TABLE flow.Graph ADD CONSTRAINT FK_Graph_GraphAttr FOREIGN KEY (GraphId) REFERENCES flow.GraphAttr (RowId);
GO

-- Change meaning of 'GraphAttr.Id' from FK Attribute.RowId to self GraphAttr.RowId and equal to Graph.GraphId
-- When GraphAttr.Id == RowId, the Name column is the preferred name otherwise it is an alias.
UPDATE flow.GraphAttr SET Id = RowId
GO



