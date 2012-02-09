/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

package org.labkey.ms2.pipeline.client.mascot;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.ms2.pipeline.client.InputXmlComposite;
import org.labkey.ms2.pipeline.client.ParamParser;
import org.labkey.ms2.pipeline.client.ParameterNames;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>MascotInputXmlComposite</code>
 */
public class MascotInputXmlComposite extends InputXmlComposite
{

    public MascotInputXmlComposite()
    {
        ParameterNames.ENZYME = "mascot, enzyme";
        ParameterNames.STATIC_MOD = "mascot, fixed modifications";
        ParameterNames.DYNAMIC_MOD = "mascot, variable modifications";
    }

    public String update(String text)
    {
        if(params == null)
            params = new ParamParser(inputXmlTextArea);
        return super.update(text);
    }

    public Widget getLabel()
    {
        labelWidget = new Label("Mascot XML");
        labelWidget.setStylePrimaryName(LABEL_STYLE_NAME);
        return labelWidget;
    }
}
