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
USE ms2

-- Drop MS2 views
IF OBJECT_ID('ProteinDBs', 'V') IS NOT NULL
	DROP VIEW ProteinDBs
IF OBJECT_ID('Proteins', 'V') IS NOT NULL
	DROP VIEW Proteins
IF OBJECT_ID('Runs', 'V') IS NOT NULL
	DROP VIEW Runs
IF OBJECT_ID('MS2Peptides', 'V') IS NOT NULL
	DROP VIEW MS2Peptides
IF OBJECT_ID('MS2Spectra', 'V') IS NOT NULL
	DROP VIEW MS2Spectra
GO


-- Drop MS2 tables
IF OBJECT_ID('MS2Runs','U') IS NOT NULL
	DROP TABLE MS2Runs
IF OBJECT_ID('MS2Modifications','U') IS NOT NULL
	DROP TABLE MS2Modifications
IF OBJECT_ID('MS2Fractions','U') IS NOT NULL
	DROP TABLE MS2Fractions
IF OBJECT_ID('MS2PeptidesData','U') IS NOT NULL
	DROP TABLE MS2PeptidesData
IF OBJECT_ID('MS2SpectraData','U') IS NOT NULL
	DROP TABLE MS2SpectraData
IF OBJECT_ID('ProteinDataBases','U') IS NOT NULL
	DROP TABLE ProteinDataBases
IF OBJECT_ID('ProteinSequences','U') IS NOT NULL
	DROP TABLE ProteinSequences
IF OBJECT_ID('ProteinNames','U') IS NOT NULL
	DROP TABLE ProteinNames
IF OBJECT_ID('MS2History','U') IS NOT NULL
	DROP TABLE MS2History
GO


-- Drop protein annotations tables
IF OBJECT_ID('ProtSprotOrgMap','U') IS NOT NULL
	DROP TABLE ProtSprotOrgMap
IF OBJECT_ID('ProtFastas','U') IS NOT NULL
	DROP TABLE ProtFastas
IF OBJECT_ID('ProtAnnotations','U') IS NOT NULL
	DROP TABLE ProtAnnotations
IF OBJECT_ID('ProtOrganisms','U') IS NOT NULL
	DROP TABLE ProtOrganisms
IF OBJECT_ID('ProtIdentifiers','U') IS NOT NULL
	DROP TABLE ProtIdentifiers
IF OBJECT_ID('ProtSequences','U') IS NOT NULL
	DROP TABLE ProtSequences
IF OBJECT_ID('ProtIdentTypes', 'U') IS NOT NULL
	DROP TABLE ProtIdentTypes
IF OBJECT_ID('ProtAnnotationTypes','U') IS NOT NULL
	DROP TABLE ProtAnnotationTypes
IF OBJECT_ID('ProtInfoSources', 'U') IS NOT NULL
	DROP TABLE ProtInfoSources
IF OBJECT_ID('ProtAnnotInsertions', 'U') IS NOT NULL
	DROP TABLE ProtAnnotInsertions
GO


IF EXISTS (SELECT * FROM SYSTYPES WHERE name ='ENTITYID')
	EXEC sp_droptype 'ENTITYID'
IF EXISTS (SELECT * FROM SYSTYPES WHERE name ='USERID')
	EXEC sp_droptype 'USERID'
GO
