/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;
import org.apache.commons.lang.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * User: jeckels
 * Date: Mar 3, 2008
 */
public class FilterView extends QueryView
{
    public static final String PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON = "customViewRadioButton";
    public static final String PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON = "customViewProteinGroupRadioButton";

    private final String _radioButtonID;

    public FilterView(ViewContext context, boolean peptides)
    {
        super(new MS2Schema(context.getUser(), context.getContainer()));
        setSettings(createSettings(context, peptides));
        _radioButtonID = peptides ? PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON : PROTEIN_GROUPS_CUSTOM_VIEW_RADIO_BUTTON;
    }

    private QuerySettings createSettings(ViewContext context, boolean peptides)
    {
        return getSchema().getSettings(
                context,
                peptides ? MS2Controller.PEPTIDES_FILTER : MS2Controller.PROTEIN_GROUPS_FILTER,
                peptides ? MS2Schema.HiddenTableType.PeptidesFilter.toString() : MS2Schema.HiddenTableType.ProteinGroupsFilter.toString());
    }

    public String renderViewList(HttpServletRequest request, Writer out, String viewName) throws IOException
    {
        Map<String, CustomView> customViews = new TreeMap<String, CustomView>(String.CASE_INSENSITIVE_ORDER);
        Map<String, CustomView> savedViews = getQueryDef().getCustomViews(getUser(), request);
        savedViews.remove(null);
        customViews.putAll(savedViews);
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
            return null;
        String id = h(param(QueryParam.viewName));
        out.write("<select id=\"" + id + "\" name=\"" + id + "\"");
        out.write("onchange=\"document.getElementById('" + _radioButtonID + "').checked=true;\"");
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
        return id;
    }
}
