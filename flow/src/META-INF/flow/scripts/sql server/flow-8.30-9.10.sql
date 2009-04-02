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

/* flow-8.30-8.31.sql */

ALTER TABLE flow.keyword DROP CONSTRAINT PK_Keyword
ALTER TABLE flow.keyword DROP CONSTRAINT UQ_Keyword
ALTER TABLE flow.keyword DROP COLUMN rowid
go
ALTER TABLE flow.keyword ADD CONSTRAINT PK_Keyword PRIMARY KEY CLUSTERED (ObjectId, KeywordId)
go

ALTER TABLE flow.statistic DROP CONSTRAINT PK_Statistic
ALTER TABLE flow.statistic DROP CONSTRAINT UQ_Statistic
ALTER TABLE flow.statistic DROP COLUMN rowid
go
ALTER TABLE flow.statistic ADD CONSTRAINT PK_Statistic PRIMARY KEY CLUSTERED (ObjectId, StatisticId)
go