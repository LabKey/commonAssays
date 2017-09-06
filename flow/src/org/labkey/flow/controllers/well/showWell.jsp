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
<%@ page import="org.labkey.api.announcements.DiscussionService" %>
<%@ page import="org.labkey.api.exp.OntologyManager" %>
<%@ page import="org.labkey.api.exp.api.ExpMaterial" %>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls" %>
<%@ page import="org.labkey.api.exp.property.Domain" %>
<%@ page import="org.labkey.api.exp.property.DomainProperty" %>
<%@ page import="org.labkey.api.jsp.JspLoader" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.security.SecurityPolicy" %>
<%@ page import="org.labkey.api.security.SecurityPolicyManager" %>
<%@ page import="org.labkey.api.security.User"%>
<%@ page import="org.labkey.api.security.permissions.ReadPermission" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.Tuple3" %>
<%@ page import="org.labkey.api.util.URIUtil" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowFCSAnalysis" %>
<%@ page import="org.labkey.flow.data.FlowFCSFile" %>
<%@ page import="org.labkey.flow.data.FlowRun" %>
<%@ page import="org.labkey.flow.data.FlowScript" %>
<%@ page import="org.labkey.flow.data.FlowWell" %>
<%@ page import="org.labkey.flow.query.FlowTableType" %>
<%@ page import="org.labkey.flow.reports.FlowReport" %>
<%@ page import="org.labkey.flow.reports.FlowReportManager" %>
<%@ page import="org.labkey.flow.view.GraphDataRegion" %>
<%@ page import="org.labkey.flow.view.SetCommentView" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.text.DecimalFormat" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page extends="org.labkey.flow.controllers.well.WellController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext3");
        dependencies.add("Flow/util.js");
        dependencies.add("TreeGrid.js");
    }
%>
<style type="text/css">
    .right {text-align:right;}
</style>
<script type="text/javascript">
Ext.QuickTips.init();
</script>
<%
    User user = getUser();
    FlowWell well = getWell();
    FlowWell fcsFile = well.getFCSFileInput();
    FlowFCSFile originalFile = well.getOriginalFCSFile();
    if (originalFile == null && fcsFile != null)
        originalFile = fcsFile.getOriginalFCSFile();
    FlowScript script = well.getScript();
    FlowCompensationMatrix matrix = well.getCompensationMatrix();

    NumberFormat percentageFormat = new DecimalFormat("0.0%");
    NumberFormat integerFormat = NumberFormat.getIntegerInstance();
    NumberFormat decimalFormat = new DecimalFormat("#,##0.00");
    StringBuilder jsonStats = new StringBuilder();
    jsonStats.append("[");
    String comma = "";
    for (Map.Entry<StatisticSpec, Double> statistic : getStatistics().entrySet())
    {
        StatisticSpec spec = statistic.getKey();
        Double value = statistic.getValue();
        jsonStats.append(comma);
        jsonStats.append("{");
        jsonStats.append("text:").append(PageFlowUtil.jsString(spec.toShortString())).append(",");
        if (null == spec.getSubset())
            jsonStats.append("subset:'',");
        else
        {
            jsonStats.append("subset:").append(PageFlowUtil.jsString(spec.getSubset().toString())).append(",");
            if (null != spec.getSubset().getParent())
                jsonStats.append("parent:").append(PageFlowUtil.jsString(spec.getSubset().getParent().toString())).append(",");
        }
        jsonStats.append("stat:").append(PageFlowUtil.jsString(spec.getStatistic().getShortName())).append(",");
        jsonStats.append("param:").append(PageFlowUtil.jsString(spec.getParameter())).append(",");
        String formattedValue;
        switch (spec.getStatistic())
        {
            case Frequency:
            case Freq_Of_Parent:
            case Freq_Of_Grandparent:
            case Percentile:
            case Median_Abs_Dev_Percent:
                formattedValue = percentageFormat.format(value/100);
                break;

            case Count:
                formattedValue = integerFormat.format(value);
                break;

            default:
                formattedValue = decimalFormat.format(value);
        }
        jsonStats.append("value:'").append(formattedValue).append("'");
        jsonStats.append("}");
        comma = ",\n";
    }
    jsonStats.append("]");

    Map<String,String> keywords = getKeywords();
    ArrayList<String> names = new ArrayList<>();
    names.add("");
    for (int i=1 ; keywords.containsKey("$P" + i + "N"); i++)
        names.add(PageFlowUtil.jsString(keywords.get("$P" + i + "N")));

    StringBuilder jsonKeywords = new StringBuilder();
    jsonKeywords.append("[");
    comma = "";
    Pattern p = Pattern.compile("\\$?P(\\d+).*");
    for (Map.Entry<String, String> entry : keywords.entrySet())
    {
        int index = 0;
        String keyword = entry.getKey();
        String value = entry.getValue();
        Matcher m = p.matcher(keyword);
        if (m.matches())
            index = Integer.parseInt(m.group(1));
        jsonKeywords.append(comma);
        jsonKeywords.append("{");
        jsonKeywords.append("index:").append(index);
        jsonKeywords.append(",name:").append(index>0&&index<names.size()?names.get(index):"''");
        jsonKeywords.append(",keyword:").append(PageFlowUtil.jsString(keyword));
        jsonKeywords.append(",value:").append(PageFlowUtil.jsString(value));
        jsonKeywords.append("}");
        comma = ",\n";
    }
    jsonKeywords.append("]");
%>
<script type="text/javascript">

function statisticsTree(statistics)
{
    var node, subset;
    var map = {};
    for (var i=0 ; i<statistics.length ; i++)
    {
        var s = statistics[i];
        node = map[s.subset];
        if (!node)
        {
            var text = s.subset;
            if (s.parent && 0==text.indexOf(s.parent+"/"))
                text = text.substring(s.parent.length+1);
            if (0==text.indexOf("(") && text.length-1 == text.lastIndexOf(")"))
                text = text.substring(1,text.length-2);
            node = new Ext.tree.TreeNode(Ext.apply({},{text:text, qtipCfg:{text:s.subset}, expanded:true, uiProvider:Ext.ux.tree.TreeGridNodeUI, parentNode:null}, s));    // stash original object in data
            map[s.subset] = node;
        }
        var name = s.stat;
        if (s.param)
            name = name + "(" + s.param + ")";
        node.attributes['__' + name] = s.value;
    }
    for (subset in map)
    {
        node = map[subset];
        var parentSubset = node.attributes.parent;
        if (!parentSubset)
            parentSubset = '';
        var parent = map[parentSubset];
        if (parent && parent != node)
        {
            parent.appendChild(node);
            node.attributes.parentNode = parent;
        }
    }
    var treeData = [];
    for (subset in map)
    {
        node = map[subset];
        if (!node.attributes.parentNode /*&& (node.childNodes.length > 0 || node.attributes.stats.length > 0 )*/)
            treeData.push(node);
    }
    return treeData;
}

function statisticsColumns(statistics)
{
    var map = {};
    var columns = [];

    for (var i=0 ; i<statistics.length ; i++)
    {
        var s = statistics[i];
        var name = s.stat;
        if (s.param)
            name = name + "(" + s.param + ")";
        if (!map[name])
        {
            var renderer = null;
            if ('Count' != s.stat)
                renderer = _toFixed;
            var dataIndex = '__' + name;
            var col = {header:Ext.util.Format.htmlEncode(name), dataIndex:dataIndex, tpl: "{[values['" + dataIndex + "'] ? values['" + dataIndex + "'] : '']}", width:80, align:'right', renderer:renderer, stat:s.stat, param:s.param};
            map[name] = col;
            columns.push(col);
        }
    }
    columns.sort(function(a,b) {
        var A = a.param ? a.param : "";
        var B = b.param ? b.param : "";
        if (A != B)
            return A < B ? -1 : 1;
        A = a.stat; B = b.stat;
        if (A == B)
            return 0;
        if (A == 'Count')
            return -1;
        if (B == 'Count')
            return 1;
        if (A == '%P')
            return -1;
        if (B == '%P')
            return 1;
        if (A == '%G')
            return -1;
        if (B == '%G')
            return 1;
        return A < B ? -1 : 1;
    });
    return columns;
}

function _toFixed(f)
{
    if (f == undefined)
        return "";
    if (f.toFixed)
        return f.toFixed(2);
    return f;
}

function _pad(i)
{
    var s = "" + i;
    return s.length > 2 ? s : "  ".substr(s.length) + s;
}

function showStatistics()
{
    var treeData = statisticsTree(statistics);
    var statsColumns = statisticsColumns(statistics);
    var population = [{header:'Population', dataIndex:'text', width:300}];
    var columns = population.concat(statsColumns);

    var tree = new Ext.ux.tree.TreeGrid({
        el:'statsTree',
        rootVisible:false,
        useArrows:true,
        autoScroll:false,
        autoHeight:true,
        animate:true,
        enableDD:false,
        containerScroll: false,
        columns: columns
    });

    var root = new Ext.tree.TreeNode({text:'-', expanded:true});
    for (var i=0 ; i<treeData.length ; i++)
        root.appendChild(treeData[i]);
    tree.setRootNode(root);
    tree.render();
    tree.updateColumnWidths();
}

function showKeywords()
{
    for (var i=0 ; i<keywords.length ; i++)
    {
        var o = keywords[i];
        o.label = o.index == 0 ? 'Keywords' : 'Parameter ' + _pad(o.index) + ' -- ' + o.name;
    }

    var store = new Ext.data.GroupingStore({
        reader: new Ext.data.JsonReader({id:'keyword'}, [{name:'index'},{name:'name'},{name:'label'},{name:'keyword'}, {name:'value'}]),
        data: keywords,
        sortInfo: {field:'keyword', direction:"ASC"},
        groupField:'label'});
    
    var grid = new Ext.grid.GridPanel({
        el:'keywordsGrid',
        autoScroll:false,
        autoHeight:true,
        width:600,
        store: store,
        columns:[
            {id:'keyword', header:'Keyword', dataIndex:'keyword'},
            {header:'Value', dataIndex:'value', width:200},
            {header:'Label', dataIndex:'label'}
        ]
        ,view: new Ext.grid.GroupingView({
            startCollapsed:true, hideGroupedColumn:true,
            forceFit:true,
            groupTextTpl: '{values.group}'
        })
    });

    grid.render();
}

Ext.onReady(function()
{
    if (statistics.length > 0)
    {
        showStatistics();
    }

    if (keywords.length > 0)
    {
        showKeywords();
    }
});

var statistics = <%=jsonStats%>;
var treeData;
var stats;
var keywords = <%=jsonKeywords%>;
</script>
<table class="lk-fields-table"><%

if (getRun() == null)
{
    %><tr><td colspan="2">The run has been deleted.</td></tr><%
}
else 
{
    %><tr><td>Run Name:</td><td><a href="<%=getRun().urlShow()%>"><%=h(getRun().getName())%></a></td></tr><%

    FlowExperiment experiment = getRun().getExperiment();
    if (experiment != null)
    {
        %><tr><td>Analysis Folder:</td><td><a href="<%=experiment.urlShow()%>"><%=h(experiment.getName())%></a></td></tr><%
    }
}
    %><tr><td>Well Name:</td><td><%=h(well.getName())%></td></tr><%

if (fcsFile != null && fcsFile != well)
{
    %><tr><td>FCS File:</td>
        <td>
            <a href="<%=h(fcsFile.urlShow())%>"><%=h(fcsFile.getName())%></a>
    <% if (originalFile != null && originalFile != well && originalFile != fcsFile) { %>
            (original <a href="<%=h(originalFile.urlShow())%>"><%=h(originalFile.getName())%></a>)
    <% } %>
        </td>
    </tr><%
}
else if (originalFile != null && originalFile != well && originalFile != fcsFile)
{
    %><tr><td>Original FCS File:</td><td><a href="<%=h(originalFile.urlShow())%>"><%=h(originalFile.getName())%></a></td></tr><%
}
    %><tr><td>Well Comment:</td>
        <td><%include(new SetCommentView(well), out);%></td>
    </tr><%

if (script != null)
{
    %><tr><td>Analysis Script:</td><td><a href="<%=h(script.urlShow())%>"><%=h(script.getName())%></a></td></tr><%
}
if (matrix != null)
{
    %><tr><td>Compensation Matrix:</td><td><a href="<%=h(matrix.urlShow())%>"><%=h(matrix.getName())%></a></td></tr><%
}

for (ExpMaterial sample : well.getSamples())
{
    %><tr><td><%=h(sample.getSampleSet().getName())%></td>
        <td><a href="<%=h(sample.detailsURL())%>"><%=h(sample.getName())%></a></td>
    </tr><%
}

for (Tuple3<FlowReport, Domain, FlowTableType> pair : FlowReportManager.getReportDomains(getContainer(), getUser()))
{
    FlowReport report = pair.first;
    Domain domain = pair.second;
    FlowTableType tableType = pair.third;

    String lsid = FlowReportManager.getReportResultsLsid(report, well);
    Map<String, Object> properties = OntologyManager.getProperties(getContainer(), lsid);

    %><tr><td>&nbsp;</td></tr><%
    %><tr><td><%=h(report.getDescriptor().getReportName())%> Report</td><td>&nbsp;</td><%
    for (DomainProperty dp : domain.getProperties())
    {
        String propertyURI = dp.getPropertyURI();
        if (properties.containsKey(propertyURI))
        {
            Object value = properties.get(propertyURI);
            %><tr><td>&nbsp;&nbsp;&nbsp;<%=h(dp.getName())%>:</td><td><%=h(String.valueOf(value))%></td></tr><%
        }
    }
    %></tr><%
}
%></table>
<%
    if (getContainer().hasPermission(getUser(), UpdatePermission.class))
    {
%><%= button("edit").href(well.urlFor(WellController.EditWellAction.class)) %><br/><br/>
<%
    }
%>

<div id="keywordsGrid" class="extContainer"></div>
<div id="statsTree" class="extContainer"></div>

<%
if (getGraphs().length > 0)
{
    final String graphSize = FlowPreference.graphSize.getValue(request);
    %><br/><%
    include(new JspView(JspLoader.createPage(GraphDataRegion.class, "setGraphSize.jsp")), out);
    for (GraphSpec graph : getGraphs())
    {
        %>
        <span style="display:inline-block; vertical-align:top; height:<%=h(graphSize)%>px; width:<%=h(graphSize)%>px;">
        <img style="width:<%=h(graphSize)%>px; height:<%=h(graphSize)%>px;" class='labkey-flow-graph' src="<%=h(getWell().urlFor(WellController.ShowGraphAction.class))%>&amp;graph=<%=PageFlowUtil.encode(graph.toString())%>" onerror="flowImgError(this);">
        </span><wbr>
        <%
    }
}


List<FlowFCSFile> relatedFiles = well.getFCSFileOutputs();
if (relatedFiles.size() > 0)
{
    %>
    <br/><br/>
    <h4>FCS Files derived from this file and associated FCS Analyses:</h4>
    <table class="labkey-data-region-legacy labkey-show-borders">
        <tr>
            <td class="labkey-column-header">Name</td>
            <td class="labkey-column-header">Run Name</td>
            <td class="labkey-column-header">Analysis Folder</td>
        </tr><%
    int count = 0;
    for (FlowFCSFile related : relatedFiles)
    {
        count++;
        FlowRun run = related.getRun();
        FlowExperiment experiment = run.getExperiment();

        %><tr class="<%=getShadeRowClass(count % 2 == 0)%>">
            <td style="white-space:nowrap"><a href="<%=h(related.urlShow())%>"><%=h(related.getLabel())%></a></td>
            <td style="white-space:nowrap"><a href="<%=h(run.urlShow())%>"><%=h(run.getLabel())%></a></td>
        <% if (experiment == null) { %>
            <td style="white-space:nowrap">&nbsp;</td>
        <% } else { %>
            <td style="white-space:nowrap"><a href="<%=h(experiment.urlShow())%>"><%=h(experiment.getLabel())%></a></td>
        <% } %>
        </tr><%

        List<FlowFCSAnalysis> analyses = related.getFCSAnalysisOutputs();
        for (FlowFCSAnalysis analysis : analyses)
        {
            count++;
            %><tr class="<%=getShadeRowClass(count % 2 == 0)%>">
                <td style="white-space:nowrap; padding-left:2em;"><a href="<%=h(analysis.urlShow())%>"><%=h(analysis.getLabel())%></a></td>
                <td style="white-space:nowrap"><a href="<%=h(run.urlShow())%>"><%=h(run.getLabel())%></a></td>
                <% if (experiment == null) { %>
                <td style="white-space:nowrap">&nbsp;</td>
                <% } else { %>
                <td style="white-space:nowrap"><a href="<%=h(experiment.urlShow())%>"><%=h(experiment.getLabel())%></a></td>
                <% } %>
            </tr><%
        }
    }
    %></table><%
}

LinkedHashMap<Integer, FlowFCSAnalysis> allAnalyses = new LinkedHashMap<>(10);
for (FlowFCSAnalysis analysis : well.getFCSAnalysisOutputs())
    allAnalyses.put(analysis.getRowId(), analysis);
if (originalFile != null)
{
    List<FlowFCSFile> originalRelatedFiles = originalFile.getFCSFileOutputs();
    for (FlowFCSFile originalRelatedFile : originalRelatedFiles)
        for (FlowFCSAnalysis analysis : originalRelatedFile.getFCSAnalysisOutputs())
            allAnalyses.put(analysis.getRowId(), analysis);
}
if (allAnalyses.size() > 0)
{
    %>
    <br/><br/>
    <h4>FCS Analyses performed on this file:</h4>
    <table class="labkey-data-region-legacy labkey-show-borders">
        <tr>
            <td class="labkey-column-header">Name</td>
            <td class="labkey-column-header">Run Name</td>
            <td class="labkey-column-header">Analysis Folder</td>
        </tr><%
    int count = 0;
    for (FlowWell analysis : allAnalyses.values())
    {
        count++;
        FlowRun run = analysis.getRun();
        FlowExperiment experiment = run.getExperiment();

        %><tr class="<%=getShadeRowClass(count % 2 == 0)%>">
            <td style="white-space:nowrap"><a href="<%=h(analysis.urlShow())%>"><%=h(analysis.getLabel())%></a></td>
            <td style="white-space:nowrap"><a href="<%=h(run.urlShow())%>"><%=h(run.getLabel())%></a></td>
        <% if (experiment == null) { %>
            <td style="white-space:nowrap">&nbsp;</td>
        <% } else { %>
            <td style="white-space:nowrap"><a href="<%=h(experiment.urlShow())%>"><%=h(experiment.getLabel())%></a></td>
        <% } %>
        </tr><%
    }
    %></table><br/><%
}

%><p><%

URI fileURI = well.getFCSURI();
if (null == fileURI)
{
    %>There is no file on disk for this well.<br><%
}
else
{
    PipeRoot r = PipelineService.get().findPipelineRoot(well.getContainer());
    boolean canReadFiles = canReadPipelineFiles(user, r);
    if (null != r && canReadFiles)
    {
        URI rel = URIUtil.relativize(r.getUri(), fileURI);
        if (null != rel)
        {
            File f = r.resolvePath(rel.getPath());
            if (f != null && f.canRead())
            {
                %><labkey:link href="<%=h(getWell().urlFor(WellController.ChooseGraphAction.class))%>" text="More Graphs" /><br><%
                %><labkey:link href="<%=h(getWell().urlFor(WellController.KeywordsAction.class))%>" text="Keywords from the FCS file" /><br><%
                %><labkey:link href="<%=h(getWell().urlDownload())%>" rel="nofollow" text="Download FCS file" /><br><%
            }
            else
            {
                %><div class="error">The original FCS file is no longer available or is not readable: <%=h(rel.getPath())%></div><%
            }
        }
    }
}

if (user != null && !user.isGuest() && well instanceof FlowFCSAnalysis)
{
    %><a class="labkey-text-link" href="<%=well.urlFor(RunController.ExportAnalysis.class).addParameter("selectionType", "wells")%>" rel="nofollow">Download Analysis zip</a><br><%
}

if (getRun() != null)
{
    %><labkey:link href="<%=PageFlowUtil.urlProvider(ExperimentUrls.class).getRunGraphDetailURL(getRun().getExperimentRun(), well.getData())%>" text="Experiment Run Graph Details" /><br><%
}

%></p><%

    DiscussionService service = DiscussionService.get();
    DiscussionService.DiscussionView discussion = service.getDiscussionArea(
            getViewContext(),
            well.getLSID(),
            well.urlShow(),
            "Discussion of " + well.getLabel(),
            false, true);
    include(discussion, out);
%>


<%!
// UNDONE: move to pipeline service
boolean canReadPipelineFiles(User user, PipeRoot root)
{
    // If this is a default pipeline root, then inherit permissions from parent container
    SecurityPolicy policy = SecurityPolicyManager.getPolicy(root, root.isDefault());
    return user != null && !user.isGuest() && policy.hasPermission(user, ReadPermission.class);
}
%>

