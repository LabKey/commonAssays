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
package org.labkey.ms2.search;

import org.labkey.api.view.JspView;
import org.labkey.ms2.MS2Controller;

/**
 * User: cnathe
 * Date: 3/29/13
 */
public class MSSearchWebpart extends JspView<ProteinSearchBean>
{
    public static final String NAME = "Mass Spec Search (Tabbed)";
    public static final String TITLE = "Mass Spec Search";

    public MSSearchWebpart()
    {
        super("/org/labkey/ms2/search/msTabbedSearch.jsp");
        setTitle(TITLE);
    }
}
