/*
 * Copyright (c) 2007-2012 LabKey Corporation
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

import org.labkey.ms2.query.PeptideFilter;
import org.labkey.ms2.query.FilterView;
import org.labkey.ms2.MS2Controller;
import org.labkey.api.view.ViewContext;

/**
 * User: jeckels
 * Date: Feb 26, 2007
 */
public class ProteinSearchBean implements PeptideFilter
{
    private boolean _horizontal;
    private final MS2Controller.ProbabilityProteinSearchForm _form;
    private FilterView _peptideView;
    
    public ProteinSearchBean(boolean horizontal, MS2Controller.ProbabilityProteinSearchForm form)
    {
        _horizontal = horizontal;
        _form = form;
    }

    public FilterView getPeptideView(ViewContext viewContext)
    {
        if (_peptideView == null)
        {
            _peptideView = new FilterView(viewContext, true);
        }
        return _peptideView;
    }
    
    public String getPeptideCustomViewName(ViewContext context)
    {
        return _form.getCustomViewName(context);
    }
    
    public boolean isHorizontal()
    {
        return _horizontal;
    }

    public MS2Controller.ProbabilityProteinSearchForm getForm()
    {
        return _form;
    }
}
