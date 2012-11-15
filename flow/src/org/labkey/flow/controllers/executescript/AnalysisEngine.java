/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.flow.controllers.executescript;

/**
* User: kevink
* Date: 10/21/12
*/
public enum AnalysisEngine
{
    // LabKey's analysis engine
    LabKey(true),

    // Analysis results read by LabKey's FlowJo workspace parser
    FlowJoWorkspace(false),

    // Execute external FlowJo process
    FlowJo(true),

    // Execute external R process
    R(true),

    // Generic external analysis archive
    Archive(false);

    private boolean _requiresPipeline;

    AnalysisEngine(boolean requiresPipeline)
    {
        _requiresPipeline = requiresPipeline;
    }

    public boolean requiresPipeline()
    {
        return _requiresPipeline;
    }
}
