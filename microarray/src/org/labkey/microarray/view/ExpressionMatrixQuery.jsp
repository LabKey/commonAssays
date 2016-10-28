<%
/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("microarray");
    }
%>
<div id="queryUI"></div>
<div id='reportDiv'></div>

<script type="text/javascript">
    var nodes = {
        "Experimental Organism Strain Details":[{"text":"strain name"}],
        "Coinfection":[{"text":"HIV Test Conducted"},{"text":"HIV Status"}],
        "Clinical and physical identification":[{"text":"CD4 lymphocyte/T Helper Absolute Count (synonym T-helper cells)"},{"text":"Interferon Gamma Release Assay Result"},{"text":"Type of Gamma Release Assay for Tuberculosis"}],
        "AFB Staining":[{"text":"AFB Result"},{"text":"AFB Sample"}],
        "Infection":[{"text":"Site of Tuberculosis"},{"text":"Site of Tuberculosis Details"}],
        "Experimental Infection":[{"text":"Route of infection"},{"text":"Dose/strength used for infection"}],
        "Identification":[{"text":"Tuberculosis Signs/Symptoms"},{"text":"Medical history predisposing to Tuberculosis"},{"text":"Underlying Pulmonary Disease"},{"text":"Type of Identification Performed on Culture Growth"},{"text":"Lesion type"},{"text":"Granuloma histopathology"}],
        "Treatment":[{"text":"Tuberculosis Treatment Status"},{"text":"Tuberculosis Treatment Classification"},{"text":"Dosage (i)"},{"text":"Tuberculosis Drug Frequency (i)"},{"text":"Treatment duration (i)"},{"text":"Treatment initiation (i)"},{"text":"Tuberculosis Drug (i)"},{"text":"Tuberculosis Treatment Failure Reason (i)"},{"text":"Tuberculosis Disease Outcomes Clinical"},{"text":"Did the Subject Complete Treatment for Tuberculosis?"},{"text":"End date of treatment (i)"},{"text":"Start date of treatment (i)"}],
        "Experimental or Clinical System Organisms":[{"text":"Mycobacterium Species"},{"text":"Mycobacterium strain"},{"text":"Detailed Ethnicity CDC Concept Code"},{"text":"Detailed Ethnicity CDC Preferred Concept Name"},{"text":"Country of Orgin"},{"text":"Date of sample collection"},{"text":"Host sex"},{"text":"Host age"},{"text":"Host species"}],
        "Clinical and Physical Identification":[{"text":"Tuberculin Skin Test (TST) Result"},{"text":"Interpretation of Tuberculin Skin Test (TST)"}],
        "Experimental samples and techniques":[{"text":"Sample component measured"},{"text":"Assay Technology"},{"text":"Assay Vendor"},{"text":"Assay Platform"},{"text":"Cell line"},{"text":"Host sample cells isolated"},{"text":"Host sample cells treatment"},{"text":"Host sample tissue"},{"text":"Experimental Reactivation"},{"text":"Other Experimental Treatment"}],
        "Immunization":[{"text":"BCG Vaccination Status"}],
        "Media":[{"text":"Culture Medium"},{"text":"Identification Tests Performed on Culture Growth"},{"text":"Method for Documentation Confirming Disease Outcome Microbilogic"}],
        "Imaging":[{"text":"Imaging Results (i)"},{"text":"Imaging Technology (i)"},{"text":"Imaging Time Point (i)"},{"text":"Image Interpretation (i)"}],
        "Disease Condition":[{"text":"Identification of Subject Tuberculosis Status"},{"text":"Tuberculosis Treatment Episode"},{"text":"Relapse"},{"text":"Previous Treatment for Latent Tuberculosis"},{"text":"Latent Tuberculosis infection Status"},{"text":"Multi Drug-Resistant Tuberculosis Status"},{"text":"Extensively Drug Resistant Tuberculosis Status"},{"text":"Relapsed"}],
        "Experimental Samples and Techniques":[{"text":"Dataset name"},{"text":"Sample name"},{"text":"Sample id", "name": "SampleID"}]};

    var fillTreeStore = function(treeStore, nodes){
        var rootNode = treeStore.getRootNode();
        for (var categoryName in nodes){
            var node = rootNode.appendChild({
                name: categoryName,
                text: categoryName,
                expanded: false
            });

            for (var i = 0; i < nodes[categoryName].length; i++) {
                node.appendChild({
                    name: nodes[categoryName][i].name,
                    text: nodes[categoryName][i].text,
                    expanded: false,
                    leaf: true
                });
            }
        }
    };

    function onLoad(response)
    {
        var moduleProperties = Ext4.decode(response.responseText);
        var assayDesignName = moduleProperties.values.AssayDesignName.effectiveValue;
        if (!assayDesignName)
        {
            Ext4.Msg.alert("Error", "Missing AssayDesignName in Microarray module properties. Please set " +
                    "a value in the Folder Management UI.")
        }
        var reportId = moduleProperties.values.ComparisonReportId.effectiveValue;
        if (!reportId)
        {
            // The ComparisonReportId is the id of the R report to be executed when "Compare" is clicked.
            // The id should look like db:3047
            Ext4.Msg.alert("Error", "Missing ComparisonReportId in Microarray module properties. Please " +
                    "set a value in the Folder Management UI.")
        }

        Ext4.define('TBDCC.FieldTreeModel', {
            extend: 'Ext.data.Model',
            fields: [
                {name: 'name', type: 'string'},
                {name: 'text', type: 'string'},
                {name: 'expanded', type: 'boolean'}
            ]
        });

        Ext4.define('TBDCC.FieldModel', {
            extend: 'Ext.data.Model',
            fields: [{name: 'name', type: 'string'}, {name: 'label', type: 'string'}]
        });

        var fieldTreeStore = Ext4.create('Ext.data.TreeStore', {
            autoLoad: false,
            model: 'TBDCC.FieldTreeModel',
            proxy: {
                type: 'memory',
                reader: {type:'json'}
            },
            root: {
                id: '1',
                name: 'Ontology Fields',
                nodeType: 'async',
                expanded: true
            }
        });

        var fieldValuesStore = Ext4.create('Ext.data.Store', {
            model: 'TBDCC.FieldModel',
            proxy: {
                type: 'memory',
                reader: {type:'json'}
            },
            autoload: false
        });

        Ext4.define('TreeFilter', {
            extend: 'Ext.AbstractPlugin',
            alias: 'plugin.jsltreefilter',

            collapseOnClear: true,       // collapse all nodes when clearing/resetting the filter
            allowParentFolders: false,   // allow nodes not designated as 'leaf' (and their child items) to  be matched by the filter

            init: function (tree) {
                var me = this;
                me.tree = tree;

                tree.filter = Ext4.Function.bind(me.filter, me);
                tree.filterBy = Ext4.Function.bind(me.filterBy, me);
                tree.clearFilter = Ext4.Function.bind(me.clearFilter, me);
            },

            filter: function (value, propArray, re) {

                if (Ext4.isEmpty(value)) {               // if the search field is empty
                    this.clearFilter();
                    return;
                }

                var me = this,
                    tree = me.tree,
                    matches = [],                         // array of nodes matching the search criteria
                    root = tree.getRootNode(),            // root node of the tree
                    _propArray = propArray || ['text'],   // propArray is optional - will be set to the ['text'] property of the TreeStore record by default
                    _re = re || new RegExp(value, "ig"),  // the regExp could be modified to allow for case-sensitive, starts  with, etc.
                    visibleNodes = [],                    // array of nodes matching the search criteria + each parent non-leaf  node up to root
                    viewNode;

                tree.expandAll();                       // expand all nodes for the the following iterative routines

                // iterate over all nodes in the tree in order to evalute them against the search criteria
                root.cascadeBy(function (node) {
                    for (var p=0; p < _propArray.length; p++) {
                        if (node.get(_propArray[p]) &&
                            node.get(_propArray[p]).match &&
                            node.get(_propArray[p]).match(_re)) {  // if the node matches the search criteria and is a leaf (could be  modified to searh non-leaf nodes)
                                matches.push(node);                // add the node to the matches array
                                return;
                        }
                    }
                });

                if (me.allowParentFolders === false) {     // if me.allowParentFolders is false (default) then remove any  non-leaf nodes from the regex match
                    Ext4.each(matches, function (match) {
                        if (match && !match.isLeaf()) { Ext4.Array.remove(matches, match); }
                    });
                }

                Ext4.each(matches, function (item, i, arr) {   // loop through all matching leaf nodes
                    root.cascadeBy(function (node) {           // find each parent node containing the node from the matches array
                        if (node.contains(item)) {
                            visibleNodes.push(node);           // if it's an ancestor of the evaluated node add it to the visibleNodes  array
                        }
                    });
                    if (me.allowParentFolders === true &&  !item.isLeaf()) { // if me.allowParentFolders is true and the item is  a non-leaf item
                        item.cascadeBy(function (node) {                     // iterate over its children and set them as visible
                            visibleNodes.push(node)
                        });
                    }
                    visibleNodes.push(item);   // also add the evaluated node itself to the visibleNodes array
                });

                root.cascadeBy(function (node) {                             // finally loop to hide/show each node
                    viewNode = Ext4.fly(tree.getView().getNode(node));       // get the dom element assocaited with each node
                    if (viewNode) {                                          // the first one is undefined ? escape it with a conditional
                        viewNode.setVisibilityMode(Ext4.Element.DISPLAY);    // set the visibility mode of the dom node to display (vs offsets)
                        viewNode.setVisible(Ext4.Array.contains(visibleNodes, node));
                    }
                });
            },

            filterBy: function(fn, scope) {
                if (!Ext4.isFunction(fn)) {
                    return;
                }

                var me = this,
                    tree = me.tree,
                    root = tree.getRootNode(),
                    matches = [],
                    viewNode,
                    visibleNodes = [];

                tree.expandAll();                       // expand all nodes for the the following iterative routines

                root.cascadeBy(function (node) {
                    if (fn.call(scope || this, node) === true) { matches.push(node); }
                });

                if (me.allowParentFolders === false) {     // if me.allowParentFolders is false (default) then remove any  non-leaf nodes from the regex match
                    Ext4.each(matches, function (match) {
                        if (match && !match.isLeaf()) { Ext4.Array.remove(matches, match); }
                    });
                }

                Ext4.each(matches, function (item, i, arr) {   // loop through all matching leaf nodes
                    root.cascadeBy(function (node) {           // find each parent node containing the node from the matches array
                        if (node.contains(item)) {
                            visibleNodes.push(node);           // if it's an ancestor of the evaluated node add it to the visibleNodes  array
                        }
                    });
                    if (me.allowParentFolders === true &&  !item.isLeaf()) { // if me.allowParentFolders is true and the item is  a non-leaf item
                        item.cascadeBy(function (node) {                     // iterate over its children and set them as visible
                            visibleNodes.push(node)
                        });
                    }
                    visibleNodes.push(item);   // also add the evaluated node itself to the visibleNodes array
                });

                root.cascadeBy(function (node) {                             // finally loop to hide/show each node
                    viewNode = Ext4.fly(tree.getView().getNode(node));       // get the dom element assocaited with each node
                    if (viewNode) {                                          // the first one is undefined ? escape it with a conditional
                        viewNode.setVisibilityMode(Ext4.Element.DISPLAY);    // set the visibility mode of the dom node to display (vs offsets)
                        viewNode.setVisible(Ext4.Array.contains(visibleNodes, node));
                    }
                });
            },

            clearFilter: function () {
                var me = this,
                    tree = this.tree,
                    root = tree.getRootNode();

                if (me.collapseOnClear) { tree.collapseAll(); }              // collapse the tree nodes
                root.cascadeBy(function (node) {                             // final loop to hide/show each node
                    var viewNode = Ext4.fly(tree.getView().getNode(node));   // get the dom element assocaited with each node
                    if (viewNode) {                                          // the first one is undefined ? escape it with a conditional and show  all nodes
                        viewNode.show();
                    }
                });
            }
        });

        fillTreeStore(fieldTreeStore, nodes);

        var ontologyFieldTreePanel = {
            xtype: 'treepanel',
            store: fieldTreeStore,
            flex: 1,
            id: 'ontologyFieldTreePanel',
            width: 290,
            height: 473,
            margin: '0 5 0 0',
            rootVisible: false,
            autoScroll: true,
            plugins : [ Ext4.create('TreeFilter', { collapseOnClear : false, allowParentFolders: true }) ],
            listeners: {
                scope: this,
                select: function(treePanel, selectedNode) {
                    if (selectedNode.isLeaf()) {
                        var fieldName = selectedNode.data.name ? selectedNode.data.name : selectedNode.data.text;
                        LABKEY.Query.executeSql({
                                schemaName: 'Samples',
                                sql: 'SELECT X AS name, X as label FROM (SELECT DISTINCT "' + fieldName + '" AS X FROM ExpressionMatrixSamples) AS Y',
                                success: function (response) {
                                    fieldValuesStore.loadRawData(response.rows);
                                },
                                failure: function (response) {
                                    // Column isnt found, clear the values.
                                    fieldValuesStore.loadRawData([]);
                                },
                                scope: this
                        });
                    } else {
                        // We want to clear any values and selections from the fieldValueStore if the user
                        // navigates away.
                        fieldValuesStore.loadRawData([]);
                    }
                }
            }
        };

        var valuesPanelConfig = {
            xtype: 'grid',
            flex: 1,
            height: 500,
            store: fieldValuesStore,
            margin: '0 0 0 5',
            columns: [{text: 'Values', dataIndex: 'label', flex: 1}],
            selModel: {
                mode: 'MULTI'
            },
            viewConfig : {
                emptyText : 'No data available for selected field'
            },
            multiSelect: true
        };

        var valuesPanel = Ext4.create('Ext.grid.Panel', valuesPanelConfig);

        var updateCompareEnabled = function() {
            if (Ext4.ComponentManager.get('genes').getValue() == '') {
                Ext4.ComponentManager.get('compareButton').setDisabled(true);
                return;
            }

            for (var i = 0; i < rightContainer.items.length; i++) {
                var panel = rightContainer.items.getAt(i).items.getAt(0);

                if (panel.comparisonStore.getCount() == 0 || panel.baselineStore.getCount() == 0) {
                    Ext4.ComponentManager.get('compareButton').setDisabled(true);
                    return;
                }
            }

            Ext4.ComponentManager.get('compareButton').setDisabled(false);
        };

        var updateTreeFilter = function() {
            var filter = function(rec) {
                return !(rec.data.text && rec.data.text.toLowerCase().indexOf(Ext4.ComponentManager.get('treeSearch').getValue().toLowerCase()) == -1);
            };

            var treePanel = Ext4.ComponentManager.get('ontologyFieldTreePanel');
            treePanel.clearFilter();
            treePanel.filterBy(filter, this);
        };

        var treeContainer = {
            xtype: 'panel',
            border: false,
            flex: 1,
            layout: {
                align: 'center'
            },
            items: [{
                xtype: 'textfield',
                name: 'treeSearch',
                id: 'treeSearch',
                width: 290,
                emptyText: 'Search Fields...',
                listeners: {
                    scope: this,
                    change : updateTreeFilter
                }
            }, ontologyFieldTreePanel]
        };

        var leftContainer = {
            xtype: 'panel',
            border: false,
            flex: 1,
            minWidth: 500,
            height: 500,
            layout: {
                type: 'hbox',
                align: 'center'
            },
            items: [treeContainer, valuesPanel]
        };

        var mergeValues = function(recordValues, values) {
            for (var i = 0; i < values.length; i++) {
                if(recordValues.indexOf(values[i]) == -1) {
                    recordValues.push(values[i])
                }
            }

            return recordValues;
        };

        var removeValues = function(recordValues, values) {
            for (var i = 0; i < values.length; i++) {
                var idx = recordValues.indexOf(values[i]);
                if(idx != -1) {
                    recordValues.splice(idx, 1);
                }
            }

            return recordValues;
        };

        function addCriteria(containerStore, type) {
            var field =  Ext4.getCmp('ontologyFieldTreePanel').getSelectionModel().getSelection()[0];
            if(field) {
                var fieldName = field.data.name = field.data.name ? field.data.name : field.data.text;
                var fieldText = field.data.text;
                var firstRecordIndex = containerStore.find('name', fieldName, 0, false, true, true);
                var selection = valuesPanel.getSelectionModel().getSelection();
                var values = [];

                if(selection.length == 0) {
                    return;
                }

                for (var i = 0; i < selection.length; i++) {
                    values.push(selection[i].data.name);
                }

                if (firstRecordIndex != -1) {
                    // There can be up to two records per name since there are two types of filters.
                    var secondRecordIndex = containerStore.find('name', fieldName, firstRecordIndex + 1, false, true, true);
                    var firstRecord = containerStore.getAt(firstRecordIndex);
                    var secondRecord = secondRecordIndex != -1 ? containerStore.getAt(secondRecordIndex) : null;

                    if(firstRecord.data.type == type) {
                        // Add new records instead of modifying existing ones so the grid renders the changes.
                        containerStore.add({name: firstRecord.data.name, text: firstRecord.data.text, values: mergeValues(firstRecord.data.values, values), type: firstRecord.data.type});
                    } else {
                        var newValues = removeValues(firstRecord.data.values, values);
                        if(newValues.length > 0) {
                            containerStore.add({name: firstRecord.data.name, text: firstRecord.data.text, values: newValues, type: firstRecord.data.type});
                        }

                        if(secondRecord === null) {
                            containerStore.add({name: fieldName, text: fieldText, values: values, type: type});
                        }
                    }
                    containerStore.remove(firstRecord);

                    if(secondRecord != null) {
                        if(secondRecord.data.type == type) {
                            containerStore.add({name: secondRecord.data.name, text: secondRecord.data.text, values: mergeValues(secondRecord.data.values, values), type: secondRecord.data.type});
                        } else {
                            var newValues = removeValues(secondRecord.data.values, values);
                            if(newValues.length > 0) {
                                containerStore.add({name: secondRecord.data.name, text: secondRecord.data.text, values: newValues, type: secondRecord.data.type});
                            }
                        }

                        containerStore.remove(secondRecord);
                    }
                } else {
                    containerStore.add({name: fieldName, text: fieldText, values: values, type: type});
                }

                updateCompareEnabled();
            }
        }

        var centerContainer = {
            xtype: 'panel',
            layout: 'vbox',
            border: false,
            width: 150,
            height: 500,
            padding: 10,
            items: [{
                xtype: 'container',
                layout: {
                    type:'vbox',
                    align: 'center',
                    pack: 'center',
                    defaultMargins: {bottom: 5}
                },
                height: 250,
                items: [{
                    xtype: 'button',
                    text: 'Required >>',
                    scope: this,
                    handler: function() {
                        addCriteria(getActiveBaselineStore(), 'Required');
                    }
                },{
                    xtype: 'button',
                    text: 'Not Allowed >>',
                    scope: this,
                    handler: function() {
                        addCriteria(getActiveBaselineStore(), 'Not Allowed');
                    }
                }]
            },{
                xtype: 'container',
                layout: {
                    type:'vbox',
                    align: 'center',
                    pack: 'center',
                    defaultMargins: {bottom: 5}
                },
                height: 250,
                items: [{
                    xtype: 'button',
                    text: 'Required >>',
                    scope: this,
                    handler: function() {
                        addCriteria(getActiveComparisonStore(), 'Required');
                    }
                },{
                    xtype: 'button',
                    text: 'Not Allowed >>',
                    scope: this,
                    handler: function() {
                        addCriteria(getActiveComparisonStore(), 'Not Allowed');
                    }
                }]
            }]
        };

        var addComparisonSet = function(){
            var idx = rightContainer.items.length;
            rightContainer.insert(
                    idx,
                    {
                        title: 'Comparison Set ' + (idx + 1),
                        closable: true,
                        items: [Ext4.create('TBDCC.CriteriaPanel', {
                            listeners: {
                                scope: this,
                                storechanged: updateCompareEnabled
                            }
                        })]
                    }
            );
            rightContainer.setActiveTab(idx);
            updateCompareEnabled();
        };

        var rightContainer = Ext4.create('Ext.tab.Panel', {
            width: 650,
            height: 525,
            border: false,
            frame: false,
            padding: 0,
            tabBar: {
                items: [{
                    xtype: 'button',
                    text: '+',
                    handler: addComparisonSet,
                    scope: this
                }]
            },
            items: [{
                title: 'Comparison Set 1',
                closable: true,
                menu: {items:[{text: 'remove'}]},
                items: [Ext4.create('TBDCC.CriteriaPanel', {
                    listeners: {
                        scope: this,
                        storechanged: updateCompareEnabled
                    }
                })]
            }],
            listeners: {
                scope: this,
                remove: function(panel){
                    for(var i = 0; i < panel.items.length; i++) {
                        panel.items.getAt(i).setTitle('Comparison Set ' + (i+1));
                    }
                    updateCompareEnabled();
                }
            }
        });

        var getActiveBaselineStore = function(){
            return rightContainer.getActiveTab().items.getAt(0).baselineStore;
        };

        var getActiveComparisonStore = function(){
            return rightContainer.getActiveTab().items.getAt(0).comparisonStore;
        };

        var topContainer = {
            xtype: 'container',
            layout: 'hbox',
            items: [leftContainer, centerContainer, rightContainer]
        };

        var geneTextField = {
            xtype: 'textfield',
            fieldLabel: 'Genes',
            width: 500,
            margin: '5 0 0 0',
            name: 'genes',
            id: 'genes',
            listeners: {
                scope: this,
                change : updateCompareEnabled
            }
        };

        var getComparisonSets = function(genes){
            var comparisonSets = [];

            for (var i = 0; i < rightContainer.items.length; i++) {
                var panel = rightContainer.items.getAt(i);
                comparisonSets.push({
                    id: parseInt(panel.title.slice(panel.title.indexOf("t ") + 2, panel.title.length)),
                    baselineGridFilters: getSelectRowsFilters(panel.items.getAt(0).baselineStore, genes),
                    comparisonGridFilters: getSelectRowsFilters(panel.items.getAt(0).comparisonStore, genes)
                });
            }

            return comparisonSets;
        };

        var applyRFilters = function(partConfig){
            for (var i = 0; i < rightContainer.items.length; i++) {
                var panel = rightContainer.items.getAt(i);
                var baselineStore = panel.items.getAt(0).baselineStore;
                var comparisonStore = panel.items.getAt(0).comparisonStore;
                var id = parseInt(panel.title.slice(panel.title.indexOf("t ") + 2, panel.title.length));

                Ext4.apply(partConfig, makeRFilter(baselineStore, 'b', id));
                Ext4.apply(partConfig, makeRFilter(comparisonStore, 'c', id));
            }
        };

        var makePreCheckRequest = function(requestNum, callback, scope, comparisonSet){
            var baseRequest = {
                schemaName: 'Assay.ExpressionMatrix.' + assayDesignName,
                queryName: 'Data',
                requiredVersion: 9.1,
                maxRows: 1,
                columns: ["Value", "SampleId", "DataId/Name", "FeatureId/GeneSymbol", "FeatureId/RowId", "FeatureId/FeatureId", "Run/LogData"],
                scope: scope,
                success: callback
            };
            var baselineRequestCfg = Ext4.apply({filterArray: comparisonSet.baselineGridFilters}, baseRequest);
            var comparisonRequestCfg = Ext4.apply({filterArray: comparisonSet.comparisonGridFilters}, baseRequest);
            LABKEY.Query.selectRows(baselineRequestCfg);
            LABKEY.Query.selectRows(comparisonRequestCfg);
        };

        var getTabConfig = function(filters, setId, title, divId){
            return {
                title: title,
                html: '<div id ="' + divId + '"></div>',
                minHeight: 600,
                bodyPadding: 5,
                listeners: {
                    scope: this,
                    afterrender: function(tab){
                        var div = Ext4.query('#' + divId)[0];
                        if(div && div.children.length == 0){
                            new LABKEY.QueryWebPart({
                                schemaName: 'Assay.ExpressionMatrix.' + assayDesignName,
                                queryName: 'Data',
                                renderTo: divId,
                                viewName: 'PreviewGrid',
                                frame: 'none',
                                columns: ["Value", "SampleId", "DataId/Name", "FeatureId/GeneSymbol", "FeatureId/RowId", "FeatureId/FeatureId", "Run/LogData"],
                                removeableFilters: filters,
                                scope: this,
                                success: function(){
                                    // force re-layout so QWP fits with no scroll bars.
                                    tab.doComponentLayout();
                                }
                            });
                        }
                    }
                }
            };
        };

        var renderResultsPanel = function(comparisonSets, partConfig, includeRReport){
            Ext4.get('reportDiv').dom.innerHTML = null;
            var chartTab = {
                title: 'Chart',
                autoScroll: true,
                minHeight: 600,
                bodyPadding: 5
            };
            var tabs = [chartTab];

            if (includeRReport) {
                // Issue 18528
                chartTab.html = '<div id="r-report"><br /><br /></div>';
                chartTab.listeners = {
                    scope: this,
                    afterrender: function(tab){
                        tab.getEl().mask('loading...');
                        var reportWebPart = new LABKEY.WebPart({
                            partName: 'Report',
                            renderTo: 'r-report',
                            frame: 'none', // Issue 18529
                            success: function(){
                                tab.getEl().unmask();
                                // Force re-layout so the report webpart fits with no scroll bars.
                                tab.doComponentLayout();
                            },
                            partConfig: partConfig
                        });
                        reportWebPart.render();
                    }
                }
            } else {
                chartTab.html = '<p class="labkey-error">No results were returned. The chosen filters may be too strict.</p>';
            }

            for (var i = 0; i < comparisonSets.length; i++) {
                tabs.push(getTabConfig(comparisonSets[i].baselineGridFilters, comparisonSets[i].id, 'Baseline Grid ' + comparisonSets[i].id, 'baseline-qwp-' + comparisonSets[i].id));
                tabs.push(getTabConfig(comparisonSets[i].comparisonGridFilters, comparisonSets[i].id, 'Comparison Grid ' + comparisonSets[i].id, 'comparison-qwp-' + comparisonSets[i].id));
            }

            Ext4.create('Ext.tab.Panel', {
                width: 800,
                frame: false,
                border: false,
                renderTo: 'reportDiv',
                items: tabs
            });
        };

        var getGenes = function(geneString) {
            // Partial fix for Issue 18398: Make gene symbol search criteria case-insensitive
            geneString = geneString.toUpperCase();

            // Issue 18274: Handle alternate delimiters in GEOMicroarray filtering UI
            if (geneString.split(',').length > 1) {
                return geneString.split(',').join(';')
            }

            if(geneString.split(' ').length > 1) {
                return geneString.split(' ').join(';')
            }

            if(geneString.split('\t').length > 1) {
                return geneString.split('\t').join(';')
            }

            // assume semicolons or none (one gene).
            return geneString;
        };

        var doCompare = function(){
            var genes = getGenes(Ext4.ComponentManager.get('genes').getValue());
            var comparisonSets = getComparisonSets(genes);
            var partConfig = {
                title: 'Baseline / Comparison Image Output',
                reportId: reportId,
                assayDesignName: assayDesignName,
                genes: genes,
                showSection: 'labkey_png'
            };
            applyRFilters(partConfig);
            var requestNum = comparisonSets.length * 2;
            var reqCounter = 0; // Since the baseline and comparison requests are async we increment the counter when
            // either one completes. Then when the counter is 2 we know we can send the request
            // out for the WebPart.
            var hasData = false; // Make sure at least one filter set returns data.
            var counterCallback = function(response){
                reqCounter = reqCounter + 1;
                if(response.rowCount && response.rowCount > 0) {
                    hasData =  true;
                }
                if(reqCounter == requestNum) {
                    if(hasData) {
                        renderResultsPanel(comparisonSets, partConfig, true);
                    } else {
                        renderResultsPanel(comparisonSets, partConfig, false);
                    }
                }
            };

            for (var i = 0; i < comparisonSets.length; i++) {
                makePreCheckRequest(requestNum, counterCallback, this, comparisonSets[i]);
            }
        };

        var compareBtn = {
            xtype: 'button',
            text: 'Compare',
            margin: '5 0 0 0',
            id: 'compareButton',
            disabled: true,
            scope: this,
            handler: doCompare
        };

        var outerPanel = Ext4.create('Ext.panel.Panel', {
            renderTo: 'queryUI',
            border: false,
            frame: false,
            width: 1400,
            height: 580,
            style: 'background-color: transparent',
            items: [topContainer, geneTextField, compareBtn]
        });
    }

    var makeRFilter = function(store, type, id) {
        var result = {};
        for (var i = 0; i < store.getCount(); i++) {
            var recordData = store.getAt(i).data;
            var keyName = type + '.' + id + '.' + (recordData.type == 'Required' ? 'in' : 'notin' ) + '.' + recordData.name;
            var filter = result[keyName] || '';
            if (!filter) {
                result[keyName] = recordData.values.join(';');
            }
            else {
                result[keyName] = filter + ';' + recordData.values;
            }
        }

        return result;
    };

    var getSelectRowsFilters = function(store, geneSymbols) {
        var filters = [new LABKEY.Query.Filter.In('FeatureId/GeneSymbol', geneSymbols)];

        for (var i = 0; i < store.getCount(); i++) {
            var recordData = store.getAt(i).data;

            if(recordData.type == 'Required') {
                filters.push(new LABKEY.Query.Filter.In('SampleId/' + recordData.name, recordData.values.join(';')));
            } else {
                filters.push(new LABKEY.Query.Filter.NotIn('SampleId/' + recordData.name, recordData.values.join(';')));
            }
        }

        return filters;
    };

    Ext4.onReady(function() {
        Ext4.Ajax.request({
            url : LABKEY.ActionURL.buildURL('core', 'getModuleProperties', null),
            method : 'POST',
            scope: this,
            success: onLoad,
            failure: LABKEY.Utils.displayAjaxErrorResponse,
            jsonData: {
                moduleName: 'microarray',
                includePropertyDescriptors: true,
                includePropertyValues: true
            },
            headers : {
                'Content-Type' : 'application/json'
            }
        });
    });

</script>
