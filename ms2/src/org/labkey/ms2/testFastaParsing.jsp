<%
/*
 * Copyright (c) 2005-2014 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.protein.ProteinPlus" %>
<%@ page import="org.labkey.ms2.protein.fasta.Protein" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.FastaParsingForm form = ((JspView<MS2Controller.FastaParsingForm>) HttpView.currentView()).getModelBean();
%>
<labkey:form method="POST">
    <table width="100%">
        <tr>
            <td nowrap="true">FASTA header line:</td>
            <td width="100%"><input type="text" name="header" value="<%= h(form.getHeader())%>" size="40" style="width: 100%;"></td>
        </tr>
        <tr>
            <td />
            <td><%= button("Submit").submit(true) %></td>
        </tr>

        <% if (form.getHeader() != null) {
            ProteinPlus p = new ProteinPlus(new Protein(form.getHeader(), new byte[0]));
            Map<String, Set<String>> idents = p.getProtein().getIdentifierMap(); %>
            <tr>
                <td><strong>Protein name:</strong></td>
                <td><%= h(p.getProtein().getLookup())%></td>
            </tr>
            <tr>
                <td><strong>Description:</strong></td>
                <td><%= h(p.getDescription())%></td>
            </tr>
            <% for (Map.Entry<String,Set<String>> entry : idents.entrySet()) { %>
                <tr>
                    <td><strong><%= h(entry.getKey()) %>:</strong></td>
                    <td>
                       <%  String separator = "";
                        for (String value : entry.getValue()) { %><%= separator%> <%= h(value) %><% separator = ", ";
                        } %></td>
                </tr>
            <% } %>
        <% } %>
    </table>
</labkey:form>
