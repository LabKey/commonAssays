DROP TABLE luminex.DataRow
GO

DROP TABLE luminex.Analyte
GO

CREATE TABLE luminex.Analyte
(
	RowId INT IDENTITY(1,1) NOT NULL,
    Name VARCHAR(50) NOT NULL,
    DataId INT NOT NULL,
    FitProb REAL,
    ResVar REAL,
    RegressionType VARCHAR(100),
    StdCurve VARCHAR(255),
    MinStandardRecovery INT NOT NULL,
    MaxStandardRecovery INT NOT NULL,

	CONSTRAINT PK_Luminex_Analyte PRIMARY KEY (RowId),
	CONSTRAINT FK_LuminexAnalyte_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
)
GO

CREATE INDEX IX_LuminexAnalyte_DataId ON luminex.Analyte (DataId)
GO

CREATE TABLE luminex.DataRow
(
	RowId INT IDENTITY(1,1) NOT NULL,
    DataId INT NOT NULL,
    AnalyteId INT NOT NULL,
    Type VARCHAR(10),
    Well VARCHAR(50),
    Outlier BIT,
    Description VARCHAR(50),
    FIString VARCHAR(20),
    FI REAL,
    FIOORIndicator VARCHAR(10),
    FIBackgroundString VARCHAR(20),
    FIBackground REAL,
    FIBackgroundOORIndicator VARCHAR(10),
    StdDevString VARCHAR(20),
    StdDev REAL,
    StdDevOORIndicator VARCHAR(10),
    ObsConcString VARCHAR(20),
    ObsConc REAL,
    ObsConcOORIndicator VARCHAR(10),
    ExpConc REAL,
    ObsOverExp REAL,
    ConcInRangeString VARCHAR(20),
    ConcInRange REAL,
    ConcInRangeOORIndicator VARCHAR(10),

	CONSTRAINT PK_Luminex_DataRow PRIMARY KEY (RowId),
	CONSTRAINT FK_LuminexDataRow_DataId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId),
	CONSTRAINT FK_LuminexDataRow_AnalyteId FOREIGN KEY (DataId) REFERENCES exp.Data(RowId)
)
GO

CREATE INDEX IX_LuminexDataRow_DataId ON luminex.DataRow (DataId)
GO

CREATE INDEX IX_LuminexDataRow_AnalyteId ON luminex.DataRow (AnalyteId)
GO
