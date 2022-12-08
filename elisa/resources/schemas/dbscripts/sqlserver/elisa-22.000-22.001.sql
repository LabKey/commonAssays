IF NOT EXISTS(SELECT * FROM sys.Schemas WHERE name = 'elisa')
    BEGIN
        EXEC ('CREATE SCHEMA elisa');
    END
GO

IF OBJECT_ID(N'elisa.CurveFit', N'U') IS NULL BEGIN
    CREATE TABLE elisa.CurveFit
    (
        RowId INT IDENTITY (1, 1) NOT NULL,
        Container ENTITYID NOT NULL,
        Created DATETIME NOT NULL,
        CreatedBy USERID NOT NULL,
        Modified DATETIME NOT NULL,
        ModifiedBy USERID NOT NULL,

        RunId INT NOT NULL,
        ProtocolId INT NOT NULL,
        PlateName NVARCHAR(250),
        Spot INT,
        RSquared DOUBLE PRECISION,
        FitParameters NVARCHAR(500),

        CONSTRAINT PK_CurveFit PRIMARY KEY (RowId),
        CONSTRAINT FK_CurveFit_ProtocolId FOREIGN KEY (ProtocolId) REFERENCES exp.Protocol (RowId),
        CONSTRAINT FK_CurveFit_ExperimentRun FOREIGN KEY (RunId)
            REFERENCES exp.ExperimentRun (RowId)
            ON UPDATE NO ACTION ON DELETE NO ACTION
    );
END;

IF NOT EXISTS(SELECT * FROM sys.indexes WHERE name = 'IX_CurveFit_ProtocolId')
    BEGIN
        EXEC ('CREATE INDEX IX_CurveFit_ProtocolId ON elisa.CurveFit(ProtocolId)');
    END
GO
IF NOT EXISTS(SELECT * FROM sys.indexes WHERE name = 'IX_CurveFit_RunId')
    BEGIN
        EXEC ('CREATE INDEX IX_CurveFit_RunId ON elisa.CurveFit(RunId)');
    END
GO

EXEC core.executeJavaUpgradeCode 'moveCurveFitData';
