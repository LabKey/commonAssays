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
CREATE TABLE prot.CustomAnnotationSet
(
	CustomAnnotationSetId SERIAL NOT NULL PRIMARY KEY,
	Container EntityId NOT NULL,
	Name VARCHAR(200) NOT NULL,
	CreatedBy UserId,
	Created timestamp without time zone,
	ModifiedBy userid,
	Modified timestamp without time zone,
	CustomAnnotationType VARCHAR(20) NOT NULL,
	Lsid lsidtype,
	CONSTRAINT fk_CustomAnnotationSet_Container FOREIGN KEY (container) REFERENCES core.containers(EntityId),
	CONSTRAINT fk_CustomAnnotationSet_CreatedBy FOREIGN KEY (createdby) REFERENCES core.usersdata(userid),
	CONSTRAINT fk_CustomAnnotationSet_ModifiedBy FOREIGN KEY (modifiedby) REFERENCES core.usersdata(userid),
	CONSTRAINT uq_CustomAnnotationSet UNIQUE (Container, Name)
);

CREATE INDEX IX_CustomAnnotationSet_Container ON prot.CustomAnnotationSet(Container);

CREATE TABLE prot.CustomAnnotation
(
	CustomAnnotationId SERIAL NOT NULL PRIMARY KEY,
	CustomAnnotationSetId INT NOT NULL,
	ObjectURI LsidType NOT NULL,
	LookupString VARCHAR(200) NOT NULL,

	CONSTRAINT fk_CustomAnnotation_CustomAnnotationSetId FOREIGN KEY (CustomAnnotationSetId) REFERENCES prot.CustomAnnotationSet(CustomAnnotationSetId),
	CONSTRAINT UQ_CustomAnnotation_LookupString_SetId UNIQUE (LookupString, CustomAnnotationSetId),
	CONSTRAINT UQ_CustomAnnotation_ObjectURI UNIQUE (ObjectURI)
);

CREATE INDEX IX_CustomAnnotation_CustomAnnotationSetId ON prot.CustomAnnotation(CustomAnnotationSetId);

