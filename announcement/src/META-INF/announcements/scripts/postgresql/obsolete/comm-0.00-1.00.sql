/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

-- Create schema comm: tables for Announcements and Wiki

CREATE SCHEMA comm;

CREATE TABLE comm.Announcements
	(
	RowId SERIAL,
	EntityId ENTITYID NOT NULL,
	CreatedBy USERID,
	Created TIMESTAMP,
	ModifiedBy USERID,
	Modified TIMESTAMP,
	Owner USERID,
	Container ENTITYID NOT NULL,
	Parent ENTITYID,
	Title VARCHAR(255),
	Expires TIMESTAMP,
	Body TEXT,

	CONSTRAINT PK_Announcements PRIMARY KEY (RowId),
	CONSTRAINT UQ_Announcements UNIQUE (Container, Parent, RowId)
	);


CREATE TABLE comm.Pages
       (
       RowId SERIAL,
       EntityId ENTITYID NOT NULL,
       CreatedBy USERID,
       Created TIMESTAMP,
       ModifiedBy USERID,
       Modified TIMESTAMP,
       Owner USERID,
       Container ENTITYID NOT NULL,
       Name VARCHAR(255) NOT NULL,
       Title VARCHAR(255),
       Body TEXT,

       CONSTRAINT PK_Pages PRIMARY KEY (EntityId),
       CONSTRAINT UQ_Pages UNIQUE (Container, Name)
       );