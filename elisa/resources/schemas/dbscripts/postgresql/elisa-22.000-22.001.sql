CREATE SCHEMA IF NOT EXISTS elisa;

CREATE TABLE IF NOT EXISTS elisa.CurveFit
(
    RowId SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    Created TIMESTAMP NOT NULL,
    CreatedBy USERID NOT NULL,
    Modified TIMESTAMP NOT NULL,
    ModifiedBy USERID NOT NULL,

    RunId INTEGER NOT NULL,
    ProtocolId INTEGER NOT NULL,
    PlateName VARCHAR(250),
    Spot INTEGER,
    RSquared DOUBLE PRECISION,
    FitParameters VARCHAR(500),

    CONSTRAINT PK_CurveFit PRIMARY KEY (RowId),
    CONSTRAINT FK_CurveFit_ProtocolId FOREIGN KEY (ProtocolId) REFERENCES exp.Protocol (RowId),
    CONSTRAINT FK_CurveFit_RunId FOREIGN KEY (RunId)
        REFERENCES exp.ExperimentRun (RowId) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX IF NOT EXISTS IX_CurveFit_ProtocolId ON elisa.CurveFit(ProtocolId);
CREATE INDEX IF NOT EXISTS IX_CurveFit_RunId ON elisa.CurveFit(RunId);

SELECT core.executeJavaUpgradeCode('moveCurveFitData');
