/*
 * Copyright (c) 2006-2011 LabKey Corporation
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

package org.labkey.flow.persist;

public enum ObjectType
{
    fcsKeywords(1, InputRole.FCSFile),
    compensationControl(2, null),
    fcsAnalysis(3, null),
    compensationMatrix(4, InputRole.CompensationMatrix),
    script(5, InputRole.AnalysisScript),

    workspace_fcsAnalysis(6, null),
    workspace_script(7, InputRole.AnalysisScript),
    workspace(8, InputRole.Workspace)
    ;

    final int _typeId;
    final InputRole _inputRole;
    ObjectType(int typeId, InputRole role)
    {
        _typeId = typeId;
        _inputRole = role;
    }
    public int getTypeId()
    {
        return _typeId;
    }

    public InputRole getInputRole()
    {
        return _inputRole;
    }

    static public ObjectType fromTypeId(int value)
    {
        for (ObjectType type : values())
        {
            if (type.getTypeId() == value)
                return type;
        }
        return null;
    }
}
