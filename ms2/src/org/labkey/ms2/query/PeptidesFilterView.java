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

package org.labkey.ms2.query;

import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.data.Container;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;

/**
 * User: jeckels
 * Date: Mar 3, 2008
 */
public class PeptidesFilterView extends QueryView
{
    private static final String PEPTIDE_FILTER_LINK_ID = "peptideFilterLink";
    private MS2Controller.PeptideFilteringComparisonForm _form;

    public PeptidesFilterView(ViewContext context, MS2Controller.PeptideFilteringComparisonForm form)
    {
        super(new MS2Schema(context.getUser(), context.getContainer()), new QuerySettings(context, MS2Controller.PEPTIDES_FILTER, MS2Schema.HiddenTableType.PeptidesFilter.toString()));
        _form = form;
    }

    public void renderCustomizeViewLink(PrintWriter out)
    {
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.addParameter(QueryParam.viewName, _form.getCustomViewName(getViewContext()));
        url.addParameter(QueryParam.defaultTab, "filter");
        out.write(textLink("edit peptide filter", url, PEPTIDE_FILTER_LINK_ID));
    }

    public QueryPicker getColumnListPicker(HttpServletRequest request)
    {
        QueryPicker basePicker = super.getColumnListPicker(request);
        return new QueryPicker("Peptide filter:", basePicker.getParamName(), _form.getCustomViewName(getViewContext()), basePicker.getChoices())
        {
            protected void appendOnChangeHandler(StringBuilder ret)
            {
                ActionURL url = urlFor(QueryAction.chooseColumns);
                url.deleteParameter(QueryParam.viewName.toString());
                url.addParameter(QueryParam.defaultTab, "filter");
                url.addParameter(QueryParam.viewName, "");
                ret.append(" onchange=\"document.getElementById('");
                ret.append(PEPTIDE_FILTER_LINK_ID);
                ret.append("').href='");
                ret.append(url.toString());
                ret.append("' + this.value; document.getElementById('customViewRadioButton').checked=true;\"");
            }
        };
    }

}
