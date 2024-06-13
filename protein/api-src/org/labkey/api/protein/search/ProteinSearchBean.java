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

package org.labkey.api.protein.search;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;

import java.util.function.Function;

public class ProteinSearchBean implements PeptideFilter
{
    private static Function<ProteinSearchBean, JspView<ProteinSearchBean>> PEPTIDE_PANEL_VIEW_FACTORY = null;

    private final boolean _horizontal;
    private final ProbabilityProteinSearchForm _form;

    public ProteinSearchBean(boolean horizontal, ProbabilityProteinSearchForm form)
    {
        _horizontal = horizontal;
        _form = form;
    }

    @Override
    public String getPeptideCustomViewName(ViewContext context)
    {
        return _form.getCustomViewName(context);
    }
    
    public boolean isHorizontal()
    {
        return _horizontal;
    }

    public ProbabilityProteinSearchForm getForm()
    {
        return _form;
    }

    public @Nullable JspView<ProteinSearchBean> getPeptidePanelView()
    {
        return PEPTIDE_PANEL_VIEW_FACTORY != null ? PEPTIDE_PANEL_VIEW_FACTORY.apply(this) : null;
    }

    public static void registerPeptidePanelViewFactory(Function<ProteinSearchBean, JspView<ProteinSearchBean>> factory)
    {
        PEPTIDE_PANEL_VIEW_FACTORY = factory;
    }
}
