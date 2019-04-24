/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

/* ms2-16.30-16.31.sql */

ALTER TABLE ms2.itraqproteinquantitation ADD Ratio9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Error9 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Ratio10 REAL;
ALTER TABLE ms2.itraqproteinquantitation ADD Error10 REAL;

ALTER TABLE ms2.itraqpeptidequantitation ADD TargetMass9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD AbsoluteIntensity9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD Normalized9 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD TargetMass10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD AbsoluteIntensity10 REAL;
ALTER TABLE ms2.itraqpeptidequantitation ADD Normalized10 REAL;