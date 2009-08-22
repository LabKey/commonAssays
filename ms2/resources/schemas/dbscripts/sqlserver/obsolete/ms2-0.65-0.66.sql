/*
 * Copyright (c) 2005-2008 Fred Hutchinson Cancer Research Center
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
/****** ProtSprotOrgMap                                  */
IF OBJECT_ID('ProtSprotOrgMap','U') IS NOT NULL
	DROP TABLE ProtSprotOrgMap
GO
CREATE TABLE ProtSprotOrgMap
	(
	SprotSuffix VARCHAR(5) NOT NULL,
	SuperKingdomCode CHAR(1) NULL,
	TaxonId INT NULL,
	FullName VARCHAR(200) NOT NULL,
	Genus VARCHAR(100) NOT NULL,
	Species VARCHAR(100) NOT NULL,
	CommonName VARCHAR(200) NULL,
	Synonym VARCHAR(200) NULL,

	CONSTRAINT PK_ProtSprotOrgMap PRIMARY KEY (SprotSuffix)
	)
GO

