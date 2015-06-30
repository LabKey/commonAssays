/*
 * Copyright (c) 2015 LabKey Corporation
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

CREATE TABLE ms2.ExpressionData (
  RowId SERIAL,
  Value REAL,
  SeqId INT NOT NULL,
  SampleId INT NOT NULL,
  DataId INT NOT NULL,

  CONSTRAINT PK_ExpressionData PRIMARY KEY (RowId),
  CONSTRAINT FK_ExpressionData_SeqId FOREIGN KEY (SeqId) REFERENCES prot.sequences (SeqId),
  CONSTRAINT FK_ExpressionData_SampleId FOREIGN KEY (SampleId) REFERENCES exp.material (RowId),
  CONSTRAINT FK_ExpressionData_DataId FOREIGN KEY (DataId) REFERENCES exp.data (RowId)
);

CREATE INDEX IX_ExpressionData_SeqId ON ms2.ExpressionData(SeqId);
CREATE INDEX IX_ExpressionData_SampleId ON ms2.ExpressionData(SampleId);
CREATE INDEX IX_ExpressionData_DataId ON ms2.ExpressionData(DataId);

