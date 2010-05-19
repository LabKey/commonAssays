/*
 * Copyright (c) 2010 LabKey Corporation
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
-- the combined BaseSamples and Samples columns
SELECT
Samples.SampleSet,
Samples.Flag AS DerivedFlag,
Samples.Run.Input.Sample.Flag AS BaseFlag,
--Samples.PTID_VISITNO,
Samples.Run.Input.Sample.PTID,
Samples.Run.Input.Sample.VISITNO,
Samples.Run.Input.Sample.DRAWDT,
Samples.Run.Input.Sample.NETWORK,
Samples.Run.Input.Sample.LABID,
Samples.Run.Input.Sample.SPECROLE,
Samples.Run.Input.Sample.PTIDTYPE,
Samples.Run.Input.Sample.CTRSAMPNAME,
Samples.Run.Input.Sample.METHOD,
Samples."ASSAY ID",
Samples.SAMP_ORD,
Samples.VIALID,
Samples."PLT TEMPLATE",
Samples.TESTDT,
Samples."Collection Num",
Samples.GUAVA_DATA_ID,
Samples.VIABL,
Samples.RECOVR,
Samples."mls R10 (2M/ml)",
Samples."mls R10 (5M/ml)"
FROM Samples
