/*
 * Copyright (c) 2014-2017 LabKey Corporation
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
package org.labkey.luminex.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.luminex.LuminexRunUploadForm;
import org.labkey.luminex.LuminexUploadWizardAction;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.Titration;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

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
            selectedSQL.append("t.RowId = at.TitrationId AND t.RunId = ? AND LOWER(t.Name) = LOWER(?) AND t.Standard = ?");
            selectedSQL.add(_form.getReRun().getRowId());
            selectedSQL.add(titrationName);
            selectedSQL.add(Boolean.TRUE);

            defVal = new SqlSelector(LuminexProtocolSchema.getSchema(), selectedSQL).exists() ? "true" : "false";
        }
        String checked = "";

        if (_errorReshow)
        {
            // if reshowing form on error, preselect based on request value
            if (_form.getViewContext().getRequest().getParameter(propertyName) != null)
                checked = "CHECKED";
        }
        else if (_standardTitrations.contains(_titration))
        {
            if (_standardTitrations.size() == 1)
            {
                // if there is only one standard, then preselect the checkbox
                checked = "CHECKED";
            }
            else if (_standardTitrations.size() > 1 && (defVal == null || defVal.toLowerCase().equals("true")))
            {
                // if > 1 standard and default value exists, set checkbox based on default value
                // else if no default value and titration is standard, then preselect the checkbox
                checked = "CHECKED";
            }
        }

        out.write("<input type=\"checkbox\" value='" + 1 + "' name='" + propertyName + "' " + checked + " />");
    }

    @Override
    public void renderInputWrapperBegin(Writer out) throws IOException
    {
        String titrationCellName = LuminexUploadWizardAction.getTitrationColumnCellName(_titration.getName());

        out.write("<td");
        out.write(" style=\"display:" + (_hideCell ? "none" : "table-cell") + ";\"");
        out.write(" name=\"" + PageFlowUtil.filter(titrationCellName) + "\">");
    }

    public void renderInputWrapperEnd(Writer out) throws IOException
    {
        out.write("</td>");
    }

    @Override
    public void renderDetailsCaptionCell(RenderContext ctx, Writer out, @Nullable String cls) throws IOException
    {
        String titrationCellName = PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationColumnCellName(_titration.getName()));
        out.write("<td name=\"" + titrationCellName + "\"" + "class=\"" + PageFlowUtil.filter(cls)+ "\""
                + " style=\"display:" + (_hideCell ? "none" : "table-cell") + ";\">");
        renderTitle(ctx, out);
        out.write("</td>");
    }

    @Override
    public String getFormFieldName(RenderContext ctx)
    {
        return PageFlowUtil.filter(LuminexUploadWizardAction.getTitrationCheckboxName(_titration.getName(), _analyteName));
    }
}
