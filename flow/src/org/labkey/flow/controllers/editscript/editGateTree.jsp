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
<%@ page import="org.apache.commons.lang3.StringUtils"%>
<%@ page import="org.labkey.flow.analysis.web.SubsetSpec"%>
<%@ page import="org.labkey.flow.controllers.editscript.EditGateTreeForm"%>
<%@ page import="org.labkey.flow.controllers.editscript.ScriptController" %>
<%@ page extends="org.labkey.flow.controllers.editscript.ScriptController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%! String indent(SubsetSpec subset)
    {
        return StringUtils.repeat("+", subset.getSubsets().length);
    }
%>
<% EditGateTreeForm form = (EditGateTreeForm) getForm(); %>
<labkey:errors/>
<p>
    Use this page to rename populations.  To delete a population, delete its name.<br>
</p>
<labkey:form action="<%=formAction(ScriptController.EditGateTreeAction.class)%>" method="POST">
<table class="lk-fields-table">
    <%
        for (int i = 0; i < form.populationNames.length; i ++)
        {
    %>
    <tr><td>
        <%=indent(form.subsets[i])%> <input type="text" name="populationNames[<%=i%>]" value="<%=h(form.populationNames[i])%>">
    </td></tr>
    <% } %>
</table>
<br/>
<input class="labkey-button" type="submit" value="Update">
</labkey:form>
