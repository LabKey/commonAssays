/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.ms2.pipeline.client.sequest;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HasText;
import org.labkey.ms2.pipeline.client.InputXmlComposite;
import org.labkey.ms2.pipeline.client.ParamParser;

/**
 * User: billnelson@uky.edu
 * Date: Apr 18, 2008
 */

/**
 * <code>SequestInputXmlComposite</code>
 */
public class SequestInputXmlComposite extends InputXmlComposite
{
    public String update(String text)
    {
        if(params == null)
            params = new SequestParamParser(inputXmlTextArea);
        return super.update(text);
    }

    public Widget getLabel()
    {
        labelWidget = new Label("Sequest XML");
        labelWidget.setStylePrimaryName(LABEL_STYLE_NAME);
        return labelWidget;
    }

    private class SequestParamParser extends ParamParser
    {
        private SequestParamParser(HasText xml)
        {
            super(xml);
        }
    }
}
