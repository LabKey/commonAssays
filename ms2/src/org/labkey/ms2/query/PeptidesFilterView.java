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
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.apache.commons.lang.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * User: jeckels
 * Date: Mar 3, 2008
 */
public class PeptidesFilterView extends QueryView
{
    private static final String PEPTIDE_FILTER_LINK_ID = "peptideFilterLink";
    private PeptideFilter _filter;

    public PeptidesFilterView(ViewContext context, PeptideFilter filter)
    {
        super(new MS2Schema(context.getUser(), context.getContainer()), new QuerySettings(context, MS2Controller.PEPTIDES_FILTER, MS2Schema.HiddenTableType.PeptidesFilter.toString()));
        _filter = filter;
    }

    public void renderCustomizeViewLink(Writer out) throws IOException
    {
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.addParameter(QueryParam.viewName, _filter.getCustomViewName(getViewContext()));
        url.addParameter(QueryParam.defaultTab, "filter");
        out.write(textLink("edit peptide filter", url, PEPTIDE_FILTER_LINK_ID));
    }

    public void renderViewList(HttpServletRequest request, Writer out) throws IOException
    {
        Map<String, CustomView> customViews = getQueryDef().getCustomViews(getUser(), request);
        Map<String, String> options = new LinkedHashMap<String, String>();
        options.put("", "<default>");
        for (CustomView view : customViews.values())
        {
            if (view.isHidden())
                continue;
            String label = view.getName();
            if (label == null)
                continue;
            options.put(view.getName(), label);
        }

        String _currentValue = _filter.getCustomViewName(getViewContext());
        int count = options.size();
        boolean currentMissing = false;
        if (_currentValue != null && !options.containsKey(_currentValue))
        {
            count ++;
            currentMissing = true;
        }
        if (count <= 1)
            return;
        out.write("Peptide filter:");
        out.write("<select name=\"" + h(param(QueryParam.viewName)) + "\"");
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.deleteParameter(QueryParam.viewName.toString());
        url.addParameter(QueryParam.defaultTab, "filter");
        url.addParameter(QueryParam.viewName, "");
        out.write(" onchange=\"document.getElementById('");
        out.write(PEPTIDE_FILTER_LINK_ID);
        out.write("').href='");
        out.write(url.toString());
        out.write("' + this.value; document.getElementById('customViewRadioButton').checked=true;\"");
        out.write(">");
        if (currentMissing)
        {
            out.write("<option selected value=\"" + h(_currentValue) + "\">");
            out.write("&lt;" + h(_currentValue) + ">");
            out.write("</option>");
        }
        for (Map.Entry<?, String> entry : options.entrySet())
        {
            out.write("\n<option");
            if (ObjectUtils.equals(entry.getKey(), _currentValue))
            {
                out.write(" selected");
            }
            out.write(" value=\"");
            out.write(h(entry.getKey()));
            out.write("\">");
            out.write(h(entry.getValue()));
            out.write("</option>");
        }
        out.write("</select>");
    }
}
