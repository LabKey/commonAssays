<%
    /*
     * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.flow.controllers.well.EditWellForm" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowDataType" %>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<% EditWellForm form = (EditWellForm) __form;
    FlowWell well = form.getWells().get(0);
    List<FlowWell> wells = form.getWells();%>
<labkey:errors/>
<labkey:form method="POST" action="<%=h(well.urlFor(WellController.EditWellAction.class))%>">
    <input name="editWellReturnUrl" type="hidden" value="<%=h(form.editWellReturnUrl)%>"/>
    <input name="ff_isBulkEdit" type="hidden" value="<%=h(form.ff_isBulkEdit)%>"/>
    <input name="isUpdate" type="hidden" value="true" />
    <table id="keywordTable" class="lk-fields-table">
        <tr>
            <td>Run Name:</td>
            <td>
                <a href="<%= new ActionURL(RunController.ShowRunAction.class, getContainer()).addParameter("runId",well.getRun().getRunId())%>"><%=h(well.getRun().getName())%>
                </a></td>
        </tr>
        <% if (form.ff_isBulkEdit)
        { %>
        <tr>
            <td colspan="2">
                <h4>Press Update to save the input values for multiple keywords for the selected FCS files.
                    <br>
                    <ul>
                        <li>Adding a new keyword or editing the name will apply only to FCS files that have been
                            selected.
                        </li>
                        <li>If a field is left blank, there are no changes.</li>
                    </ul>
                </h4>
            </td>
            <td>
        <tr>
            <td>Selected Files:</td>
            <td>
                <% String prefix = "";
                    for (FlowWell flowWell : wells)
                    {%>
                <%=h(prefix + flowWell.getName())%>
                <input type="hidden" name="ff_fileRowId" value="<%=h(flowWell.getRowId())%>">
                <% prefix = ", ";
                }%>
            </td>
        </tr>
        <%
        }
        else
        {
        %>
        <tr>
            <td>Well Name:</td>
            <td><input type="text" name="ff_name" value="<%=h(form.ff_name)%>">
                <input type="hidden" name="ff_fileRowId" value="<%=h(wells.get(0).getRowId())%>"></td>
        </tr>
        <tr>
            <td>Comment:</td>
            <td><textarea rows="5" cols="40" name="ff_comment"><%=h(form.ff_comment)%></textarea></td>
        </tr>
        <%}%>
        <% if (well.getDataType() == FlowDataType.FCSFile)
        { %>
        <tr>
            <td colspan="2">Keywords:</td>
        </tr>
        <%
            for (int i = 0; i < form.ff_keywordName.length; i++)
            { %>
        <tr>
            <td>
                <%
                    if (form.ff_keywordError[i] == null)
                    {
                %>
                <input type=hidden name="ff_keywordName" value="<%=h(form.ff_keywordName[i])%>">
                <%=h(form.ff_keywordName[i])%>
                <input type=hidden name="ff_keywordError" value="">
                <%
                }
                else
                {
                %>
                <input type="text" name="ff_keywordName" value="<%=h(form.ff_keywordName[i])%>">
                <input type=hidden name="ff_keywordError" value="<%=h(form.ff_keywordError[i])%>">
                <%}%>

            </td>
            <td><input type="text" name="ff_keywordValue" value="<%=h(form.ff_keywordValue[i])%>">
                <%
                    if (form.ff_keywordError[i] != null)
                    {
                %>
                <%=h(form.ff_keywordError[i])%>
                <%}%>
            </td>
        </tr>
        <%
            }
        %>
        <tr><td colspan="2"><i class="fa fa-plus-circle add-new-keyword" style="color: #116596"> Create a new keyword:</i></td></tr>
        <% } %>
    </table>

    <labkey:button text="update"/>
    <%
        String cancelURL = String.valueOf(well.urlFor(WellController.ShowWellAction.class));
        if (form.ff_isBulkEdit)
        {
            cancelURL = new ActionURL(form.editWellReturnUrl).toString();
        }
    %>
    <labkey:button text="cancel" href="<%=text(cancelURL)%>"/>
</labkey:form>
<script type="text/javascript">
    (function ($) {
        $('.add-new-keyword').click(function () {
            var newRow = document.getElementById('keywordTable').insertRow(-1);
            var labelCell = newRow.insertCell(-1);
            labelCell.innerHTML = '<input type="text" name="ff_keywordName" value="">';
            var valueCell = newRow.insertCell(-1);
            valueCell.innerHTML = '<input type="text" name="ff_keywordValue"><input type=hidden name="ff_keywordError" value="">';

        });
    })(jQuery);
</script>






