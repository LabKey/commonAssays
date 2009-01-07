/*
 * Copyright (c) 2008 LabKey Corporation
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

alter table flow.keyword drop constraint PK_Keyword;
alter table flow.keyword drop constraint UQ_Keyword;
alter table flow.keyword drop column rowid;
alter table flow.keyword add constraint PK_Keyword PRIMARY KEY (ObjectId, KeywordId);
cluster PK_Keyword on flow.keyword;

alter table flow.statistic drop constraint PK_Statistic;
alter table flow.statistic drop constraint UQ_Statistic;
alter table flow.statistic drop column rowid;
alter table flow.statistic add constraint PK_Statistic PRIMARY KEY (ObjectId, StatisticId);
cluster PK_Statistic on flow.statistic;