/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

/* SQL Server Version */

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_TrimmedPeptide'
GO
CREATE INDEX IX_MS2PeptidesData_TrimmedPeptide ON
ms2.PeptidesData(TrimmedPeptide)
GO

EXEC core.fn_dropifexists 'PeptidesData', 'ms2', 'INDEX','IX_MS2PeptidesData_Peptide'
GO
CREATE INDEX IX_MS2PeptidesData_Peptide ON
ms2.PeptidesData(Peptide)
GO
