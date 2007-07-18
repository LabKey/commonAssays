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

