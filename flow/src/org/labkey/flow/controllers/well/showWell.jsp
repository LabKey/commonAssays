<%
/*
 * Copyright (c) 2006-2011 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.property.Domain" %>
<%@ page import="org.labkey.api.exp.property.DomainProperty" %>
<%@ page import="org.labkey.api.jsp.JspLoader" %>
<%@ page import="org.labkey.api.pipeline.PipeRoot" %>
<%@ page import="org.labkey.api.pipeline.PipelineService" %>
<%@ page import="org.labkey.api.security.SecurityManager" %>
<%@ page import="org.labkey.api.security.SecurityPolicy" %>
<%@ page import="org.labkey.api.security.permissions.ReadPermission" %>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission"%>
<%@ page import="org.labkey.api.settings.AppProps" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.util.Tuple3" %>
<%@ page import="org.labkey.api.util.URIUtil" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="org.labkey.api.webdav.WebdavService" %>
<%@ page import="org.labkey.flow.FlowPreference" %>
<%@ page import="org.labkey.flow.analysis.web.GraphSpec" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.flow.controllers.run.RunController" %>
<%@ page import="org.labkey.flow.controllers.well.WellController" %>
<%@ page import="org.labkey.flow.data.FlowCompensationMatrix" %>
<%@ page import="org.labkey.flow.data.FlowExperiment" %>
<%@ page import="org.labkey.flow.data.FlowFCSAnalysis" %>
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
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page extends="org.labkey.flow.controllers.well.WellController.Page" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>
<style type="text/css">
    .right {text-align:right;}
</style>
<script type="text/javascript" src="<%=AppProps.getInstance().getContextPath()%>/Flow/util.js"></script>
<script type="text/javascript">
LABKEY.requiresClientAPI(true);
LABKEY.requiresScript("TreeGrid.js");
Ext.QuickTips.init();
</script>
<%
    ViewContext context = HttpView.currentContext();
    FlowWell well = getWell();
    FlowWell fcsFile = well.getFCSFile();
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
    ArrayList<String> names = new ArrayList<String>();
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
            var col = {header:Ext.util.Format.htmlEncode(name), dataIndex:dataIndex, tpl: "{[values['" + dataIndex + "']]}", width:80, align:'right', renderer:renderer, stat:s.stat, param:s.param};
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
<table><%

if (getRun() == null) 
{
    %><tr><td colspan="2">The run has been deleted.</td></tr><%
}
else 
{
    %><tr><td>Run Name:</td><td><%=h(getRun().getName())%></td></tr><%
}
    %><tr><td>Well Name:</td><td><%=h(well.getName())%></td></tr><%

if (fcsFile != null && fcsFile != well)
{
    %><tr><td>FCS File:</td><td><a href="<%=h(fcsFile.urlShow())%>"><%=h(fcsFile.getName())%></a></td></tr><%
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
    %><tr><td><%=report.getDescriptor().getReportName()%> Report</td><td>&nbsp;</td><%
    for (DomainProperty dp : domain.getProperties())
    {
        String propertyURI = dp.getPropertyURI();
        if (properties.containsKey(propertyURI))
        {
            Object value = properties.get(propertyURI);
            %><tr><td>&nbsp;&nbsp;&nbsp;<%=dp.getName()%>:</td><td><%=String.valueOf(value)%></td></tr><%
        }
    }
    %></tr><%
}
%></table>
<%
    if (getContainer().hasPermission(getUser(), UpdatePermission.class))
    {
%><br><%=generateButton("edit", well.urlFor(WellController.EditWellAction.class))%><br>
<%
    }
%>

<p id="keywordsGrid" class="extContainer"></p>
<p id="statsTree" class="extContainer"></p>

<%
if (getGraphs().length > 0)
{
    final String graphSize = FlowPreference.graphSize.getValue(request);
    include(new JspView(JspLoader.createPage(GraphDataRegion.class, "setGraphSize.jsp")), out);
    for (GraphSpec graph : getGraphs())
    {
        %>
        <span style="display:inline-block; vertical-align:top; height:<%=graphSize%>px; width:<%=graphSize%>px;">
        <img style="width:<%=graphSize%>px; height:<%=graphSize%>px;" class='labkey-flow-graph' src="<%=h(getWell().urlFor(WellController.ShowGraphAction.class))%>&amp;graph=<%=u(graph.toString())%>" onerror="flowImgError(this);">
        </span><wbr>
        <%
    }
}

List<FlowWell> analyses = getWell().getFCSAnalyses();
if (analyses.size() > 0)
{
    %><table><tr><th colspan="3">Analyses performed on this file:</th></tr>
    <tr><th>FCS Analysis Name</th><th>Run Analysis Name</th><th>Analysis Name</th></tr><%
    for (FlowWell analysis : analyses)
    {
        FlowRun run = analysis.getRun();
        FlowExperiment experiment = run.getExperiment();

        %><tr><td><a href="<%=h(analysis.urlShow())%>"><%=h(analysis.getLabel())%></a></td>
        <td><%=h(run.getLabel())%></td>
        <td><%=experiment == null ? "" : h(experiment.getLabel())%></td>
        </tr><%
    }
    %></table><%
}

URI fileURI = well.getFCSURI();
if (null == fileURI)
{
    %><p>There is no file on disk for this well.</p><%
}
else
{
    PipeRoot r = PipelineService.get().findPipelineRoot(well.getContainer());
    if (null != r)
    {
        // UNDONE: PipeRoot should have wrapper for this
        //NOTE: we are specifically not inheriting policies from the parent container
        //as the old permissions-checking code did not do this. We need to consider
        //whether the pipeline root's parent really is the container, or if we should
        //be checking a different (more specific) permission.
        SecurityPolicy policy = SecurityManager.getPolicy(r, false);
        if (policy.hasPermission(context.getUser(), ReadPermission.class))
        {
            URI rel = URIUtil.relativize(r.getUri(), fileURI);
            if (null != rel)
            {
                File f = r.resolvePath(rel.getPath());
                if (f != null && f.canRead())
                {
                    %><p><a href="<%=h(getWell().urlFor(WellController.ChooseGraphAction.class))%>">More Graphs</a><br><%
                    %><a href="<%=h(getWell().urlFor(WellController.KeywordsAction.class))%>">Keywords from the FCS file</a><br><%
                    String url = context.getContextPath() + "/" + WebdavService.getServletPath() + r.getContainer().getPath() + "/@pipeline/" + rel.toString();
                    %><a href="<%=h(url)%>">Download FCS file</a><br><%
                }
                else
                {
                    %><div class="error">The original FCS file is no longer available or is not readable: <%=rel.getPath()%></div><%
                }
            }
        }
    }

    if (well instanceof FlowFCSAnalysis)
    {
        %><a href="<%=well.urlFor(RunController.ExportAnalysis.class)%>">Download Analysis zip</a><br><%
    }
    %></p><%
}
    DiscussionService.Service service = DiscussionService.get();
    DiscussionService.DiscussionView discussion = service.getDisussionArea(
            getViewContext(),
            well.getLSID(),
            well.urlShow(),
            "Discussion of " + well.getLabel(),
            false, true);
    include(discussion, out);
%>
