<%
/*
 * Copyright (c) 2009 LabKey Corporation
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
%>
<%@ page import="org.labkey.microarray.MicroarrayBulkPropertiesDisplayColumn" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.microarray.MicroarrayBulkPropertiesTemplateAction" %>
<%@ page import="org.labkey.microarray.MicroarrayRunUploadForm" %>

<%
    ViewContext context = HttpView.currentContext();
    MicroarrayRunUploadForm form = ((JspView<MicroarrayRunUploadForm>)HttpView.currentView()).getModelBean();
    ActionURL templateURL = new ActionURL(MicroarrayBulkPropertiesTemplateAction.class, context.getContainer());
    templateURL.addParameter("rowId", form.getProtocol().getRowId());
    String existingValue = form.getRawBulkProperties();
    boolean useBulk = existingValue != null && !existingValue.trim().equals("");
%>
<script type="text/javascript">
    function toggleBulkProperties(enabled)
    {
        if (enabled)
        {
            document.getElementById('<%= MicroarrayBulkPropertiesDisplayColumn.PROPERTIES_FIELD_NAME%>').style.display='block';
        }
        else
        {
            document.getElementById('<%= MicroarrayBulkPropertiesDisplayColumn.PROPERTIES_FIELD_NAME%>').style.display='none';
        }
    }
</script>

<input type="checkbox" id="<%= MicroarrayBulkPropertiesDisplayColumn.ENABLED_FIELD_NAME %>" <%= useBulk ? "checked" : "" %>
       onclick="toggleBulkProperties(document.getElementById('<%= MicroarrayBulkPropertiesDisplayColumn.ENABLED_FIELD_NAME %>').checked)" name="<%= MicroarrayBulkPropertiesDisplayColumn.ENABLED_FIELD_NAME %>"> Specify run properties for all runs at once with tab-separated value<%= PageFlowUtil.helpPopup("Bulk Properties", "<p>You may use a set of TSV (tab-separated values) to specify run metadata.<p>The barcode column in the TSV is matched with the barcode value in the MageML file. The sample name columns, configured in the assay design, will be used to look for matching samples by name in all visible sample sets.</p><p>Any additional run level properties may be specified as separate columns.</p>", true)%>
[<a href="<%= templateURL %>">download Excel template</a>]
<br/>
<textarea <%= !useBulk ? "style=\"display: none;\"" : "" %>
        style="width: 100%"
        id="<%= MicroarrayBulkPropertiesDisplayColumn.PROPERTIES_FIELD_NAME %>" rows="5" cols="80"
        name="<%= MicroarrayBulkPropertiesDisplayColumn.PROPERTIES_FIELD_NAME %>"><%= PageFlowUtil.filter(existingValue) %></textarea>
