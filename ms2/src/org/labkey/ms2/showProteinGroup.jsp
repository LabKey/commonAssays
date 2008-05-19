<%
/*
 * Copyright (c) 2006-2008 LabKey Corporation
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
<%@ page import="org.labkey.api.util.Formats"%>
<%@ page import="org.labkey.api.view.HttpView"%>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.ms2.Protein" %>
<%@ page import="org.labkey.ms2.ProteinGroupWithQuantitation" %>
<%
    ProteinGroupWithQuantitation group = ((JspView<ProteinGroupWithQuantitation>)HttpView.currentView()).getModelBean();
%>
<table>
    <tr>
        <td>Group number:</td>
        <td><%= group.getGroupNumber() %><% if (group.getIndistinguishableCollectionId() != 0) { %>-<%= group.getIndistinguishableCollectionId() %><% } %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td>Group probability:</td>
        <td><%= group.getGroupProbability() %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td>Protein probability:</td>
        <td><%= group.getProteinProbability() %></td>
    </tr>
    <tr>
        <td>Total number of peptides:</td>
        <td><%= group.getTotalNumberPeptides() %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td>Number of unique peptides:</td>
        <td><%= group.getUniquePeptidesCount() %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td>Percent spectrum ids:</td>
        <td><%= group.getPctSpectrumIds() == null ? "" : Formats.percent2.format(group.getPctSpectrumIds()) %></td>
    </tr>
    <tr>
        <td>Percent coverage:</td>
        <td><%= group.getPercentCoverage() == null ? "" : Formats.percent2.format(group.getPercentCoverage()) %></td>
    </tr>

    <% if (group.getRatioMean() != null) { %>
        <tr>
            <td>Ratio mean:</td>
            <td><%= group.getRatioMean() %></td>
            <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td>Ratio standard dev:</td>
            <td><%= group.getRatioStandardDev() %></td>
            <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td>Number of quantitation peptides:</td>
            <td><%= group.getRatioNumberPeptides() %></td>
        </tr>
        <tr>
            <td>Heavy to light ratio mean:</td>
            <td><%= group.getHeavy2LightRatioMean() %></td>
            <td>&nbsp;&nbsp;&nbsp;&nbsp;</td>
            <td>Heavy to light standard dev:</td>
            <td><%= group.getHeavy2LightRatioStandardDev() %></td>
        </tr>
    <% } %>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td>Jump to:</td>
        <td colspan="7"><%
            Protein[] proteins = group.lookupProteins();
            for (int i = 0; i < proteins.length; i++)
            {
                Protein protein = proteins[i]; %>
                <a href="#Protein<%= i %>"><%= protein.getLookupString() %></a>,
            <% } %>
            <a href="#Peptides">Peptides</a>
        </td>
    </tr>
</table>
