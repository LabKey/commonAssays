/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
/* Create and Index on ms2.Runs(Container) to speed-up MS1 Feature to MS2 Peptide matching */
/* SQL Server Version */

EXEC core.fn_dropifexists 'Runs', 'ms2', 'INDEX','MS2Runs_Container'
GO
CREATE INDEX MS2Runs_Container ON ms2.Runs(Container)
GO
