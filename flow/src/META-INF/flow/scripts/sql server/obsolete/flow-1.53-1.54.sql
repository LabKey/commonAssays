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
exec sp_addAppRole 'flow', 'password';

CREATE TABLE flow.Attribute(
    RowId int IDENTITY(1,1) NOT NULL,
    Name NVARCHAR(256) COLLATE Latin1_General_BIN NOT NULL,
    CONSTRAINT PK_Attribute PRIMARY KEY (RowId),
    CONSTRAINT UQ_Attribute UNIQUE(Name)
);

CREATE TABLE flow.Object(
    RowId int IDENTITY(1,1) NOT NULL,
    DataId INT,
    TypeId INT NOT NULL,
    Uri VARCHAR(400),
    CONSTRAINT PK_Object PRIMARY KEY(RowId),
    CONSTRAINT FK_Object_Data FOREIGN KEY(DataId) REFERENCES exp.Data(RowId)
);

CREATE TABLE flow.Keyword(
    RowId int IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    KeywordId INT NOT NULL,
    Value NVARCHAR(1024) NOT NULL,
    CONSTRAINT PK_Keyword PRIMARY KEY (RowId),
    CONSTRAINT UQ_Keyword UNIQUE(ObjectId, KeywordId),
    CONSTRAINT FK_Keyword_Object FOREIGN KEY(ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Keyword_Attribute FOREIGN KEY (KeywordId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Statistic(
    RowId int IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    StatisticId INT NOT NULL,
    Value FLOAT NOT NULL,
    CONSTRAINT PK_Statistic PRIMARY KEY (RowId),
    CONSTRAINT UQ_Statistic UNIQUE(ObjectId, StatisticId),
    CONSTRAINT FK_Statistic_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Statistic_Attribute FOREIGN KEY (StatisticId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Graph(
    RowId int IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    GraphId INT NOT NULL,
    Data IMAGE,
    CONSTRAINT PK_Graph PRIMARY KEY(RowId),
    CONSTRAINT UQ_Graph UNIQUE(ObjectId, GraphId),
    CONSTRAINT FK_Graph_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId),
    CONSTRAINT FK_Graph_Attribute FOREIGN KEY (GraphId) REFERENCES flow.Attribute(RowId)
);

CREATE TABLE flow.Script(
    RowId int IDENTITY(1,1) NOT NULL,
    ObjectId INT NOT NULL,
    Text NTEXT,
    CONSTRAINT PK_Script PRIMARY KEY(RowId),
    CONSTRAINT UQ_Script UNIQUE(ObjectId),
    CONSTRAINT FK_Script_Object FOREIGN KEY (ObjectId) REFERENCES flow.Object(RowId)
);