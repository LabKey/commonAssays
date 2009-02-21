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
<%@ page import="org.labkey.flow.analysis.model.ScriptSettings" %>
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
    addCompare(opOptions, CompareType.GT);
    addCompare(opOptions, CompareType.LT);
    addCompare(opOptions, CompareType.GTE);
    addCompare(opOptions, CompareType.LTE);
    addCompare(opOptions, CompareType.CONTAINS);
    addCompare(opOptions, CompareType.DOES_NOT_CONTAIN);
    addCompare(opOptions, CompareType.DOES_NOT_START_WITH);
    addCompare(opOptions, CompareType.STARTS_WITH);
    addCompare(opOptions, CompareType.IN);
%>
<labkey:errors />
<br>
<form action="<%=form.getProtocol().urlFor(ProtocolController.Action.editICSMetadata)%>" method="POST">
    <table class="labkey-wp">
        <tr class="labkey-wp-header"><th align="left">Background and Foreground Match Columns:</th></tr>
        <tr><td>
            Select the columns that match between both the foreground and background wells.<br><br>
            For example, you usually want to match wells from the same FCSAnalysis Run and from
            the sample sample draw.
        </td></tr>
    </table>
    <br>
    <table>
        <% for (int i = 0; i < EditICSMetadataForm.MATCH_COLUMNS_MAX; i++) { %>
        <tr>
            <td><%=i == 0 ? "&nbsp;" : "and"%></td>
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
            Specify the column and value filter(s) which uniquely identify the background wells from
            the foreground wells.<br>If multiple wells match the background criteria, the
            background value will be the average of all the matched wells.<br><br>
            For example, if your background wells may have either "neg" or "NegControl" for
            the "Peptide" keyword then select column "Keyword Peptide", select operator "Equals One Of",
            and set the value to "neg;NegControl".
            Multiple values are combined with ';' for the "Equals One Of" operator.
        </td></tr>
    </table>
    <br>
    <table>
        <tr>
            <td>&nbsp;</td>
            <td align="center" class="labkey-form-label"><em>Background Column</em></td>
            <td align="center" class="labkey-form-label"><em>Operator</em></td>
            <td align="center" class="labkey-form-label"><em>Value</em></td>
        </tr>
        <%
            for (int i = 0; i < EditICSMetadataForm.BACKGROUND_COLUMNS_MAX; i++) {
                ScriptSettings.FilterInfo filter = form.backgroundFilter[i];
                %>
                <tr>
                    <td><%=i == 0 ? "&nbsp;" : "and"%></td>
                    <td><select name="backgroundField"><labkey:options value="<%=filter == null ? null : filter.getField()%>" map="<%=fieldOptions%>" /></select></td>
                    <td><select name="backgroundOp"><labkey:options value="<%=filter == null ? null : filter.getOp()%>" map="<%=opOptions%>" /></select></td>
                    <td><input name="backgroundValue" type="text" value="<%=h(filter == null ? null : filter.getValue())%>"></td>
                </tr>
                <%
            }
        %>
    </table>

    <br>
    <labkey:button text="Set ICS Metadata" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</form>
