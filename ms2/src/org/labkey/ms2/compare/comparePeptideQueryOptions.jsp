<%
/*
 * Copyright (c) 2008-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.data.MultiValuedForeignKey" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="org.labkey.ms2.MS2Manager" %>
<%@ page import="org.labkey.ms2.query.FilterView" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.JspBase"%>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
JspView<MS2Controller.CompareOptionsBean> view = (JspView<MS2Controller.CompareOptionsBean>) HttpView.currentView();
MS2Controller.CompareOptionsBean bean = view.getModelBean();
MS2Controller.PeptideFilteringComparisonForm form = bean.getForm();
String peptideViewName = form.getPeptideCustomViewName(getViewContext());
%>

<script type="text/javascript" src="<%= org.labkey.api.settings.AppProps.getInstance().getContextPath() %>/MS2/inlineViewDesigner.js"></script>

<form action="<%= bean.getTargetURL() %>" name="peptideFilterForm">
    <input name="runList" type="hidden" value="<%= bean.getRunList() %>" />
    <p>Peptides to include in the comparison:</p>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" value="<%= MS2Controller.ProphetFilterType.none %>" <%= form.isNoPeptideFilter() ? "checked=\"true\"" : "" %> /> All peptides</div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="peptideProphetRadioButton" value="<%= MS2Controller.ProphetFilterType.probability %>" <%= form.isPeptideProphetFilter() ? "checked=\"true\"" : "" %>/> Peptides with PeptideProphet probability &ge; <input onfocus="document.getElementById('peptideProphetRadioButton').checked=true;" type="text" size="2" name="<%= MS2Controller.PeptideFilteringFormElements.peptideProphetProbability %>" value="<%= form.getPeptideProphetProbability() == null ? "" : form.getPeptideProphetProbability() %>" /></div>
    <div class="labkey-indented"><input type="radio" name="<%= MS2Controller.PeptideFilteringFormElements.peptideFilterType %>" id="<%= FilterView.PEPTIDES_CUSTOM_VIEW_RADIO_BUTTON %>" value="<%= MS2Controller.ProphetFilterType.customView %>" <%= form.isCustomViewPeptideFilter() ? "checked=\"true\"" : "" %>/>
        Peptides that meet the filter criteria in a custom view:
        <% String peptideViewSelectId = bean.getPeptideView().renderViewList(request, out, peptideViewName); %>
        <%= PageFlowUtil.textLink("Create or Edit View", (ActionURL)null, "showViewDesigner('" + org.labkey.ms2.query.MS2Schema.HiddenTableType.PeptidesFilter + "', 'peptidesCustomizeView', " + PageFlowUtil.jsString(peptideViewSelectId) + "); return false;", "editPeptidesViewLink") %>

        <br/>
        <br/>
        <div id="peptidesCustomizeView" class="labkey-indented"></div>
    </div>
    <hr/>
    <p>
        Optionally require that peptides have a sequence match in protein: <input type="text" size="30" name="<%= MS2Controller.PeptideFilteringFormElements.targetProtein %>" value="<%= form.getTargetProtein()==null ? "" : form.getTargetProtein() %>" />
        <%= PageFlowUtil.helpPopup("Protein Filter", "<p>Show only peptides whose sequences match against a specified protein. It need not be the protein mapped to the peptide by the search engine or ProteinProphet.</p><p>If no protein matches the name specified, or if multiple proteins match, this page will be redisplayed to correct the search.</p>", true)%>
    </p>
    <div class="labkey-indented"><span class="labkey-error" > <%= form.getTargetProteinMsg()== null ? "" : form.getTargetProteinMsg()  %></span></div>
   <%
       StringBuffer links= new StringBuffer();
       if (null != form.getMatchingSeqIds() && null != form.getMatchingProtNames())
       {
           String[] ids = form.getMatchingSeqIds().split(",");
           String[] names = form.getMatchingProtNames().split(",");
           ActionURL url;
           if (ids.length == names.length)
           {
               for (int i=0; i< ids.length; i++)
               {
                   url= this.getViewContext().getActionURL().clone();
                   url.setAction(bean.getTargetURL().getAction());
                   url.deleteParameter(MS2Controller.PeptideFilteringFormElements.targetProtein);
                   url.deleteParameter(MS2Controller.PeptideFilteringFormElements.targetSeqId);
                   url.addParameter(MS2Controller.PeptideFilteringFormElements.targetProtein.name(),names[i]);
                   url.addParameter(MS2Controller.PeptideFilteringFormElements.targetSeqId.name(),ids[i]);
                   links.append("&nbsp;&nbsp;&nbsp;");
                   links.append(PageFlowUtil.textLink(names[i],url));
                   links.append("<br/>");
               }
           }
           else
               links.append("  Error parsing potential matches, try a more specific protein search term ");
       }
   %>
   <div class="labkey-indented"><%= links.toString()  %> </div>


    <p><labkey:button text="Compare"/></p>
</form>
