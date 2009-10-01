<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.query.AttributeCache" %>
<%@ page import="org.labkey.flow.query.FlowPropertySet" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="java.util.ArrayList" %>
<%
    ViewContext context = HttpView.currentContext();
    FlowPropertySet fps = new FlowPropertySet(context.getContainer());
    
    StringBuilder jsonStats = new StringBuilder();
    jsonStats.append("[");
    String comma = "";
    for (StatisticSpec spec : fps.getStatistics().keySet())
    {
        jsonStats.append(comma);
        jsonStats.append("{");
        jsonStats.append("text:").append(PageFlowUtil.jsString(spec.toString())).append(",");
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
        jsonStats.append("}");
        comma = ",\n";
    }
    jsonStats.append("]");
%>
<script type="text/javascript">

function statisticsTree(statistics)
{
    var s, node, subset;
    var map = {};
    for (var i=0 ; i<statistics.length ; i++)
    {
        s = statistics[i];
        node = map[s.subset];
        if (!node)
        {
            var text = s.subset;
            if (s.parent && 0==text.indexOf(s.parent+"/"))
                text = text.substring(s.parent.length+1);
            if (0==text.indexOf("(") && text.length-1 == text.lastIndexOf(")"))
                text = text.substring(1,text.length-2);
            node = new Ext.tree.TreeNode(Ext.apply({},{text:text, qtipCfg:{text:s.subset}, expanded:true, uiProvider:Ext.tree.ColumnNodeUI, parentNode:null}, s));    // stash original object in data
            node.attributes.stats = [];
            map[s.subset] = node;
        }
        var name = s.stat;
        if (s.param)
            name = name + "(" + s.param + ")";
        node.attributes.stats.push(name);
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
        if (!node.attributes.parentNode && (node.childNodes.length > 0 /* || node.attributes.stats.length > 0 */))
            treeData.push(node);
    }
    return treeData;
}

var StatisticField = Ext.extend(Ext.form.TriggerField,
{
    onTriggerClick : function()
    {
        if (this.disabled)
        {
            return;
        }
        if (this.popup == null)
        {
            var tree = new Ext.tree.TreePanel({
                cls:'extContainer',
                rootVisible:false,
                useArrows:true,
                autoScroll:false,
                containerScroll:true,
//                width:800, height:400,
//                autoHeight:true,
                animate:true,
                enableDD:false
            });
            var root = new Ext.tree.TreeNode({text:'-', expanded:true});
            for (var i=0 ; i<_statsTreeData.length ; i++)
                root.appendChild(_statsTreeData[i]);
            tree.setRootNode(root);
            var sm = tree.getSelectionModel();
            sm.on("selectionchange", function(sm,curr,prev){
                var subset = curr.attributes.subset;
                var stats = curr.attributes.stats;
                if (!stats || !stats.length) return;
                var items = [];
                for (var i=0 ; i<stats.length ; i++)
                    items.push({text:stats[i]});
                var statMenu = new Ext.menu.Menu({
                    width:240,
                    cls:'extContainer',
                    items: items
                });
                statMenu.on("itemclick",function(mi,e){
                    var stat = mi.text=="%P"?"Freq_Of_Parent":mi.text;
                    statMenu.destroy();
                    this.pickValue(subset + ":" + stat);
                }, this);
                statMenu.show(curr.getUI().getTextEl());    
                //this.pickValue(curr.attributes.subset);
            }, this);

            this.popup = new Ext.Window({
                autoScroll:true,
                closeAction:'hide',
                closable:true,
                constrain:true,
                items:[tree],
                title:'Statistic Picker',
                width:800, height:400
            });
        }
        this.popup.show();
        this.popup.center();
    },

    pickValue : function(value)
    {
        if (this.popup) this.popup.hide();
        this.setValue(value);
    }

});


var _statistics = <%=jsonStats%>;
var _statsTreeData = statisticsTree(_statistics);
Ext.reg('statisticField', StatisticField);

</script>
