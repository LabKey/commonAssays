<%
/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.flow.analysis.web.StatisticSpec" %>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.flow.query.AttributeCache" %>
<%@ page import="org.labkey.flow.query.FlowPropertySet" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.api.view.ViewContext" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.labkey.flow.data.FlowProtocol" %>
<%@ page import="org.labkey.api.exp.api.ExpSampleSet" %>
<%@ page import="java.util.List" %>
<%@ page import="org.labkey.api.exp.property.DomainProperty" %>
<%@ page import="org.labkey.api.data.CompareType" %>
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

    StringBuilder jsonSamples = new StringBuilder();
    jsonSamples.append("[");
    List<String> sampleSetProperties = new ArrayList<>();
    FlowProtocol protocol = FlowProtocol.ensureForContainer(context.getUser(), context.getContainer());
    if (protocol != null)
    {
        ExpSampleSet sampleSet = protocol.getSampleSet();
        if (sampleSet != null)
        {
            for (DomainProperty dp : sampleSet.getPropertiesForType())
                sampleSetProperties.add(dp.getName());
        }
    }

    StringBuilder stats = new StringBuilder();
    stats.append("[");
    comma = "";
    for (StatisticSpec.STAT stat : StatisticSpec.STAT.values())
    {
        if (stat == StatisticSpec.STAT.Spill)
            continue;
        stats.append(comma);
        stats.append("[\"").append(stat.getShortName()).append("\", \"").append(stat.getLongName()).append("\"]");

        comma = ",\n";
    }
    stats.append("]");

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
        config.store = <%=stats%>

        StatCombo.superclass.constructor.call(this, config);
    },

    filterStats : function (stats)
    {
        if (stats && stats.length > 0)
        {
            if (stats.length == 1)
            {
                this.getStore().filter("field1", stats[0]);
                this.setValue(stats[0]);
            }
            else
            {
                var options = [];
                for (var i=0 ; i<stats.length ; i++)
                    options.push({ property: "field1", value: stats[i] });
                var filterFn = this.createOrFilter(options);
                this.getStore().filterBy(filterFn);
            }
        }
        else
        {
            this.clearValue();
            this.getStore().clearFilter();
        }
    },

    createFilterFns : function (options)
    {
        var filters = [];
        for (var i = 0; i < options.length; i++)
        {
            var option = options[i],
                    func   = options.fn,
                    scope  = options.scope || this;

            if (!Ext.isFunction(func)) {
                func = this.getStore().createFilterFn(option.property, option.value, option.anyMatch, option.caseSensitive, option.exactMatch);
            }

            filters.push({fn: func, scope: scope});
        }

        return filters;
    },

    createOrFilter : function (options)
    {
        var filters = this.createFilterFns(options);
        return function (record) {
            for (var j = 0; j < filters.length; j++)
            {
                var filter = filters[j],
                    fn     = filter.fn,
                    scope  = filter.scope;

                var isMatch = fn.call(scope, record);
                if (isMatch)
                    return true;
            }

            return false;
        };
    }
});

var SubsetField = Ext.extend(Ext.form.TriggerField,
{
    initComponent : function ()
    {
        this.width = 400;
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
            //width:800, height:400,
            //autoHeight:true,
            animate:true,
            enableDD:false
        });
        var root = new Ext.tree.TreeNode({text:'-', expanded:true});
        for (var i=0 ; i<FlowPropertySet.statsTreeData.length ; i++)
            root.appendChild(FlowPropertySet.statsTreeData[i]);
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
        this.hiddenField = new Ext.form.Hidden({name: config.name});

        this.subsetField = new SubsetField({name: config.name + "_subset"});
        this.subsetField.on("selectionchange", this.subsetChanged, this);
        this.subsetField.on("change", this.subsetChanged, this);
        this.subsetField.on("blur", this.subsetChanged, this);

        this.statCombo = new Ext.form.ComboBox({name: config.name + "_stat", store: []});
        this.statCombo.on("selectionchange", this.statChanged, this);
        this.statCombo.on("change", this.statChanged, this);
        this.statCombo.on("blur", this.statChanged, this);

        this.items = [ this.subsetField, this.statCombo, this.hiddenField ];

        StatisticField.superclass.constructor.call(this, config);
    },

    subsetChanged : function ()
    {
        var node = this.subsetField.getSelectedNode();
        var stats = node.attributes.stats;
        if (stats && stats.length > 0)
        {
            this.statCombo.getStore().removeAll();
            this.statCombo.getStore().loadData(stats);
            //this.statCombo.enable();
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
            this.statCombo.getStore().loadData(stats);
            this.statCombo.setValue(stat);
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

var FlowPropertySet = {};
FlowPropertySet.keywords = [<%
    comma = "";
    for (String s : fps.getVisibleKeywords())
    {
        %><%=comma%><%=PageFlowUtil.jsString(s)%><%
        comma=",";
    }
%>];
FlowPropertySet.statistics = <%=jsonStats%>;
FlowPropertySet.statsTreeData = statisticsTree(FlowPropertySet.statistics);

var SampleSet = {};
SampleSet.properties = [<%
    comma = "";
    for (String s : sampleSetProperties)
    {
        %><%=comma%><%=PageFlowUtil.jsString(s)%><%
        comma=",";
    }
%>];

</script>
