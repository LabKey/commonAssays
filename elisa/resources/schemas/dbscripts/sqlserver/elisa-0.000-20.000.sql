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
GO

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
GO

EXEC core.executeJavaUpgradeCode 'moveCurveFitData';
