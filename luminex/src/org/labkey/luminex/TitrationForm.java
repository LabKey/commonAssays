/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.api.view.ViewForm;

/**
* User: cnathe
* Date: Sept 19, 2011
*/
public class TitrationForm extends ViewForm
{
    private String _titration;
    private String _protocol;

    public String getTitration()
    {
        return _titration;
    }

    public void setTitration(String titration)
    {
        _titration = titration;
    }

    public String getProtocol()
    {
        return _protocol;
    }

    public void setProtocol(String protocol)
    {
        _protocol = protocol;
    }
}
