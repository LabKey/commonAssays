/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.util.PageFlowUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * Created by cnathe on 7/21/14.
 */
public class AnalytePropStandardsDisplayColumn extends SimpleDisplayColumn
{
    private LuminexRunUploadForm _form;
    private Titration _titration;
    private String _analyteName;
    private String _protocolName;
    private boolean _errorReshow;
    private boolean _hideCell;
    private Set<Titration> _standardTitrations;

    public AnalytePropStandardsDisplayColumn(LuminexRunUploadForm form, Titration titration, String analyteName, String protocolName,
                                             boolean errorReshow, boolean hideCell, Set<Titration> standardTitrations)
    {
        _form = form;
        _titration = titration;
        _analyteName = analyteName;
        _protocolName = protocolName;
        _errorReshow = errorReshow;
        _hideCell = hideCell;
        _standardTitrations = standardTitrations;
    }

    @Override
    public void renderInputHtml(RenderContext ctx, Writer out, Object value) throws IOException
    {
        String titrationName = _titration.getName();
        String propertyName = PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationCheckboxName(titrationName, _analyteName));
        Map<String, String> defaultTitrationValues = PropertyManager.getProperties(_form.getUser(),
                _form.getContainer(), _protocolName + ": " + titrationName);

        Map<String, Titration> existingTitrations = LuminexUploadWizardAction.getExistingTitrations(_form.getReRun());
        Map<String, Analyte> existingAnalytes = LuminexUploadWizardAction.getExistingAnalytes(_form.getReRun());

        String defVal = defaultTitrationValues.get(propertyName);
        // If we're replacing this run, and the previous version of the run had the analyte/titration
        // combination, see if it they were used together
        if (_form.getReRun() != null && existingTitrations.containsKey(titrationName) && existingAnalytes.containsKey(_analyteName))
        {
            SQLFragment selectedSQL = new SQLFragment("SELECT at.* FROM ");
            selectedSQL.append(LuminexProtocolSchema.getTableInfoAnalyteTitration(), "at");
            selectedSQL.append(", ");
            selectedSQL.append(LuminexProtocolSchema.getTableInfoTitration(), "t");
            selectedSQL.append(", ");
            selectedSQL.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
            selectedSQL.append(" WHERE LOWER(a.Name) = LOWER(?) AND a.RowId = at.AnalyteId AND ");
            selectedSQL.add(_analyteName);
            selectedSQL.append("t.RowId = at.TitrationId AND t.RunId = ? AND LOWER(t.Name) = LOWER(?)");
            selectedSQL.add(_form.getReRun().getRowId());
            selectedSQL.add(titrationName);

            if (new SqlSelector(LuminexProtocolSchema.getSchema(), selectedSQL).exists())
            {
                defVal = Boolean.TRUE.toString();
            }

        }
        String checked = "";

        // if reshowing form on error, preselect based on request value
        if (_errorReshow)
        {
            if (_form.getViewContext().getRequest().getParameter(propertyName) != null)
                checked = "CHECKED";
        }
        // if there is only one standard, then preselect the checkbox
        else if (_standardTitrations.size() == 1 && _standardTitrations.contains(_titration))
        {
            checked = "CHECKED";
        }
        else if (_standardTitrations.size() > 1)
        {
            // if > 1 standard and default value exists, set checkbox based on default value
            if (defVal != null && defVal.toLowerCase().equals("true"))
                checked = "CHECKED";
                // if no default value and titration is standard, then preselect the checkbox
            else if (defVal == null && _standardTitrations.contains(_titration))
                checked = "CHECKED";
        }

        out.write("<input type='checkbox' value='" + value + "' name='" + propertyName + "' " + checked + " />");
    }

    @Override
    public void renderInputCell(RenderContext ctx, Writer out, int span) throws IOException
    {
        String titrationCellName = PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationColumnCellName(_titration.getName()));
        out.write("<td colspan=" + span + " name='" + titrationCellName
                + "' style='display:" + (_hideCell ? "none" : "table-cell") + "' >");
        renderInputHtml(ctx, out, 1);
        out.write("</td>");
    }

    @Override
    public void renderDetailsCaptionCell(RenderContext ctx, Writer out) throws IOException
    {
        String titrationCellName = PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationColumnCellName(_titration.getName()));
        out.write("<td name='" + titrationCellName + "' "
                + " class='labkey-form-label' style='display:" + (_hideCell ? "none" : "table-cell") + "' >");
        renderTitle(ctx, out);
        out.write("</td>");
    }

    @Override
    public String getFormFieldName(RenderContext ctx)
    {
        return PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationCheckboxName(_titration.getName(), _analyteName));
    }
}
