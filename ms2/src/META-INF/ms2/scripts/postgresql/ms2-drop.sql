/*
 * Copyright (c) 2008 LabKey Corporation
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

-- DROP obsolete views.  Do not remove; these are needed when upgrading from older versions.
SELECT core.fn_dropifexists('ProteinDBs', 'prot', 'VIEW', NULL);
SELECT core.fn_dropifexists('MS2Spectra', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('MS2ExperimentRuns', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('MS2Peptides', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('MS2SimplePeptides', 'ms2', 'VIEW', NULL);

-- DROP current views.
SELECT core.fn_dropifexists('Peptides', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('SimplePeptides', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('ProteinGroupsWithQuantitation', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('ExperimentRuns', 'ms2', 'VIEW', NULL);
SELECT core.fn_dropifexists('FastaAdmin', 'prot', 'VIEW', NULL);
SELECT core.fn_dropifexists('Spectra', 'ms2', 'VIEW', NULL);
