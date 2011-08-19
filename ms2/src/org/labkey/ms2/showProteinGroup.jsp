<%
/*
 * Copyright (c) 2006-2010 LabKey Corporation
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
<%@ page import="java.text.Format" %>
<%@ page import="org.labkey.ms2.reader.ITraqProteinQuantitation" %>
<%
    ProteinGroupWithQuantitation group = ((JspView<ProteinGroupWithQuantitation>)HttpView.currentView()).getModelBean();
    ITraqProteinQuantitation libra = group.getITraqProteinQuantitation();
    Format floatFormat = Formats.f2;
%>
<table>
    <tr>
        <td class="labkey-form-label">Group number</td>
        <td><%= group.getGroupNumber() %><% if (group.getIndistinguishableCollectionId() != 0) { %>-<%= group.getIndistinguishableCollectionId() %><% } %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td class="labkey-form-label">Group probability</td>
        <td><%= floatFormat.format(group.getGroupProbability()) %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td class="labkey-form-label">Protein probability</td>
        <td><%= floatFormat.format(group.getProteinProbability()) %></td>
    </tr>
    <tr>
        <td class="labkey-form-label">Total number of peptides</td>
        <td><%= group.getTotalNumberPeptides() %></td>
        <td />
        <td class="labkey-form-label">Number of unique peptides</td>
        <td><%= group.getUniquePeptidesCount() %></td>
        <td />
        <% if (group.getPctSpectrumIds() != null) { %>
            <td class="labkey-form-label">Percent spectrum ids</td>
            <td><%= Formats.percent2.format(group.getPctSpectrumIds()) %></td>
        <% } %>
    </tr>
    <% if (group.getPercentCoverage() != null) { %>
        <tr>
            <td class="labkey-form-label">Percent coverage</td>
            <td><%= Formats.percent2.format(group.getPercentCoverage()) %></td>
        </tr>
    <% } %>

    <% if (group.getRatioMean() != null) { %>
        <tr>
            <td class="labkey-form-label">Ratio mean</td>
            <td><%= floatFormat.format(group.getRatioMean()) %></td>
            <td />
            <td class="labkey-form-label">Ratio standard dev</td>
            <td><%= floatFormat.format(group.getRatioStandardDev()) %></td>
            <td />
            <td class="labkey-form-label">Number of quantitation peptides</td>
            <td><%= group.getRatioNumberPeptides() %></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Heavy to light ratio mean</td>
            <td><%= floatFormat.format(group.getHeavy2LightRatioMean()) %></td>
            <td />
            <td class="labkey-form-label">Heavy to light standard dev</td>
            <td><%= floatFormat.format(group.getHeavy2LightRatioStandardDev()) %></td>
        </tr>
    <% } %>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td class="labkey-form-label">Jump to</td>
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

<%
// display the Libra quantitation ratio values, if applicable
if (libra != null)
{
%>
    <tr><td colspan="8">&nbsp;</td></tr>
    <tr><td colspan="8" style="font-size:110%;font-weight:bold;">iTRAQ Quantitation</td></tr>
    <tr>
    <% if (libra.getRatio1() != null) { %>
        <td class="labkey-form-label">Ratio 1</td><td><%= floatFormat.format(libra.getRatio1()) %></td><td />
    <% } %>
    <% if (libra.getRatio4() != null) { %>
        <td class="labkey-form-label">Ratio 4</td><td><%= floatFormat.format(libra.getRatio4()) %></td><td />
    <% } %>
    <% if (libra.getRatio7() != null) { %>
        <td class="labkey-form-label">Ratio 7</td><td><%= floatFormat.format(libra.getRatio7()) %></td>
    <% } %>
    </tr><tr>
    <% if (libra.getRatio2() != null) { %>
        <td class="labkey-form-label">Ratio 2</td><td><%= floatFormat.format(libra.getRatio2()) %></td><td />
    <% } %>
    <% if (libra.getRatio5() != null) { %>
        <td class="labkey-form-label">Ratio 5</td><td><%= floatFormat.format(libra.getRatio5()) %></td><td />
    <% } %>
    <% if (libra.getRatio8() != null) { %>
        <td class="labkey-form-label">Ratio 8</td><td><%= floatFormat.format(libra.getRatio8()) %></td>
    <% } %>
    </tr><tr>
    <% if (libra.getRatio3() != null) { %>
        <td class="labkey-form-label">Ratio 3</td><td><%= floatFormat.format(libra.getRatio3()) %></td><td />
    <% } %>
    <% if (libra.getRatio6() != null) { %>
        <td class="labkey-form-label">Ratio 6</td><td><%= floatFormat.format(libra.getRatio6()) %></td>
    <% } %>
    </tr>
<%
}
%>
</table>
