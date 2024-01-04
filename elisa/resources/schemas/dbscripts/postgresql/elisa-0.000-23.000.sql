/*
 * Copyright (c) 2020 LabKey Corporation
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
CREATE SCHEMA elisa;

CREATE TABLE elisa.CurveFit
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

CREATE INDEX IX_CurveFit_ProtocolId ON elisa.CurveFit(ProtocolId);
CREATE INDEX IX_CurveFit_RunId ON elisa.CurveFit(RunId);

SELECT core.executeJavaUpgradeCode('moveCurveFitData');