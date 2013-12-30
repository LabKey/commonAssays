<%
/*
 * Copyright (c) 2004-2012 Fred Hutchinson Cancer Research Center
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
<%@ page import="org.labkey.api.data.Container" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Run" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<MS2Controller.RunSummaryBean> me = ((JspView<MS2Controller.RunSummaryBean>)HttpView.currentView());
    Container c = getContainer();
    MS2Controller.RunSummaryBean bean = me.getModelBean();
    MS2Run run = bean.run;
%>
<table>
    <tr>
    <td class="labkey-form-label">Search Enzyme</td><td><%=MS2Controller.defaultIfNull(run.getSearchEnzyme(), "n/a")%></td>
    <td class="labkey-form-label">File Name</td><td><%=MS2Controller.defaultIfNull(run.getFileName(), "n/a")%></td>
    </tr><tr>
    <td class="labkey-form-label">Search Engine</td><td><%=MS2Controller.defaultIfNull(run.getSearchEngine(), "n/a")%></td>
    <td class="labkey-form-label">Path</td><td><%=MS2Controller.defaultIfNull(run.getPath(), "n/a")%></td>
    </tr><tr>
    <td class="labkey-form-label">Mass Spec Type</td><td><%=MS2Controller.defaultIfNull(run.getMassSpecType(), "n/a")%></td>
    <td class="labkey-form-label">Fasta File</td><td><%=MS2Controller.defaultIfNull(run.getFastaFileName(), "n/a")%></td>
    </tr><%

if (null != bean.quantAlgorithm)
{ %>
    <tr><td class="labkey-form-label">Quantitation</td><td><%=h(bean.quantAlgorithm)%></td></tr><%
} %>
    <tr><td colspan="4">
        <div>
        <%

        if (bean.writePermissions)
        { %>
            <%=textLink("Rename", MS2Controller.getRenameRunURL(c, run, getActionURL()))%><%
        } %>
            <%=bean.modHref%><%

        if (null != run.getParamsFileName() && null != run.getPath())
        { %>
            <%=PageFlowUtil.textLink("Show " + run.getParamsFileName(), "showParamsFile.view?run=" + run.getRun(), null, "paramFileLink", java.util.Collections.singletonMap("target", "paramFile"))%><%
        }

        if (run.getHasPeptideProphet())
        { %>
            <%=PageFlowUtil.textLink("Show Peptide Prophet Details", "showPeptideProphetDetails.view?run=" + run.getRun(), null, "peptideProphetDetailsLink", java.util.Collections.singletonMap("target", "peptideProphetSummary"))%><%
        }

        if (run.hasProteinProphet())
        { %>
            <%=PageFlowUtil.textLink("Show Protein Prophet Details", "showProteinProphetDetails.view?run=" + run.getRun(), null, "proteinProphetDetailsLink", java.util.Collections.singletonMap("target", "proteinProphetSummary"))%><%
        }

        if (run.getNegativeHitCount() > run.getPeptideCount() / 3)
        { %>
            <%=textLink("Discriminate", "discriminateScore.view?run=" + run.getRun())%><%
        } %>
        </div>
    </td></tr>
</table>