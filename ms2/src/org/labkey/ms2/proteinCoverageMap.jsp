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
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.TreeMap" %>
<%@ page import="java.util.TreeSet" %>
<%@ page import="java.util.stream.Collectors" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.labkey.api.protein.PeptideCharacteristic" %>
<%@ page import="org.labkey.api.ms.Replicate" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%
    MS2Controller.ProteinViewBean bean = ((JspView<MS2Controller.ProteinViewBean>)HttpView.currentView()).getModelBean();
    var currentURL = getActionURL();
    var displayLegend = true;
    var viewByParam = currentURL.getParameter("viewBy");
    var replicateIdParam = currentURL.getParameter("replicateId");
    var peptideFormParam = currentURL.getParameter("peptideForm");
    var isIntensityView = viewByParam == null || viewByParam.equalsIgnoreCase("intensity");
    var isConfidenceView = viewByParam != null && viewByParam.equalsIgnoreCase("confidenceScore");
    var isCombined = peptideFormParam == null ||
            peptideFormParam.equalsIgnoreCase(PeptideCharacteristic.COMBINED_PEPTIDE) ||
            (!peptideFormParam.equalsIgnoreCase(PeptideCharacteristic.STACKED_PEPTIDE));

    // list to store values min, max and mean values displayed on legend
    List<Double> legendValues =  new ArrayList<>();

    Map<Long, String> replicates = new TreeMap<>();
    replicates.put(Long.valueOf(0), "All");
    bean.replicates.forEach(rep -> replicates.put(rep.getId(), rep.getName()));

    var selectedReplicate = replicateIdParam != null && !Long.valueOf(replicateIdParam).equals(Long.valueOf(0)) ? Long.valueOf(replicateIdParam) : replicates.get(Long.valueOf(0));
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
<%
    if (bean.showViewSettings)
    {
%>
<div class="viewSettings">
    <h5><b>View Settings </b></h5>

    <label for="peptide-setting-select">By:</label>
    <select name="peptideSettings" id="peptide-setting-select" onchange="LABKEY.ms2.PeptideCharacteristicLegend.changeView('viewBy', 'peptide-setting-select')">
        <option value="intensity" <%=isIntensityView ? h("selected") : h("")%> >Intensity</option>
        <option value="confidenceScore" <%=isConfidenceView ? h("selected") : h("")%> >Confidence Score</option>
    </select>

    <label for="peptide-replicate-select">Replicate:</label>
    <select name="replicateSettings" id="peptide-replicate-select"  onchange="LABKEY.ms2.PeptideCharacteristicLegend.changeView('replicateId', 'peptide-replicate-select')">
        <labkey:options map="<%=replicates%>" value="<%=selectedReplicate%>"/>
    </select>


    <label>Modified forms:</label>
    <span class="peptideForms">
        <input type="radio" name="combinedOrStacked" id="combined" value="combined" <%=checked(isCombined)%> onclick="LABKEY.ms2.PeptideCharacteristicLegend.changeView('peptideForm', 'combined')"/>
        <label for="combined">Combined</label>

        <span class="stackedForm" >
            <input type="radio" name="combinedOrStacked" id="stacked" value="stacked" <%=checked(!isCombined)%> onclick="LABKEY.ms2.PeptideCharacteristicLegend.changeView('peptideForm', 'stacked')"/>
            <label for="stacked" >Stacked</label>
        </span>
    </span>

</div>
<%
    }
    // helper list of intensity or confidence score values of peptides used for calculations
    List<Double> iValues = new ArrayList<>();

    // combinedPeptideCharacteristics are always used for legend values
    var combinedPeptideCharacteristics = bean.protein.getCombinedPeptideCharacteristics();
    // modifiedPeptideCharacteristics are used for stacked view of peptides
    var modifiedPeptideCharacteristics = bean.protein.getModifiedPeptideCharacteristics();
    List<PeptideCharacteristic> peptidesForSequenceMapDisplay;

    if (isCombined)
    {
        peptidesForSequenceMapDisplay = combinedPeptideCharacteristics;
    }
    else
    {
        peptidesForSequenceMapDisplay = modifiedPeptideCharacteristics;
    }
    Map<Double, String> heatMapColorHex = new HashMap<>();
    if (combinedPeptideCharacteristics != null)
    {
        if (isIntensityView)
        {
            // sorting for calculating the intensity rank
            peptidesForSequenceMapDisplay.sort((o1, o2) -> {
                if (o1.getIntensity() == null && o2.getIntensity() == null) return 0;
                if (o2.getIntensity() == null) return 1;
                if (o1.getIntensity() == null) return -1;
                return o2.getIntensity().compareTo(o1.getIntensity());
            });

            for (int i = 0; i < peptidesForSequenceMapDisplay.size(); i++)
            {
                var peptideCharacteristic = peptidesForSequenceMapDisplay.get(i);
                if (peptideCharacteristic.getIntensity() != null && peptideCharacteristic.getIntensity() != 0)
                {
                    peptideCharacteristic.setIntensityRank(i + 1); // ranks are 1 based
                    iValues.add(peptideCharacteristic.getIntensity());
                }
            }

        }
        if (isConfidenceView)
        {
            // sorting for calculating the confidence score ranks
            peptidesForSequenceMapDisplay.sort((o1, o2) -> {
                if (o1.getConfidence() == null && o2.getConfidence() == null) return 0;
                if (o2.getConfidence() == null) return 1;
                if (o1.getConfidence() == null) return -1;
                return o2.getConfidence().compareTo(o1.getConfidence());
            });
            for (int i = 0; i < peptidesForSequenceMapDisplay.size(); i++)
            {
                var peptideCharacteristic = peptidesForSequenceMapDisplay.get(i);
                if (peptideCharacteristic.getConfidence() != null && peptideCharacteristic.getConfidence() != 0)
                {
                    peptideCharacteristic.setConfidenceRank(i+1); // ranks are 1 based
                    iValues.add(peptideCharacteristic.getConfidence());
                }
            }
        }

        if (iValues.size() > 1)
        {
            // reason to initialize the values list is for the keys in hexColorMap below
            var start = Double.MIN_VALUE;
            // fixed size heatmap legend
            for (int i = 0; i < 11; i++)
            {
                legendValues.add(start);
                start = start - 0.01;
            }
            var max = Collections.max(iValues);
            var min = Collections.min(iValues);
            var avg = (max + min) / 2;

            if (max == min)
            {
                displayLegend = false;
            }
            else
            {

                // only display min, max and avg in the legend
                legendValues.set(0, max);
                legendValues.set(5, avg);
                legendValues.set(10, min);

                int closestToMeanIndex = 0;
                var minDiff = iValues.get(0);

                // calculate closest to mean index of peptidesForSequenceMapDisplay for sequence graph coloring
                for (int i = 0; i < peptidesForSequenceMapDisplay.size(); i++)
                {
                    var value = isIntensityView ? peptidesForSequenceMapDisplay.get(i).getIntensity() : peptidesForSequenceMapDisplay.get(i).getConfidence();
                    var diff = Math.abs(value - avg);
                    if (diff < minDiff)
                    {
                        minDiff = diff;
                        closestToMeanIndex = i;
                    }
                }

                // assign blue colors from mean value to last -> lighter to darker
                Color one = new Color(0, 81, 138);
                Color two = Color.WHITE;
                List<Color> blueGradient = ColorGradient.createGradient(one, two, 5);
                heatMapColorHex.put(legendValues.get(5), "#" + Integer.toHexString(two.getRGB()).substring(2));

                // to color the heat map legend
                Collections.reverse(blueGradient);
                for (int i = 6; i < legendValues.size(); i++)
                {
                    var peptideColor = blueGradient.get(i - 6);
                    var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                    heatMapColorHex.put(legendValues.get(i), hexColor);
                }

                // to color the peptide bars in sequence graph
                blueGradient = ColorGradient.createGradient(one, two, (peptidesForSequenceMapDisplay.size() - closestToMeanIndex + 1));
                Collections.reverse(blueGradient);

                // assign blue colors from median to last -> lighter to darker
                for (int i = closestToMeanIndex + 1; i < peptidesForSequenceMapDisplay.size(); i++)
                {
                    var peptideColor = blueGradient.get(i - closestToMeanIndex + 1);
                    var peptideCharacteristic = peptidesForSequenceMapDisplay.get(i);
                    var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                    peptideCharacteristic.setColor(hexColor);
                    // for blue - contrasting foreground color is White according to the tool
                    // https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html
                    peptideCharacteristic.setForegroundColor(ColorGradient.getContrastingForegroundColor(peptideColor));
                }

                peptidesForSequenceMapDisplay.get(peptidesForSequenceMapDisplay.size() - 1).setColor("#" + Integer.toHexString(one.getRGB()).substring(2));

                // assign red colors from median to first -> lighter to darker
                one = new Color(187, 78, 78);

                // to color the heat map legend
                List<Color> redGradient = ColorGradient.createGradient(one, two, 5);

                for (int i = 4; i >= 0; i--)
                {
                    var peptideColor = redGradient.get(i);
                    var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                    heatMapColorHex.put(legendValues.get(i), hexColor);
                }

                // to color the peptide bars in sequence graph
                redGradient = ColorGradient.createGradient(one, two, closestToMeanIndex + 1);

                for (int i = closestToMeanIndex; i >= 0; i--)
                {
                    var peptideColor = redGradient.get(i);
                    var peptideCharacteristic = peptidesForSequenceMapDisplay.get(i);
                    var hexColor = "#" + Integer.toHexString(peptideColor.getRGB()).substring(2);
                    peptideCharacteristic.setColor(hexColor);
                    peptideCharacteristic.setForegroundColor(ColorGradient.getContrastingForegroundColor(peptideColor));
                }
                heatMapColorHex.keySet().removeAll(Collections.singleton(null));
            }
        }
    }

%>
<div class="sequencePanel">
    <div class="coverageMap">
        <%=bean.protein.getCoverageMap(bean.run, bean.showRunUrl, bean.aaRowWidth, bean.features)%>
    </div>
<%
    var legendLabel = "";
    var legendScale = "";
    if (!legendValues.isEmpty())
    {
        if (isIntensityView)
        {
            legendLabel = "Intensity";
            legendScale = "(Log 10 base)";
        }
        else {
            legendLabel = "Confidence Score";
            legendScale = "-(Log 10 base)";
        }
    }

    if (displayLegend)
    {
%>
        <div class="heatmap">
            <div>
                <strong><%=h(legendLabel)%></strong>
            </div>
            <div class="heatmap-legendScale">
                <strong><%=h(legendScale)%></strong>
            </div>
        </div>
<%
    }
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

    <% if (displayLegend)
        {
    %>
        LABKEY.ms2.PeptideCharacteristicLegend.addHeatMap(<%=toJsonArray(legendValues)%>, <%=toJsonObject(heatMapColorHex)%>);
    <% } %>
</script>