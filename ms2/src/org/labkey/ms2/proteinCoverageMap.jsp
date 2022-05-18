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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.protein.ProteinFeature" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.api.visualization.ColorGradient" %>
<%@ page import="org.labkey.ms2.MS2Controller" %>
<%@ page import="java.awt.*" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    var currentURL = getActionURL();
    var viewByParam = currentURL.getParameter("viewBy");
    var isIntensityView = viewByParam == null || viewByParam.equalsIgnoreCase("intensity");
    var isConfidenceView = viewByParam != null && viewByParam.equalsIgnoreCase("confidenceScore");
%>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("MS2/ProteinCoverageMap.css");
        dependencies.add("MS2/ProteinCoverageMap.js");
        dependencies.add("MS2/PeptideCharacteristicLegend.js");
        dependencies.add("util.js");
        dependencies.add("internal/jQuery");
        dependencies.add("vis/vis");
    }
%>
<div class="viewSettings">
    <h5><b>View Settings </b></h5>

    <label for="peptide-setting-select">By:</label>
    <select name="peptideSettings" id="peptide-setting-select" onchange="LABKEY.ms2.PeptideCharacteristicLegend.changeView()">
        <option value="intensity" <%=isIntensityView ? h("selected") : h("")%> >Intensity</option>
        <option value="confidenceScore" <%=isConfidenceView ? h("selected") : h("")%> >Confidence Score</option>
    </select>
</div>
<%

    List<Double> iValues = new ArrayList<>();
    var peptideCharacteristics = bean.protein.getPeptideCharacteristics();
    Map<Double, String> heatMapColorHex = new HashMap<>();
    if (peptideCharacteristics != null)
    {

        if (isIntensityView)
        {
            peptideCharacteristics.sort((o1, o2) -> {
                if (o1.getIntensity() == null && o2.getIntensity() == null) return 0;
                if (o2.getIntensity() == null) return 1;
                if (o1.getIntensity() == null) return -1;
                return o2.getIntensity().compareTo(o1.getIntensity());
            });
            peptideCharacteristics.forEach(peptideCharacteristic -> {
                if (peptideCharacteristic.getIntensity() != null)
                {
                    iValues.add(peptideCharacteristic.getIntensity());
                }
            });
        }
        if (isConfidenceView)
        {
            peptideCharacteristics.sort((o1, o2) -> {
                if (o1.getConfidence() == null && o2.getConfidence() == null) return 0;
                if (o2.getConfidence() == null) return 1;
                if (o1.getConfidence() == null) return -1;
                return o2.getConfidence().compareTo(o1.getConfidence());
            });
            peptideCharacteristics.forEach(peptideCharacteristic -> {
                if (peptideCharacteristic.getConfidence() != null)
                {
                    iValues.add(peptideCharacteristic.getConfidence());
                }
            });
        }

        if (iValues.size() > 1)
        {
            // calculate medianIndex of protein.getPeptideCharacteristics()
            var count = iValues.size();
            var medianIndex = 0;

            if (count % 2 == 0)
            {
                medianIndex = (count / 2 - 1 + count / 2) / 2;
            }
            else
            {
                medianIndex = (count - 1) / 2;
            }

            // assign blue colors from median to last -> lighter to darker
            Color one = Color.WHITE;
            Color two = new Color(0, 81, 138);
            List<Color> blueGradient = ColorGradient.createGradient(one, two, (count - medianIndex + 1));

            // assign blue colors from median to last -> lighter to darker
            for (int i = medianIndex + 1; i < count; i++)
            {
                var peptideColor = blueGradient.get(i - medianIndex);
                var peptideCharacteristic = peptideCharacteristics.get(i);
                var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                if (isIntensityView)
                {
                    heatMapColorHex.put(peptideCharacteristics.get(i).getIntensity(), hexColor);
                }
                if (isConfidenceView)
                {
                    heatMapColorHex.put(peptideCharacteristics.get(i).getConfidence(), hexColor);
                }
                peptideCharacteristic.setColor(hexColor);
                // for blue - contrasting foreground color is White according to the tool
                // https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html
                peptideCharacteristic.setForegroundColor(ColorGradient.getContrastingForegroundColor(peptideColor));
            }

            // assign red colors from median to first -> lighter to darker
            one = new Color(187, 78, 78);
            two = Color.WHITE;

            List<Color> redGradient = ColorGradient.createGradient(one, two, medianIndex + 1);

            for (int i = medianIndex; i >= 0; i--)
            {
                var peptideColor = redGradient.get(i);
                var peptideCharacteristic = peptideCharacteristics.get(i);
                var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                if (isIntensityView)
                {
                    heatMapColorHex.put(peptideCharacteristics.get(i).getIntensity(), hexColor);
                }
                if (isConfidenceView)
                {
                    heatMapColorHex.put(peptideCharacteristics.get(i).getConfidence(), hexColor);
                }
                peptideCharacteristic.setColor(hexColor);
                peptideCharacteristic.setForegroundColor(ColorGradient.getContrastingForegroundColor(peptideColor));
            }
        }
    }

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

    LABKEY.ms2.PeptideCharacteristicLegend.addHeatMap(<%=toJsonArray(iValues)%>, <%=toJsonObject(heatMapColorHex)%>);
</script>