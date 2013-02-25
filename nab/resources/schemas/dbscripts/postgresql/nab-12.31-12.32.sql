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

ALTER TABLE nab.cutoffvalue ADD CONSTRAINT fk_cutoffvalue_nabspecimen FOREIGN KEY (nabspecimenid)
        REFERENCES nab.nabspecimen (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE nab.nabspecimen ADD CONSTRAINT fk_nabspecimen_protocolid FOREIGN KEY (protocolid)
        REFERENCES exp.protocol (rowid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION;
