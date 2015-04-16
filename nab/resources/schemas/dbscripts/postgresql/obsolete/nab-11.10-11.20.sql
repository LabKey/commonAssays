/*
 * Copyright (c) 2011-2015 LabKey Corporation
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

-- Set all of the NAb run properties that control the calculations to be not shown in update views
UPDATE exp.propertydescriptor SET showninupdateview = false WHERE propertyid IN
(
	SELECT pd.propertyid
	FROM exp.propertydescriptor pd, exp.propertydomain propdomain, exp.domaindescriptor dd
	WHERE
		(LOWER(pd.name) LIKE 'cutoff%' OR lower(pd.name) LIKE 'curvefitmethod' ) AND
		pd.propertyid = propdomain.propertyid AND
		dd.domainid = propdomain.domainid AND
		domainuri IN
		(
			-- Find all the NAb run domain URIs
			SELECT dd.domainuri
			FROM exp.object o, exp.objectproperty op, exp.protocol p, exp.domaindescriptor dd
			WHERE o.objecturi = p.lsid AND op.objectid = o.objectid AND op.stringvalue = dd.domainuri AND p.lsid LIKE '%:NabAssayProtocol.%' AND dd.domainuri LIKE '%:AssayDomain-Run.%'
		)
);