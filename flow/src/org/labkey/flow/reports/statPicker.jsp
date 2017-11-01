<%
/*
 * Copyright (c) 2009-2017 LabKey Corporation
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
<%@ page import="org.labkey.api.exp.api.ExpSampleSet" %>
<%@ page import="org.labkey.api.exp.property.DomainProperty" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.flow.persist.AttributeCache" %>
<%@ page import="org.labkey.flow.query.FlowPropertySet" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("Ext3");
    }
%>
<%
    FlowPropertySet fps = new FlowPropertySet(getContainer());
    
    StringBuilder jsonStats = new StringBuilder();
    jsonStats.append("[");
    String comma = "";
    for (AttributeCache.StatisticEntry entry : fps.getStatistics())
    {
        StatisticSpec spec = entry.getAttribute();
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
        jsonStats.append("stat:").append(PageFlowUtil.jsString(spec.getStatistic().name())).append(",");
        jsonStats.append("param:").append(PageFlowUtil.jsString(spec.getParameter())).append(",");
        jsonStats.append("}");
        comma = ",\n";
    }
    jsonStats.append("]");

    StringBuilder jsonSamples = new StringBuilder();
    jsonSamples.append("[");
    List<String> sampleSetProperties = new ArrayList<>();
    FlowProtocol protocol = FlowProtocol.ensureForContainer(getUser(), getContainer());
    if (protocol != null)
    {
        ExpSampleSet sampleSet = protocol.getSampleSet();
        if (sampleSet != null)
        {
            for (DomainProperty dp : sampleSet.getType().getProperties())
                sampleSetProperties.add(dp.getName());
        }
    }

    StringBuilder stats = new StringBuilder();
    stats.append("{");
    comma = "";
    for (StatisticSpec.STAT stat : StatisticSpec.STAT.values())
    {
        if (stat == StatisticSpec.STAT.Spill)
            continue;
        stats.append(comma);
        stats.append("\"").append(stat.name()).append("\": {");
        stats.append("  name: \"").append(stat.name()).append("\"");
        stats.append(", shortName: \"").append(stat.getShortName()).append("\"");
        stats.append(", longName: \"").append(stat.getLongName()).append("\"");
        stats.append("}");

        comma = ",\n";
    }
    stats.append("}");

    StringBuilder ops = new StringBuilder();
    ops.append("[");
    comma = "";
    for (CompareType ct : new CompareType[] { CompareType.EQUAL, CompareType.NEQ_OR_NULL, CompareType.ISBLANK, CompareType.NONBLANK, CompareType.GT, CompareType.LT, CompareType.GTE, CompareType.LTE, CompareType.CONTAINS, CompareType.STARTS_WITH, CompareType.DOES_NOT_CONTAIN, CompareType.DOES_NOT_START_WITH, CompareType.IN })
    {
        ops.append(comma);
        ops.append("[\"").append(ct.getPreferredUrlKey()).append("\", \"").append(ct.getDisplayValue()).append("\"]");
        comma = ",\n";
    }
    ops.append("]");
%>
<script type="text/javascript">
Ext.QuickTips.init();

// Adds a 'subset' attribute used by the test framework
var StatTreeNodeUI = Ext.extend(Ext.tree.TreeNodeUI, {
    renderElements : function () {
        StatTreeNodeUI.superclass.renderElements.apply(this, arguments);
        var node = this.node;
        var subset = node.attributes.subset;
        this.elNode.setAttribute("subset", subset);
    }
});

function statisticsTree(statistics)
{
    var enc = Ext.util.Format.htmlEncode;
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
            node = new Ext.tree.TreeNode(Ext.apply({},{text:enc(text), qtipCfg:{text:enc(s.subset)}, expanded:true, uiProvider:StatTreeNodeUI, parentNode:null}, s));    // stash original object in data
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

var StatCombo = Ext.extend(Ext.form.ComboBox,
{
    constructor : function (config)
    {
        config.mode = 'local';
        config.store = new Ext.data.JsonStore({
            root: 'stats',
            idProperty: 'name',
            fields: ['name', 'shortName', 'longName']
        });
        config.valueField='name';
        config.displayField='longName';
        config.forceSelection=true;
        config.triggerAction='all';
        config.listWidth='230px';

        StatCombo.superclass.constructor.call(this, config);
    }
});
Ext.reg('statcombo', StatCombo);

var SubsetField = Ext.extend(Ext.form.TriggerField,
{
    initComponent : function ()
    {
        this.width = 350;
        this.addEvents('selectionchange');
        SubsetField.superclass.initComponent.call(this);
    },

    onTriggerClick : function()
    {
        if (this.disabled)
        {
            return;
        }
        if (this.popup == null)
            this.createPopup();
        this.popup.show();
        this.popup.center();
    },

    createPopup : function ()
    {
        this.tree = new Ext.tree.TreePanel({
            cls:'extContainer',
            rootVisible:false,
            useArrows:true,
            autoScroll:false,
            containerScroll:true,
            animate:true,
            enableDD:false
        });
        var root = new Ext.tree.TreeNode({text:'-', expanded:true});
        var treeData = statisticsTree(FlowPropertySet.statistics);
        for (var i=0 ; i < treeData.length ; i++)
            root.appendChild(treeData[i]);
        this.tree.setRootNode(root);
        var sm = this.tree.getSelectionModel();
        this.relayEvents(sm, ["selectionchange"]);
        sm.on("selectionchange", function (sm, curr, prev) {
            var subset = curr.attributes.subset;
            this.pickValue(subset);
        }, this);
        this.popup = new Ext.Window({
            autoScroll:true,
            closeAction:'hide',
            closable:true,
            constrain:true,
            items:[this.tree],
            title:'Statistic Picker',
            width:800, height:400
        });
    },

    pickValue : function(value)
    {
        if (this.popup) this.popup.hide();
        this.setValue(value);
    },

    getSelectedNode : function ()
    {
        if (!this.tree)
            return undefined;

        return this.tree.getSelectionModel().getSelectedNode();
    }

});
Ext.reg('subsetField', SubsetField);

// Composite of SubsetField and Stat ComboBox
var StatisticField = Ext.extend(Ext.form.CompositeField,
{
    constructor : function (config)
    {
        config.labelStyle = 'font-weight: normal;',

        config.items = [{
            xtype: 'subsetField',
            name: config.name + "_subset",
            ref: 'subsetField',
            listeners: {
                selectionchange: { fn: this.subsetChanged, scope: this },
                change: { fn: this.subsetChanged, scope: this },
                blur: { fn: this.subsetChanged, scope: this }
            }
        },{
            xtype: 'statcombo',
            name: config.name + "_stat",
            ref: 'statCombo',
            listeners: {
                selectionchange: { fn: this.statChanged, scope: this },
                change: { fn: this.statChanged, scope: this },
                blur: { fn: this.statChanged, scope: this }
            }
        },{
            xtype: 'hidden',
            name: config.name,
            ref: 'hiddenField'
        }];

        StatisticField.superclass.constructor.call(this, config);
    },

    subsetChanged : function ()
    {
        var node = this.subsetField.getSelectedNode();
        var stats = node.attributes.stats;
        if (stats && stats.length > 0)
        {
            this.statCombo.getStore().removeAll();
            var storeData = createStatStore(stats);
            this.statCombo.getStore().loadData(storeData);
            this.statCombo.focus();
        }
        else
        {
            this.statCombo.getStore().removeAll();
            //this.statCombo.disable();
        }
        this.updateHiddenValue();
    },

    statChanged : function ()
    {
        this.updateHiddenValue();
    },

    setValue : function (value)
    {
        if (value && value.length > 0)
        {
            var idxColon = value.lastIndexOf(":");
            var population = value.substring(0, idxColon);
            var stat = value.substring(idxColon+1);

            this.subsetField.setValue(population);

            var stats = [];
            for (var i = 0; i < FlowPropertySet.statistics.length; i++)
            {
                var s = FlowPropertySet.statistics[i];
                if (s.subset == population)
                    stats.push(s.stat);
            }

            var storeData = createStatStore(stats);
            this.statCombo.getStore().loadData(storeData);

            // Find the statistic by the StatisticSpec enum name, but fallback to the shortName for reports that were created incorrectly.
            var index = this.statCombo.getStore().find('name', stat);
            if (index == -1)
                index = this.statCombo.getStore().find('shortName', stat);
            if (index > -1) {
                var record = this.statCombo.getStore().getAt(index);
                this.statCombo.setValue(record.id);
            }

            //this.statCombo.setDisabled(!(stat && stat.length > 0));
        }
        this.updateHiddenValue();
    },

    getValue : function ()
    {
        return this.updateHiddenValue();
    },

    updateHiddenValue : function ()
    {
        var subset = this.subsetField.getValue();
        var stat = this.statCombo.getValue();
        var result = "";
        if (subset && stat)
            result = subset + ":" + stat;
        this.hiddenField.setValue(result);
        return result;
    }

});
Ext.reg('statisticField', StatisticField);

var OpCombo = Ext.extend(Ext.form.ComboBox, {
    constructor : function (config)
     {
        config.mode = 'local';
        config.store = <%=ops%>

        OpCombo.superclass.constructor.call(this, config);
    }
});
Ext.reg('opCombo', OpCombo);

var FlowStatistics = <%=stats%>;

function createStatStore(stats)
{
    var items = [];

    for (var i = 0, l=stats.length; i < l; i++)
    {
        var stat = stats[i];
        var item = FlowStatistics[stat];
        if (item)
            items.push(item);
    }

    return {stats: items};
}

var FlowPropertySet = {};
FlowPropertySet.keywords = [<%
    comma = "";
    for (String s : fps.getVisibleKeywords())
    {
        %><%=text(comma)%><%=PageFlowUtil.jsString(s)%><%
        comma=",";
    }
%>];
FlowPropertySet.statistics = <%=jsonStats%>;

var SampleSet = {};
SampleSet.properties = [<%
    comma = "";
    for (String s : sampleSetProperties)
    {
        %><%=text(comma)%><%=PageFlowUtil.jsString(s)%><%
        comma=",";
    }
%>];

</script>
