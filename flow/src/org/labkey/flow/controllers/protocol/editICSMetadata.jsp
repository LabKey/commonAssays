<%
/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.data.CompareType" %>
<%@ page import="org.labkey.api.data.FilterInfo" %>
<%@ page import="org.labkey.api.query.FieldKey" %>
<%@ page import="org.labkey.flow.controllers.protocol.EditICSMetadataForm" %>
<%@ page import="org.labkey.flow.controllers.protocol.ProtocolController" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page extends="org.labkey.api.jsp.FormPage" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%! void addCompare(Map<String, String> options, CompareType ct)
{
    options.put(ct.getPreferredUrlKey(), ct.getDisplayValue());
}%>
<%
    EditICSMetadataForm form = (EditICSMetadataForm)__form;

    Map<FieldKey, String> keywordSampleOptions = new LinkedHashMap();
    keywordSampleOptions.put(null, "");
    keywordSampleOptions.putAll(form.getKeywordAndSampleFieldMap(false));

    Map<FieldKey, String> keywordSampleStatsOptions = new LinkedHashMap();
    keywordSampleStatsOptions.put(null, "");
    keywordSampleStatsOptions.putAll(form.getKeywordAndSampleFieldMap(true));

    Map<String, String> opOptions = new LinkedHashMap();
    addCompare(opOptions, CompareType.EQUAL);
    addCompare(opOptions, CompareType.NEQ_OR_NULL);
    addCompare(opOptions, CompareType.ISBLANK);
    addCompare(opOptions, CompareType.NONBLANK);
    addCompare(opOptions, CompareType.GT);
    addCompare(opOptions, CompareType.LT);
    addCompare(opOptions, CompareType.GTE);
    addCompare(opOptions, CompareType.LTE);
    addCompare(opOptions, CompareType.CONTAINS);
    addCompare(opOptions, CompareType.DOES_NOT_CONTAIN);
    addCompare(opOptions, CompareType.DOES_NOT_START_WITH);
    addCompare(opOptions, CompareType.STARTS_WITH);
    addCompare(opOptions, CompareType.IN);

    ActionURL submitURL = form.getProtocol().urlFor(ProtocolController.EditICSMetadataAction.class);
    if (form.getReturnActionURL() != null)
        submitURL.addParameter(ActionURL.Param.returnUrl, form.getReturnActionURL().toString());
%>
<labkey:errors />
<labkey:form action="<%=submitURL%>" method="POST">
        <labkey:panel title="Sample Columns">
            <p>The LabKey study module uses the pair of participant and visit/date to identify the sample and uses
            this information to combine multiple assays together.</p>
            <p>Select the columns that identify the sample using FCSFile keywords or sample properties.
            Select either a specimen ID column or a participant column and one of visit or date columns.</p>
            <table class="lk-fields-table">
                <tr>
                    <td>Specimen ID column</td>
                    <td>
                        <select name="ff_specimenIdColumn"><labkey:options value="<%=form.specimenIdColumn%>" map="<%=keywordSampleOptions%>" /></select>
                    </td>
                </tr>
                <tr>
                    <td>Participant column</td>
                    <td>
                        <select name="ff_participantColumn"><labkey:options value="<%=form.participantColumn%>" map="<%=keywordSampleOptions%>" /></select>
                    </td>
                </tr>
                <tr>
                    <td>Visit column</td>
                    <td>
                        <select name="ff_visitColumn"><labkey:options value="<%=form.visitColumn%>" map="<%=keywordSampleOptions%>" /></select>
                    </td>
                </tr>
                <tr>
                    <td>Date column</td>
                    <td>
                        <select name="ff_dateColumn"><labkey:options value="<%=form.dateColumn%>" map="<%=keywordSampleOptions%>" /></select>
                    </td>
                </tr>
            </table>
        </labkey:panel>

        <labkey:panel title="Background and Foreground Match Columns">
            <p>Select the columns that match between both the foreground and background wells.</p>
            <p>For example, you usually want to match wells from the same FCSAnalysis Run and from
            the sample draw.</p>
            <table class="lk-fields-table">
                <% for (int i = 0; i < EditICSMetadataForm.MATCH_COLUMNS_MAX; i++) { %>
                <tr>
                    <td><%=i == 0 ? "&nbsp;" : "and"%></td>
                    <td>
                        <select name="ff_matchColumn"><labkey:options value="<%=form.matchColumn[i]%>" map="<%=keywordSampleOptions%>" /></select>
                    </td>
                </tr>
                <% } %>
            </table>
        </labkey:panel>

        <labkey:panel title="Background Column and Value">
            <p>Specify the column and value filter(s) which uniquely identify the background wells from
            the foreground wells.<br>If multiple wells match the background criteria, the
            background value will be the average of all the matched wells.</p>
            <p>For example, if your background wells may have either "neg" or "NegControl" for
            the "Peptide" keyword then select column "Keyword Peptide", select operator "Equals One Of",
            and set the value to "neg;NegControl".
            Multiple values are combined with ';' when using the "Equals One Of" operator.</p>
            <table class="lk-fields-table">
                <tr>
                    <td>&nbsp;</td>
                    <td style="font-weight: bold;">Background Column</td>
                    <td style="font-weight: bold;">Operator</td>
                    <td style="font-weight: bold;">Value</td>
                </tr>
                <%
                    for (int i = 0; i < EditICSMetadataForm.BACKGROUND_COLUMNS_MAX; i++) {
                        FilterInfo filter = form.backgroundFilter[i];
                        %>
                        <tr>
                            <td><%=i == 0 ? "&nbsp;" : "and"%></td>
                            <td><select name="ff_backgroundFilterField"><labkey:options value="<%=filter == null ? null : filter.getField()%>" map="<%=keywordSampleStatsOptions%>" /></select></td>
                            <td><select name="ff_backgroundFilterOp"><labkey:options value="<%=filter == null ? null : filter.getOp().getPreferredUrlKey()%>" map="<%=opOptions%>" /></select></td>
                            <td><input name="ff_backgroundFilterValue" type="text" value="<%=h(filter == null ? null : filter.getValue())%>"></td>
                        </tr>
                        <%
                    }
                %>
            </table>
        </labkey:panel>
    <labkey:button text="Set ICS Metadata" /> <labkey:button text="Cancel" href="<%=form.getProtocol().urlShow()%>"/>
</labkey:form>
