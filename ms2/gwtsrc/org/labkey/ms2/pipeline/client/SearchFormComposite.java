/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.Set;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public abstract class SearchFormComposite extends Composite implements HasName
{
    public static final String LABEL_STYLE_NAME = "labkey-form-label";

    protected boolean readOnly;

    protected int _parentTableRow;
    protected FlexTable _parentTable;

    public boolean isReadOnly()
     {
         return readOnly;
     }

     public void setReadOnly(boolean readOnly)
     {
         this.readOnly = readOnly;
     }

    abstract public void setWidth(String width);

    abstract public Widget getLabel();

    abstract public String validate();

    abstract public void syncFormToXml(ParamParser params) throws SearchFormException;

    abstract public String syncXmlToForm(ParamParser params);

    public void configureCompositeRow(FlexTable table, int row)
    {
        _parentTable = table;
        _parentTableRow = row;
    }

    public boolean isHandledParameterName(String name)
    {
        return getHandledParameterNames().contains(name);
    }

    public Set<String> getHandledParameterNames()
    {
        return Collections.emptySet();
    }
}
