<%
/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.flow.controllers.protocol.EditFCSAnalysisFilterForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%! void addCompare(Map<String, String> options, CompareType ct)
{
    options.put(ct.getPreferredUrlKey(), ct.getDisplayValue());
}%>
<% EditFCSAnalysisFilterForm form = (EditFCSAnalysisFilterForm) __form;
    Map<FieldKey, String> fieldOptions = new LinkedHashMap();
    fieldOptions.put(null, "");
    fieldOptions.putAll(form.getKeywordFieldMap());
    Map<String, String> opOptions = new LinkedHashMap();
    addCompare(opOptions, CompareType.EQUAL);
    addCompare(opOptions, CompareType.NEQ_OR_NULL);
    addCompare(opOptions, CompareType.ISBLANK);
    addCompare(opOptions, CompareType.NONBLANK);
    addCompare(opOptions, CompareType.STARTS_WITH);
    addCompare(opOptions, CompareType.CONTAINS);
    int clauseCount = Math.max(form.ff_field.length, 3);
%>
<labkey:errors />
<p>
    Filters may be applied to all analyses in the project folder.  The set of keyword and
    value pairs <i>must</i> all match in the FCS header to be included in the analysis.
    Alternatively, you may create filters on individual analysis scripts.
</p>
<labkey:form action="<%=form.getProtocol().urlFor(ProtocolController.EditFCSAnalysisFilterAction.class)%>" method="POST">
    <table class="lk-fields-table">
        <tr>
            <td>&nbsp;</td>
            <td style="font-weight: bold;">Keyword</td>
            <td style="font-weight: bold;">Operator</td>
            <td style="font-weight: bold;">Value</td>
        </tr>
        <% for (int i = 0; i < clauseCount; i ++) {
        FieldKey field = null;
        String op = null;
        String value = null;

        if (i < form.ff_field.length)
        {
            field = form.ff_field[i];
            op = form.ff_op[i];
            value = form.ff_value[i];
        }
        %>
        <tr>
            <td><%=i == 0 ? "&nbsp;" : "and"%></td>
            <td><select name="ff_field"><labkey:options value="<%=field%>" map="<%=fieldOptions%>" /> </select></td>
            <td><select name="ff_op"><labkey:options value="<%=op%>" map="<%=opOptions%>" /></select></td>
            <td><input name="ff_value" type="text" value="<%=h(value)%>" /></td>
        </tr>
        <% } %>

    </table>
    <p>
    <labkey:button text="Set filter" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</labkey:form>
