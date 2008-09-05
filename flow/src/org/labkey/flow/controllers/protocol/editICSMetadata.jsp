<%
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
%>
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.flow.controllers.protocol.EditICSMetadataForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%! void addCompare(Map<String, String> options, CompareType ct)
{
    options.put(ct.getUrlKey(), ct.getDisplayValue());
}%>
<%
    EditICSMetadataForm form = (EditICSMetadataForm)__form;

    Map<FieldKey, String> fieldOptions = new LinkedHashMap();
    fieldOptions.put(null, "");
    fieldOptions.putAll(form.getKeywordAndSampleFieldMap());

    Map<String, String> opOptions = new LinkedHashMap();
    addCompare(opOptions, CompareType.EQUAL);
    addCompare(opOptions, CompareType.NEQ_OR_NULL);
    addCompare(opOptions, CompareType.ISBLANK);
    addCompare(opOptions, CompareType.NONBLANK);
    addCompare(opOptions, CompareType.STARTS_WITH);
    addCompare(opOptions, CompareType.CONTAINS);
%>
<labkey:errors />
<form action="<%=form.getProtocol().urlFor(ProtocolController.Action.editICSMetadata)%>" method="POST">
    <table class="labkey-wp">
        <tr class="labkey-wp-header"><th align="left">Background and Foreground Match Columns:</th></tr>
        <tr><td>Select the columns that match between both the foreground and background wells.</td></tr>
    </table>
    <table>
        <% for (int i = 0; i < EditICSMetadataForm.MATCH_COLUMNS_MAX; i++) { %>
        <tr>
            <td>
                <select name="matchColumn"><labkey:options value="<%=form.matchColumn[i]%>" map="<%=fieldOptions%>" /></select>
            </td>
        </tr>
        <% } %>
    </table>

    <br><br>
    <table class="labkey-wp">
        <tr class="labkey-wp-header"><th align="left">Background Column and Value:</th></tr>
        <tr><td>
            Specify the column and value which uniquely identify the background wells from
            the foreground wells.<br>If multiple wells match the background criteria, the
            background value will be the average of all the matched wells.
        </td></tr>
    </table>
    <table>
        <tr>
            <td><select name="backgroundField"><labkey:options value="<%=form.backgroundField%>" map="<%=fieldOptions%>" /></select></td>
            <td><select name="backgroundOp"><labkey:options value="<%=form.backgroundOp%>" map="<%=opOptions%>" /></select></td>
            <td><input name="backgroundValue" type="text" value="<%=h(form.backgroundValue)%>"></td>
        </tr>
    </table>

    <br>
    <labkey:button text="Set ICS Metadata" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</form>
