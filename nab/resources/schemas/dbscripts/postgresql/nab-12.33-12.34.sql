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

DELETE FROM exp.ObjectProperty
    WHERE ObjectId IN (SELECT ObjectID FROM nab.NabSpecimen) AND
      (PropertyId IN (SELECT PropertyId FROM exp.PropertyDescriptor pd WHERE
        (pd.PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
         pd.PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
         pd.PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
         pd.PropertyURI LIKE '%:NabProperty%:AUC%' OR
         pd.PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
         pd.PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
         pd.PropertyURI LIKE '%:NabProperty%:Curve+IC%')));

DELETE FROM exp.PropertyDescriptor
    WHERE Container IN (SELECT Container FROM exp.ExperimentRun er, nab.NabSpecimen ns WHERE ns.RunId = er.RowId) AND
	  (PropertyURI LIKE '%:NabProperty%:SpecimenLsid' OR
	   PropertyURI LIKE '%:NabProperty%:Fit+Error' OR
	   PropertyURI LIKE '%:NabProperty%:WellgroupName' OR
	   PropertyURI LIKE '%:NabProperty%:AUC%' OR
	   PropertyURI LIKE '%:NabProperty%:PositiveAUC%' OR
	   PropertyURI LIKE '%:NabProperty%:Point+IC%' OR
	   PropertyURI LIKE '%:NabProperty%:Curve+IC%');