/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetFValSeries TO FValSeries;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries1 TO ObsSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries2 TO ObsSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetObsSeries3 TO ObsSeries3;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries1 TO ModelPosSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries2 TO ModelPosSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelPosSeries3 TO ModelPosSeries3;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries1 TO ModelNegSeries1;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries2 TO ModelNegSeries2;
ALTER TABLE ms2.PeptideProphetSummaries RENAME PepProphetModelNegSeries3 TO ModelNegSeries3;

ALTER TABLE ms2.PeptideProphetSummaries
    ADD MinProbSeries BYTEA NULL,
    ADD SensitivitySeries BYTEA NULL,
    ADD ErrorSeries BYTEA NULL;
