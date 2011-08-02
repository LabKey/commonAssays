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


-- Change 'PK_*' unique constraints to actually be primary key constraint.
EXEC core.fn_dropifexists 'Keyword', 'flow', 'constraint', 'PK_Keyword'
GO
ALTER TABLE flow.Keyword ADD CONSTRAINT PK_Keyword PRIMARY KEY CLUSTERED (ObjectId, KeywordId);
GO

EXEC core.fn_dropifexists 'Statistic', 'flow', 'constraint', 'PK_Statistic'
GO
ALTER TABLE flow.Statistic ADD CONSTRAINT PK_Statistic PRIMARY KEY CLUSTERED (ObjectId, StatisticId);
GO

ALTER TABLE flow.Graph DROP COLUMN rowid
GO
EXEC core.fn_dropifexists 'Graph', 'flow', 'constraint', 'PK_Graph'
GO
EXEC core.fn_dropifexists 'Graph', 'flow', 'constraint', 'UQ_Graph'
GO
ALTER TABLE flow.Graph ADD CONSTRAINT PK_Graph PRIMARY KEY CLUSTERED (ObjectId, GraphId);
GO


-- flow.Attribute table no longer used
EXEC core.fn_dropifexists 'Attribute', 'flow', 'TABLE', NULL
GO

