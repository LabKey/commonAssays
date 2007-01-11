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

ALTER TABLE comm.PageVersions ADD RendererType NVARCHAR(50) NOT NULL DEFAULT 'RADEOX'
ALTER TABLE comm.Announcements ADD RendererType NVARCHAR(50) NOT NULL DEFAULT 'RADEOX'
GO

UPDATE comm.PageVersions SET RendererType = 'HTML' WHERE Body LIKE '<div%'
UPDATE comm.Announcements SET RendererType = 'HTML' WHERE Body LIKE '<div%'
GO

ALTER TABLE comm.PageVersions DROP CONSTRAINT FK_PageVersions_Renderer
ALTER TABLE comm.PageVersions DROP COLUMN RendererId
DROP TABLE comm.Renderers

