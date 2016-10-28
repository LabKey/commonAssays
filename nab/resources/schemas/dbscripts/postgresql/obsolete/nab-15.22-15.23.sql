/*
 * Copyright (c) 2015-2016 LabKey Corporation
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

ALTER TABLE nab.NabSpecimen ALTER COLUMN FitError TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_Poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN AUC_5pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.NabSpecimen ALTER COLUMN PositiveAUC_5pl TYPE DOUBLE PRECISION;

ALTER TABLE nab.CutoffValue ALTER COLUMN Cutoff TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN Point TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_Poly TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_4pl TYPE DOUBLE PRECISION;
ALTER TABLE nab.CutoffValue ALTER COLUMN IC_5pl TYPE DOUBLE PRECISION;