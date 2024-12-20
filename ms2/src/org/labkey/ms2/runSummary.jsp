<%
/*
 * Copyright (c) 2004-2017 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.protein.ProteinManager" %>
<%@ page import="org.labkey.api.protein.fasta.FastaFile" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Run" %>
<%@ page import="org.labkey.ms2.pipeline.mascot.MascotRun" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<MS2Controller.RunSummaryBean> me = ((JspView<MS2Controller.RunSummaryBean>)HttpView.currentView());
    Container c = getContainer();
    MS2Controller.RunSummaryBean bean = me.getModelBean();
    MS2Run run = bean.run;

    List<String> fastas = new ArrayList<>();
    for (int id : run.getFastaIds())
    {
        FastaFile file = ProteinManager.getFastaFile(id);
        fastas.add(file.getFilename());
    }
%>
<table class="lk-fields-table">
    <tr>
    <td class="labkey-form-label">Search Enzyme</td><td><%=h(MS2Controller.defaultIfNull(run.getSearchEnzyme(), "n/a"))%></td>
    <td class="labkey-form-label">File Name</td><td><%=h(MS2Controller.defaultIfNull(run.getFileName(), "n/a"))%></td>
    </tr><tr>
    <td class="labkey-form-label">Search Engine</td><td><%=h(MS2Controller.defaultIfNull(run.getSearchEngine(), "n/a"))%></td>
    <td class="labkey-form-label">Path</td><td><%=h(MS2Controller.defaultIfNull(run.getPath(), "n/a"))%></td>
    </tr><tr>
    <td class="labkey-form-label">Mass Spec Type</td><td><%=h(MS2Controller.defaultIfNull(run.getMassSpecType(), "n/a"))%></td>
    <td class="labkey-form-label">Fasta File<%= h(fastas.size() > 1 ? "s" : "") %></td><td><%=h(StringUtils.join(fastas, ", "))%></td>
    </tr>
    <% if (run instanceof MascotRun) { %>
    <tr>
        <td class="labkey-form-label">Mascot File</td><td><%=h(MS2Controller.defaultIfNull(((MascotRun)run).getMascotFile(), "n/a"))%></td>
        <td class="labkey-form-label">Distiller Raw File</td><td><%=h(MS2Controller.defaultIfNull(((MascotRun)run).getDistillerRawFile(), "n/a"))%></td>
    </tr>
    <% } %>

    <%

if (null != bean.quantAlgorithm)
{ %>
    <tr><td class="labkey-form-label">Quantitation</td><td><%=h(bean.quantAlgorithm)%></td></tr><%
} %>
    <tr><td colspan="4">
        <div>
        <%

        if (bean.writePermissions)
        { %>
            <%=link("Rename", MS2Controller.getRenameRunURL(c, run, getActionURL()))%><%
        } %>
            <%=bean.modHref%><%

        if (null != run.getParamsFileName() && null != run.getPath())
        { %>
            <%=link("Show " + run.getParamsFileName()).href(urlFor(MS2Controller.ShowParamsFileAction.class).addParameter("run", run.getRun())).id("paramFileLink").target("paramFile")%><%
        }

        if (run.getHasPeptideProphet())
        { %>
            <%=link("Show Peptide Prophet Details").href(urlFor(MS2Controller.ShowPeptideProphetDetailsAction.class).addParameter("run", run.getRun())).id("peptideProphetDetailsLink").target("peptideProphetSummary")%><%
        }

        if (run.hasProteinProphet())
        { %>
            <%=link("Show Protein Prophet Details").href(urlFor(MS2Controller.ShowProteinProphetDetailsAction.class).addParameter("run", run.getRun())).id("proteinProphetDetailsLink").target("proteinProphetSummary")%><%
        } %>
        </div>
    </td></tr>
</table>
