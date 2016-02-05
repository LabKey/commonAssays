/*
 * Copyright (c) 2011-2016 LabKey Corporation
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

-- Tables used for Microarray module
CREATE SCHEMA microarray;
GO

CREATE TABLE microarray.geo_properties
(
    rowid INT identity(1,1) NOT NULL,
    prop_name VARCHAR(200),
    category VARCHAR(200),
    value TEXT,
    container ENTITYID,
    created DATETIME,
    createdby INTEGER,
    modified DATETIME,
    modifiedby INTEGER,

    CONSTRAINT PK_geo_properties PRIMARY KEY (rowid)
);