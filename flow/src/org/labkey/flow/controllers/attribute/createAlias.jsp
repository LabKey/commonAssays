<%
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
%>
<%@ page import="org.labkey.flow.controllers.attribute.AttributeController" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    AttributeController.CreateAliasForm form = (AttributeController.CreateAliasForm)getModelBean();
    AttributeCache.Entry entry = form.getEntry();

    ActionURL editURL = getActionURL();
    ActionURL returnURL = form.getReturnActionURL(new ActionURL(ProtocolController.BeginAction.class, getContainer()));
%>

Create alias for <%=h(entry.getType().name())%> <%=h(entry.getName())%>
<p>

<labkey:errors/>
<labkey:form action="<%=editURL%>" method="post">
    <table>
        <tr>
            <td>
                <label for="alias">Alias:</label>
            </td>
            <td>
                <input type="text" id="alias" name="alias" value="<%=h(form.getAlias())%>" size="60" maxlength="255"/>
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>
                <%= button("Submit").submit(true) %>
                <%= button("Cancel").href(returnURL) %>
            </td>
        </tr>
    </table>
</labkey:form>
