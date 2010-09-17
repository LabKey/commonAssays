<%
/*
 * Copyright (c) 2010 LabKey Corporation
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
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.nab.NabAssayRun" %>
<%@ page import="org.labkey.nab.NabAssayController" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="org.labkey.nab.SampleInfo" %>
<%@ page import="org.labkey.nab.Luc5Assay" %>
<%@ page import="org.labkey.nab.DilutionSummary" %>
<%@ page import="org.labkey.api.study.DilutionCurve" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    JspView<NabAssayController.RenderAssayBean> me = (JspView<NabAssayController.RenderAssayBean>) HttpView.currentView();
    NabAssayController.RenderAssayBean bean = me.getModelBean();
    NabAssayRun assay = bean.getAssay();
%>
<table class="labkey-data-region labkey-show-borders" style="background-color:#FFFFA0;border:0">
    <tr>
        <td style="background-color:#FFFFFF;border:0">&nbsp;</td>
        <th colspan="<%= 2* assay.getCutoffs().length %>">Cutoff Dilutions</th>
    </tr>
    <tr>
        <td style="background-color:#FFFFFF;border:0">&nbsp;</td>
        <th style="text-align:center" colspan=<%= assay.getCutoffs().length %>>Curve Based</th>
        <th style="text-align:center" colspan=<%= assay.getCutoffs().length %>>Point Based</th>
    </tr>
    <tr>
        <td style="background-color:#FFFFFF;border:0 1px 0 0">&nbsp;</td>
        <%
            for (int set = 0; set < 2; set++)
            {
                for (int cutoff : assay.getCutoffs())
                {
            %>
            <th  style="text-align:center"><%= cutoff %>%</th>
            <%
                }
            }
        %>
    </tr>
    <%
        for (NabAssayRun.SampleResult results : bean.getSampleResults())
        {
            String unableToFitMessage = null;

            DilutionSummary summary = results.getDilutionSummary();
            try
            {
                summary.getCurve();
            }
            catch (DilutionCurve.FitFailedException e)
            {
                unableToFitMessage = e.getMessage();
            }
    %>
    <tr>
        <td>
            <%=h(results.getCaption())%>
        </td>
        <%
            for (int set = 0; set < 2; set++)
            {
                for (int cutoff : assay.getCutoffs())
                {
                    %>
        <td style="text-align:right">
                    <%
                    boolean curveBased = set == 0;

                    if (curveBased && unableToFitMessage != null)
                    {
            %>
                N/A<%= helpPopup("Unable to fit curve", unableToFitMessage)%>
            <%
                    }
                    else
                    {
                        double val = curveBased ? summary.getCutoffDilution(cutoff / 100.0, assay.getRenderedCurveFitType()) :
                                summary.getInterpolatedCutoffDilution(cutoff / 100.0, assay.getRenderedCurveFitType());
                        if (val == Double.NEGATIVE_INFINITY)
                            out.write("&lt; " + Luc5Assay.intString(summary.getMinDilution(assay.getRenderedCurveFitType())));
                        else if (val == Double.POSITIVE_INFINITY)
                            out.write("&gt; " + Luc5Assay.intString(summary.getMaxDilution(assay.getRenderedCurveFitType())));
                        else
                        {
                            DecimalFormat shortDecFormat;
                            if (summary.getMethod() == SampleInfo.Method.Concentration)
                                shortDecFormat = new DecimalFormat("0.###");
                            else
                                shortDecFormat = new DecimalFormat("0");

                            out.write(shortDecFormat.format(val));
                        }
                    }
                        %>
            </td>
                        <%
                }
            }
        %>
    </tr>
    <%
        }
    %>
</table>
