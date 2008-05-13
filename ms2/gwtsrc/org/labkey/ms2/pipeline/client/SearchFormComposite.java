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

package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Mar 25, 2008
 */
public abstract class SearchFormComposite extends Composite implements HasName
{
    protected boolean readOnly;
    protected Widget labelWidget;


    public boolean isReadOnly()
     {
         return readOnly;
     }

     public void setReadOnly(boolean readOnly)
     {
         this.readOnly = readOnly;
     }

    abstract public void init();

    abstract public void setWidth(String width);

    abstract public Widget getLabel(String style);

    abstract public String validate();
}
