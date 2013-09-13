/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

package org.labkey.luminex;

import org.labkey.api.study.actions.ProtocolIdForm;

/**
* User: cnathe
* Date: Sept 19, 2011
*/
public class LeveyJenningsForm extends ProtocolIdForm
{
    public enum ControlType { Titration, SinglePoint }

    private String _controlName;
    private ControlType _controlType = ControlType.Titration;

    /**
     * These Titration getters and setters are needed for URL backward compatibility.
     */
    public String getTitration()
    {
        return _controlName;
    }

    public void setTitration(String controlName)
    {
        _controlName = controlName;
        _controlType = ControlType.Titration;
    }

    public String getControlName()
    {
        return _controlName;
    }

    public void setControlName(String controlName)
    {
        _controlName = controlName;
    }

    public ControlType getControlType()
    {
        return _controlType;
    }

    public void setControlType(ControlType controlType)
    {
        _controlType = controlType;
    }
}
