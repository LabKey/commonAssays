/*
 * Copyright (c) 2005-2008 LabKey Corporation
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
if NOT EXISTS (select * from systypes where name ='LSIDtype')
    exec sp_addtype 'LSIDtype', 'nvarchar(300)'
GO

ALTER TABLE dbo.MS2Runs ADD ApplicationLSID LSIDType NULL
GO

ALTER TABLE ProtAnnotations DROP CONSTRAINT FKProtAnnotationsProtSequences
GO
ALTER TABLE ProtAnnotations ADD CONSTRAINT FKProtAnnotationsProtSequences FOREIGN KEY (SeqId) REFERENCES ProtSequences (SeqId)
GO
ALTER TABLE ProtOrganisms DROP CONSTRAINT FK_Organism_ProtIdentifiers 
GO
ALTER TABLE ProtOrganisms ADD CONSTRAINT FK_Organism_ProtIdentifiers FOREIGN KEY (IdentId) REFERENCES ProtIdentifiers (IdentId) 
GO

ALTER TABLE MS2PeptidesData ADD SeqId INT NULL
GO