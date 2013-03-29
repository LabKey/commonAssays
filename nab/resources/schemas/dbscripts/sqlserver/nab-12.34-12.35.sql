/*
 * Copyright (c) 2013 LabKey Corporation
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

-- remove leftover object properties
DELETE FROM exp.ObjectProperty
    WHERE ObjectId IN (SELECT ObjectId FROM exp.Object WHERE ObjectURI LIKE 'urn:lsid:%AssayRunNabDataRow.%') AND
      (PropertyId IN (SELECT PropertyId FROM exp.PropertyDescriptor WHERE
        (PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
         PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
         PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
         PropertyURI LIKE '%:NabProperty%:AUC%' OR
         PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
         PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
         PropertyURI LIKE '%:NabProperty%:Curve+IC%')));

-- remove property descriptors we don't use anymore
DELETE FROM exp.PropertyDescriptor
    WHERE Container IN (SELECT Container FROM exp.ExperimentRun er, nab.NabSpecimen ns WHERE ns.RunId = er.RowId) AND
	  (PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
	   PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
	   PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
	   PropertyURI LIKE '%:NabProperty%:AUC%' OR
	   PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
	   PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
	   PropertyURI LIKE '%:NabProperty%:Curve+IC%');
