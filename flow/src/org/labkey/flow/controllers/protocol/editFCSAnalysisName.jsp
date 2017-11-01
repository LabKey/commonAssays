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
<%@ page import="org.labkey.flow.controllers.protocol.EditFCSAnalysisNameForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<% EditFCSAnalysisNameForm form = (EditFCSAnalysisNameForm) __form; %>
<p>
    Use this page to describe how FCSAnalyses should be named.<br>
    The name of an FCSAnalysis can be composed from keywords taken from the FCS file.<br>
    Changes to this setting will apply to existing FCSAnalyses as well as ones created in the future.<br>
</p>
<% if (form.ff_keyword != null)
{
    int selectCount = Math.max(4, form.ff_keyword.length + 2);
    Map<FieldKey, String> options = form.getKeywordFieldMap();
    Map<FieldKey, String> optionsWithNull = new LinkedHashMap();
    optionsWithNull.put(null, "");
    optionsWithNull.putAll(options);
%>
<labkey:form method="POST" action="<%=form.getProtocol().urlFor(ProtocolController.EditFCSAnalysisNameAction.class)%>">
    <p>
        Which keywords should be used to compose the FCSAnalysis name?<br>
        <% FieldKey[] keywords = form.ff_keyword;
        if (keywords == null)
        {
            %>
        <b>Note: the current value of the FCS analysis name expression is too complex.  You probably should use the advanced
        text box below.</b><br>
        <%  keywords = new FieldKey[0];
            } %>

        <% for(int i = 0; i < selectCount; i ++) {
            FieldKey value = null;
            if (i < keywords.length)
            {
                value = keywords[i];
            }
        %>
            <select name="ff_keyword">
                <labkey:options value="<%=value%>" map="<%= i == 0 ? options : optionsWithNull %>" />
            </select><br>
        <% } %>

    </p>
    <labkey:button text="Set names" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</labkey:form>
<% } %>
<br/>
<labkey:form method="POST" action="<%=form.getProtocol().urlFor(ProtocolController.EditFCSAnalysisNameAction.class)%>">
    <p>Advanced users only:<br>You can also edit the expression that is used to build up the FCS analysis name.
        Use '\${' and '}' to denote substitutions.  Keyword names should be prefixed with 'Keyword'.
        <br>
        <input type="text" width="80" name="ff_rawString" value="<%=h(form.ff_rawString)%>"/>
    </p>
    <labkey:button text="Set Expression" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>" />
</labkey:form>