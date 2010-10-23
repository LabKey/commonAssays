/*
 * Copyright (c) 2008-2010 LabKey Corporation
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
public class FilterView extends QueryView
{
    private static final String PEPTIDE_FILTER_LINK_ID = "peptideFilterLink";
    private static final String PROTEIN_GROUP_FILTER_LINK_ID = "proteinGroupFilterLink";
    public static final String PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON = "customViewRadioButton";
    public static final String PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON = "customViewProteinGroupRadioButton";

    private final String _linkID;
    private final String _radioButtonID;

    public FilterView(ViewContext context, boolean peptides)
    {
        super(new MS2Schema(context.getUser(), context.getContainer()));
        setSettings(createSettings(context, peptides));
        _linkID = peptides ? PEPTIDE_FILTER_LINK_ID : PROTEIN_GROUP_FILTER_LINK_ID;
        _radioButtonID = peptides ? PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON : PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON;
    }

    private QuerySettings createSettings(ViewContext context, boolean peptides)
    {
        return getSchema().getSettings(
                context,
                peptides ? MS2Controller.PEPTIDES_FILTER : MS2Controller.PROTEIN_GROUPS_FILTER,
                peptides ? MS2Schema.HiddenTableType.PeptidesFilter.toString() : MS2Schema.HiddenTableType.ProteinGroupsFilter.toString());
    }

    public void renderCustomizeViewLink(Writer out, String viewName) throws IOException
    {
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.addParameter(QueryParam.viewName, viewName);
        url.addParameter(QueryParam.defaultTab, "filter");
        out.write(textLink("edit view", url, _linkID));
    }

    public void renderViewList(HttpServletRequest request, Writer out, String viewName) throws IOException
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

        int count = options.size();
        boolean currentMissing = false;
        if (viewName != null && !options.containsKey(viewName))
        {
            count ++;
            currentMissing = true;
        }
        if (count <= 1)
            return;
        out.write("Custom view:");
        out.write("<select name=\"" + h(param(QueryParam.viewName)) + "\"");
        ActionURL url = urlFor(QueryAction.chooseColumns);
        url.deleteParameter(QueryParam.viewName.toString());
        url.addParameter(QueryParam.defaultTab, "filter");
        url.addParameter(QueryParam.viewName, "");
        out.write(" onchange=\"document.getElementById('");
        out.write(_linkID);
        out.write("').href='");
        out.write(url.toString());
        out.write("' + this.value; document.getElementById('" + _radioButtonID + "').checked=true;\"");
        out.write(">");
        if (currentMissing)
        {
            out.write("<option selected value=\"" + h(viewName) + "\">");
            out.write("&lt;" + h(viewName) + ">");
            out.write("</option>");
        }
        for (Map.Entry<?, String> entry : options.entrySet())
        {
            out.write("\n<option");
            if (ObjectUtils.equals(entry.getKey(), viewName))
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
