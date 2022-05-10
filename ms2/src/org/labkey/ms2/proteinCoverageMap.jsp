<%
/*
 * Copyright (c) 2011-2012 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.protein.ProteinFeature" %>
<%@ page import="org.labkey.api.protein.PeptideCharacteristic" %>
<%@ page import="java.util.StringJoiner" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.awt.*" %>
<%@ page import="org.labkey.api.data.statistics.StatsService" %>
<%@ page import="org.labkey.api.data.statistics.MathStat" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("MS2/ProteinCoverageMap.css");
        dependencies.add("MS2/ProteinCoverageMap.js");
        dependencies.add("MS2/PeptideIntensityHeatMap.js");
        dependencies.add("util.js");
        dependencies.add("internal/jQuery");
        dependencies.add("vis/lib/d3-3.5.17.min.js");
    }
%>
<div class="viewSettings">
    <h5><b>View Settings </b></h5>

    <label for="peptide-setting-select">By:</label>
    <select name="peptideSettings" id="peptide-setting-select">
        <option value="intensity">Intensity</option>
        <option value="confidenceScore">Confidence Score</option>
    </select>
</div>
<%

    List<Double> iValues = new ArrayList<>();
    var peptideCharacteristics = bean.protein.getPeptideCharacteristics();

    Map<Double, Integer> heatMapColorRGB = new HashMap<>();

    peptideCharacteristics.sort(Comparator.comparing(PeptideCharacteristic::getIntensity).reversed());
    peptideCharacteristics.forEach(peptideCharacteristic -> iValues.add(peptideCharacteristic.getIntensity()));
    // calculate medianIndex of protein.getPeptideCharacteristics()
    var count = peptideCharacteristics.size();
    var medianIndex = 0;

    if (count % 2 == 0)
    {
        medianIndex = (count / 2 - 1 + count / 2) / 2;
    }
    else
    {
        medianIndex = (count -1) / 2;
    }
    // assign white color to median
    heatMapColorRGB.put(peptideCharacteristics.get(medianIndex).getIntensity(), Color.WHITE.getRGB());

    // assign blue colors from median to last -> lighter to darker
    heatMapColorRGB.put(peptideCharacteristics.get(medianIndex+1).getIntensity(), Color.BLUE.getRGB());
    for (int i = medianIndex+2; i < count; i++)
    {
        var color = heatMapColorRGB.get(peptideCharacteristics.get(i-1).getIntensity());
        var peptideColor = new Color(color).darker().getRGB();
        heatMapColorRGB.put(peptideCharacteristics.get(i).getIntensity(), peptideColor);
        peptideCharacteristics.get(i).setIntensityColor("#"+Integer.toHexString(peptideColor).substring(2));
    }
    // assign red colors from median to first -> lighter to darker
    heatMapColorRGB.put(peptideCharacteristics.get(medianIndex-1).getIntensity(), Color.RED.getRGB());
    for (int i = medianIndex-2; i >= 0; i--)
    {
        var color = heatMapColorRGB.get(peptideCharacteristics.get(i+1).getIntensity());
        var peptideColor = new Color(color).darker().getRGB();
        heatMapColorRGB.put(peptideCharacteristics.get(i).getIntensity(), peptideColor);
        peptideCharacteristics.get(i).setIntensityColor("#"+Integer.toHexString(peptideColor).substring(2));
    }

    Map<Double, String> heatMapColorHex = new HashMap<>();
    heatMapColorRGB.forEach((i,c) -> heatMapColorHex.put(i, "#"+Integer.toHexString(c).substring(2)));

%>
<div class="sequencePanel">
    <div class="coverageMap">
        <%=bean.protein.getCoverageMap(bean.run, bean.showRunUrl, bean.aaRowWidth, bean.features)%>
    </div>
    <div class="heatmap"></div>
<%
    if (!bean.features.isEmpty())
    {
        String[] colors =
                { "#e6194b","#3cb44b","#ffe119","#0082c8","#f58231","#911eb4","#46f0f0","#f032e6","#d2f53c","#fabebe",
                    "#008080","#e6beff","#aa6e28","#fffac8","#800000","#aaffc3","#808000","#ffd8b1","#000080","#000000" };
        int colorIndex = 0;

        Set<String> uniqueFeatures = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        uniqueFeatures.addAll(bean.features.stream().map(ProteinFeature::getType).collect(Collectors.toSet()));

        %>
        <div class="featuresControls">
            <div>
                <strong>Features</strong>
            </div>
            <div>
                <input id="showFeatures" type="checkbox" name="showFeatures" readonly="readonly" />
                <label for="showFeatures" style="padding-left: 5px; border-left: 5px solid lightgray">Select All</label>
            </div>
            <% for (String uniqueFeature : uniqueFeatures)
            {
                String id = "feature-" + StringUtils.replace(uniqueFeature, " ", "");
                String color = colors.length > colorIndex ? colors[colorIndex++] : "#808080";
            %>
            <div>
                <input type="checkbox" onchange="LABKEY.ms2.ProteinCoverageMap.toggleStyleColor(<%= hq(id) %>, <%= hq(color) %>)"
                       id="<%= h(id) %>" value="<%= h(id) %>" class="featureCheckboxItem" />
                <label style="padding-left: 5px; border-left: 5px solid <%= h(color)%>" for="<%= h(id) %>">
                    <%= h(StringUtils.capitalize(uniqueFeature)) %>
                </label>
            </div>
            <% } %>
        </div>
<% } %>



<script type="application/javascript" nonce="<%=getScriptNonce()%>">
    LABKEY.ms2.ProteinCoverageMap.registerSelectAll();
    LABKEY.ms2.PeptideIntensityHeatMap.addHeatMap(<%=toJsonArray(iValues)%>, <%=toJsonObject(heatMapColorHex)%>);
</script>