/*
 * Copyright (c) 2010-2019 LabKey Corporation
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

import org.labkey.api.query.CustomView;
import org.labkey.api.query.QueryParam;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.util.element.Select.SelectBuilder;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.MS2Controller;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

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

    public String renderViewList(HttpServletRequest request, Writer out, String viewName)
    {
        Map<String, CustomView> customViews = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, CustomView> savedViews = getQueryDef().getCustomViews(getUser(), request, false, false);
        savedViews.remove(null);
        customViews.putAll(savedViews);
        Map<String, String> options = new LinkedHashMap<>();
        options.put("", "<default>");
        for (CustomView view : customViews.values())
        {
            String label = view.getName();
            if (label == null)
                continue;
            options.put(view.getName(), label);
        }

        if (options.size() <= 1)
            return null;

        String id = param(QueryParam.viewName);

        new SelectBuilder()
            .id(id)
            .name(id)
            .onChange("document.getElementById('" + _radioButtonID + "').checked=true;")
            .addOptions(options)
            .selected(viewName)
            .className(null)
            .appendTo(out);

        return id;
    }
}
