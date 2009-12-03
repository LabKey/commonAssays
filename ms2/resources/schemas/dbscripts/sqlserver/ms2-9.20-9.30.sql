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
    WHERE Name = 'GeneCards'
GO

/* ms2-9.21-9.22.sql */

ALTER TABLE ms2.peptidesdata ALTER COLUMN score1 REAL NULL;
ALTER TABLE ms2.peptidesdata ALTER COLUMN score2 REAL NULL;
ALTER TABLE ms2.peptidesdata ALTER COLUMN score3 REAL NULL;

-- It's a real pain to drop defaults in SQL Server if they weren't created with a specific name
declare @name nvarchar(32),
    @sql nvarchar(1000)

-- find constraint name for first score column
select @name = O.name
from sysobjects AS O
left join sysobjects AS T
    on O.parent_obj = T.id
where isnull(objectproperty(O.id,'IsMSShipped'),1) = 0
    and O.name not like '%dtproper%'
    and O.name not like 'dt[_]%'
	and T.name = 'peptidesdata'
    and O.name like 'DF__MS2Peptid%__Score%'
-- delete if found
if not @name is null
begin
    select @sql = 'ALTER TABLE ms2.peptidesdata DROP CONSTRAINT [' + @name + ']'
    execute sp_executesql @sql
end

select @name = null

-- find constraint name for second score column
select @name = O.name
from sysobjects AS O
left join sysobjects AS T
    on O.parent_obj = T.id
where isnull(objectproperty(O.id,'IsMSShipped'),1) = 0
    and O.name not like '%dtproper%'
    and O.name not like 'dt[_]%'
	and T.name = 'peptidesdata'
    and O.name like 'DF__MS2Peptid%__Score%'
-- delete if found
if not @name is null
begin
    select @sql = 'ALTER TABLE ms2.peptidesdata DROP CONSTRAINT [' + @name + ']'
    execute sp_executesql @sql
end

select @name = null

-- find constraint name for third score column
select @name = O.name
from sysobjects AS O
left join sysobjects AS T
    on O.parent_obj = T.id
where isnull(objectproperty(O.id,'IsMSShipped'),1) = 0
    and O.name not like '%dtproper%'
    and O.name not like 'dt[_]%'
	and T.name = 'peptidesdata'
    and O.name like 'DF__MS2Peptid%__Score%'
-- delete if found
if not @name is null
begin
    select @sql = 'ALTER TABLE ms2.peptidesdata DROP CONSTRAINT [' + @name + ']'
    execute sp_executesql @sql
end