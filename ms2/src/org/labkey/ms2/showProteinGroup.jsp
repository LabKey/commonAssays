<%
/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
<%@ page import="org.labkey.ms2.reader.ITraqProteinQuantitation" %>
<%@ page import="java.text.Format" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
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
        <td><%= h(floatFormat.format(group.getGroupProbability())) %></td>
        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
        <td class="labkey-form-label">Protein probability</td>
        <td><%= h(floatFormat.format(group.getProteinProbability())) %></td>
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
            <td><%= h(Formats.percent2.format(group.getPctSpectrumIds())) %></td>
        <% } %>
    </tr>
    <% if (group.getPercentCoverage() != null) { %>
        <tr>
            <td class="labkey-form-label">Percent coverage</td>
            <td><%= h(Formats.percent2.format(group.getPercentCoverage())) %></td>
        </tr>
    <% } %>

    <% if (group.getRatioMean() != null) { %>
        <tr>
            <td class="labkey-form-label">Ratio mean</td>
            <td><%= h(floatFormat.format(group.getRatioMean())) %></td>
            <td />
            <td class="labkey-form-label">Ratio standard dev</td>
            <td><%= h(floatFormat.format(group.getRatioStandardDev())) %></td>
            <td />
            <td class="labkey-form-label">Number of quantitation peptides</td>
            <td><%= group.getRatioNumberPeptides() %></td>
        </tr>
        <tr>
            <td class="labkey-form-label">Heavy to light ratio mean</td>
            <td><%= h(floatFormat.format(group.getHeavy2LightRatioMean())) %></td>
            <td />
            <td class="labkey-form-label">Heavy to light standard dev</td>
            <td><%= h(floatFormat.format(group.getHeavy2LightRatioStandardDev())) %></td>
        </tr>
    <% } %>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td class="labkey-form-label">Jump to</td>
        <td colspan="7"><%
            List<Protein> proteins = group.lookupProteins();
            for (int i = 0; i < proteins.size(); i++)
            {
                Protein protein = proteins.get(i); %>
                <a href="#Protein<%= i %>"><%=h(protein.getLookupString())%></a>,
            <% } %>
            <a href="#Peptides">Peptides</a>
        </td>
    </tr>
</table>
<%
// display the Libra quantitation ratio values, if applicable
if (libra != null)
{
%>
<br/>
<table>
    <tr>
        <td class="labkey-form-label">iTRAQ/TMT Channel</td>
        <td class="labkey-form-label">Ratio</td>
        <td class="labkey-form-label">Error</td>
    </tr>
    <% if (libra.getRatio1() != null) { %>
        <tr>
            <td align="right">1</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio1())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError1())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio2() != null) { %>
        <tr>
            <td align="right">2</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio2())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError2())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio3() != null) { %>
        <tr>
            <td align="right">3</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio3())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError3())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio4() != null) { %>
        <tr>
            <td align="right">4</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio4())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError4())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio5() != null) { %>
        <tr>
            <td align="right">5</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio5())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError5())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio6() != null) { %>
        <tr>
            <td align="right">6</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio6())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError6())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio7() != null) { %>
        <tr>
            <td align="right">7</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio7())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError7())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio8() != null) { %>
        <tr>
            <td align="right">8</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio8())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError8())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio9() != null) { %>
        <tr>
            <td align="right">9</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio9())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError9())) %></td>
        </tr>
    <% } %>
    <% if (libra.getRatio10() != null) { %>
        <tr>
            <td align="right">10</td>
            <td align="right"><%= h(floatFormat.format(libra.getRatio10())) %></td>
            <td align="right"><%= h(floatFormat.format(libra.getError10())) %></td>
        </tr>
    <% } %>
</table>
<% } %>
