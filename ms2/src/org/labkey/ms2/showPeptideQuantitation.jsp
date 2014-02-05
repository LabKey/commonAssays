<%
/*
 * Copyright (c) 2011-2014 LabKey Corporation
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
<%@ page import="org.labkey.api.util.URLHelper"%>
<%@ page import="org.labkey.api.view.ActionURL"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView"%>
<%@ page import="org.labkey.ms2.ElutionGraph"%>
<%@ page import="org.labkey.ms2.MS2Controller"%>
<%@ page import="org.labkey.ms2.MS2Peptide" %>
<%@ page import="org.labkey.ms2.PeptideQuantitation" %>
<%@ page import="org.labkey.ms2.ShowPeptideContext" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
JspView<ShowPeptideContext> me = (JspView<ShowPeptideContext>) HttpView.currentView();
ShowPeptideContext ctx = me.getModelBean();
MS2Peptide p = ctx.peptide;

PeptideQuantitation quant = p.getQuantitation();
ActionURL elutionGraphUrl = ctx.url.clone();
String errorMessage = ctx.url.getParameter("elutionProfileError");

elutionGraphUrl.setAction(MS2Controller.ShowLightElutionGraphAction.class);
elutionGraphUrl.deleteParameter("rowIndex");

elutionGraphUrl.addParameter("tolerance", String.valueOf(ctx.form.getTolerance()));
int currentCharge;
if (ctx.form.getQuantitationCharge() > 0)
{
    currentCharge = ctx.form.getQuantitationCharge();
}
else
{
    currentCharge = p.getCharge();
}
DecimalFormat format = new DecimalFormat();

if (errorMessage != null)
{ %>
    <span class="labkey-error"><%= errorMessage %></span>
<%  }
%>
<a name="quantitation"></a>
<table class="labkey-tab-strip">
    <tr>
        <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
        <% for (int i = 1; i <= 6; i++)
        {
            URLHelper chargeUrl = ctx.url.clone().replaceParameter("quantitationCharge", Integer.toString(i)); %>
            <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
            <td class="labkey-tab<%= i == currentCharge ? "-selected" : "" %>" style="margin-bottom: 0;"><a href="<%= h(chargeUrl ) %>#quantitation"><%= i %>+</a></td><%
        } %>
        <td class="labkey-tab-space" width="100%"></td>
        <td class="labkey-tab-space"><img width="5" src="<%=getWebappURL("_.gif")%>"></td>
    </tr>
</table>
<table style="height: <%= ElutionGraph.HEIGHT * 3 + 50 %>px;">
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Light</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; ">Scans:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getLightFirstScan()%> - <%= quant.getLightLastScan()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Mass:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getLightMass() %></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Area:</span></td>
                    <td><span style="font-size: smaller; "><%= format.format(quant.getLightArea()) %></span></td>
                </tr>
            </table>
        </td>
        <td><img height="<%= ElutionGraph.HEIGHT %>" width="<%= ElutionGraph.WIDTH %>" src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Light Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction(MS2Controller.ShowHeavyElutionGraphAction.class);
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Heavy</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; ">Scans:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavyFirstScan()%> - <%= quant.getHeavyLastScan()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Mass:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavyMass() %></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Area:</span></td>
                    <td><span style="font-size: smaller; "><%= format.format(quant.getHeavyArea()) %></span></td>
                </tr>
            </table>
        </td>
        <td><img height="<%= ElutionGraph.HEIGHT %>" width="<%= ElutionGraph.WIDTH %>" src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Heavy Elution Graph"/></td>
    </tr>
    <%
        elutionGraphUrl.setAction(MS2Controller.ShowCombinedElutionGraphAction.class);
    %>
    <tr>
        <td>
            <table align="center">
                <tr><td colspan="2" align="center"><strong>Combined</strong></td></tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Heavy to light ratio:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getHeavy2LightRatio()%></span></td>
                </tr>
                <tr>
                    <td><span style="font-size: smaller; "><%= p.getCharge() %>+ Light to heavy ratio:</span></td>
                    <td><span style="font-size: smaller; "><%= quant.getRatio()%></span></td>
                </tr>
            </table>
        </td>
        <td><img height="<%= ElutionGraph.HEIGHT %>" width="<%= ElutionGraph.WIDTH %>" src="<%=elutionGraphUrl.getEncodedLocalURIString()%>" alt="Combined Elution Graph"/></td>
    </tr>
</table>
