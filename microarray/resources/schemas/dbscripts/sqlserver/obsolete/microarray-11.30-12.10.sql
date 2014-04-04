/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

create table microarray.geo_properties (
  rowid int identity(1,1) not null,
  prop_name varchar(200),
  category varchar(200),
  value text,
  container entityid,
  created datetime,
  createdby integer,
  modified datetime,
  modifiedby integer,

  constraint PK_geo_properties primary key (rowid)
);

