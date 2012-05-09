<%
/*
 * Copyright (c) 2012 LabKey Corporation
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
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.nab.NabController"%>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.data.DataRegion" %>
<%@ page import="org.labkey.api.exp.api.ExpProtocol" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
JspView<java.util.List<org.labkey.api.exp.api.ExpProtocol>> me = (JspView<java.util.List<org.labkey.api.exp.api.ExpProtocol>>) HttpView.currentView();
java.util.List<org.labkey.api.exp.api.ExpProtocol> protocols = me.getModelBean();
%>
<p>
    This will migrated NAb runs created by the legacy implementation into the newer assay-based representation.
</p>
<p>
<% if (protocols.isEmpty()) { %>
    No NAb assay designs found.
<%
}
else
{ %>
    <form method="POST">
        Target assay design:
        <select name="protocolId">
            <% for (ExpProtocol protocol : protocols) { %>
                <option value="<%= protocol.getRowId() %>"><%= h(protocol.getName()) %></option>
            <% } %>
        </select>
        <labkey:button text="Submit" />
    </form>
<% }%>

</p>