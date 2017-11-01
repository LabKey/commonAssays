/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

/**
 * User: cnathe
* Date: Sept 7, 2011
*/

Ext.QuickTips.init();

/**
 * Class to display panel for selecting which runs are part of the current guide set for the given
 * titration, analyte, isotype, and conjugate combination
 *
 * @params disableId : the ID of the guide set to be disabled
 * @params guideSetId : the ID of the guide set to be managed (if null, then we are to create a new one)
 * @params assayName
 * @params controlName
 * @params controlType
 * @params analyte
 * @params isotype
 * @params conjugate 
 */
LABKEY.ManageGuideSetPanel = Ext.extend(Ext.FormPanel, {

    labelStyleStr : 'padding: 0; margin: 0;',

    metrics : [
        {name: 'EC504PL', label: 'EC50 4PL', includeForSinglePointControl: false},
        {name: 'EC505PL', label: 'EC50 5PL (Rumi)', includeForSinglePointControl: false},
        {name: 'AUC', label: 'AUC', includeForSinglePointControl: false},
        {name: 'MaxFI', label: 'High MFI', includeForSinglePointControl: true}
    ],

    constructor : function(config){
        // check that the config properties needed are present
        if (!config.assayName)
            throw "You must specify a assayName!";
        if (!config.controlName || !config.controlType || !config.analyte || config.isotype == undefined || config.conjugate == undefined)
            throw "You must specify the following params: controlName, controlType, analyte, isotype, and conjugate!";

        // apply some Ext panel specific properties to the config
        Ext.apply(config, {
            border: false,
            items: [],
            padding: 10,
            buttonAlign: 'right',
            buttons: [],
            cls: 'extContainer',
            autoScroll: true
        });

        this.addEvents('closeManageGuideSetPanel');

        LABKEY.ManageGuideSetPanel.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        LABKEY.ManageGuideSetPanel.superclass.initComponent.call(this);

        var columns = 'RowId, CurrentGuideSet, Comment, Created, ValueBased';
        Ext.each(this.metrics, function(metric){
            if (this.isTitrationControlType() || metric.includeForSinglePointControl)
                columns += ', ' + metric.name + 'Average, ' + metric.name + 'StdDev';
        }, this);

        if (this.guideSetId)
        {
            // query the server for the current guide set information
            LABKEY.Query.selectRows({
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                queryName: 'GuideSet',
                filterArray: [LABKEY.Filter.create('RowId', this.guideSetId)],
                columns: columns,
                success: this.addGuideSetInfoLabels,
                scope: this
            });
        }
        else
            this.addGuideSetInfoLabels();
    },

    addGuideSetInfoLabels: function(data) {
        if (data)
        {
            if (data.rows.length > 1)
            {
                Ext.Msg.alert("Error", "More than one guide set found for id " + this.guideSetId);
            }
            else if(data && data.rows.length == 1)
            {
                this.guideSetRowData = data.rows[0];
                this.valueBased = this.guideSetRowData["ValueBased"];
            }
        }
        else
        {
            this.guideSetRowData = {};
        }

        // add labels for the guide set information to the top of the panel
        this.add(new Ext.Panel({
            width: 1075,
            border: false,
            items: [{
                border: false,
                layout: 'column',
                defaults:{
                    columnWidth: 0.35,
                    layout: 'form',
                    border: false
                },
                items: [{
                    defaults:{xtype: 'label', labelStyle: this.labelStyleStr},
                    items: [
                        {fieldLabel: 'Guide Set Id', text: this.guideSetId ? this.guideSetId : "TBD", id: 'guideSetIdLabel'},
                        {fieldLabel: 'Control Name', text: this.controlName},
                        {fieldLabel: 'Analyte', text: this.analyte, id: 'analyteLabel'}
                    ]
                },{
                    defaults:{xtype: 'label', labelStyle: this.labelStyleStr},
                    items: [
                        {fieldLabel: 'Created', text: this.guideSetRowData["Created"] ? this.dateRenderer(this.guideSetRowData["Created"]) : "TBD"},
                        {fieldLabel: 'Isotype', text: this.isotype == "" ? '[None]' : this.isotype},
                        {fieldLabel: 'Conjugate', text: this.conjugate == "" ? '[None]' : this.conjugate}
                    ]
                },{
                    columnWidth: 0.3,
                    items: [{
                        xtype: 'label',
                        labelStyle: this.labelStyleStr,
                        fieldLabel: 'Type',
                        text: this.guideSetRowData["ValueBased"] ? 'Value-based' : 'Run-based',
                        hidden: !this.guideSetId
                    },{
                        xtype:'fieldset',
                        title: 'Guide Set Type',
                        collapsible: false,
                        padding: '0 5px 5px 5px',
                        defaultType: 'radio',
                        layout: 'hbox',
                        hidden: this.guideSetId,
                        items: [
                            {
                                hideLabel: true,
                                boxLabel: 'Run-based',
                                name: 'ValueBased',
                                inputValue: false,
                                checked: true,
                                width: 150
                            },
                            {
                                hideLabel: true,
                                boxLabel: 'Value-based',
                                name: 'ValueBased',
                                inputValue: true,
                                listeners: {
                                    scope: this,
                                    check: this.toggleGuideSetSections
                                }
                            }
                        ]
                    }]
                }]
            }]
        }));
        this.add(new Ext.Spacer({height: 20}));

        // make sure that this guide set is a "current" guide set (only current sets are editable)
        var fields;
        var runPrefix;
        if (this.isTitrationControlType())
        {
            fields = ['Analyte', 'GuideSet', 'IncludeInGuideSetCalculation', 'Titration', 'Titration/Run/Conjugate', 'Titration/Run/Batch/Network', 'Titration/Run/Batch/CustomProtocol',
                'Titration/Run/NotebookNo', 'Titration/Run/AssayType', 'Titration/Run/ExpPerformer', 'Analyte/Data/AcquisitionDate', 'Titration/Run/Folder/Name',
                'Titration/Run/Isotype', 'Titration/Run/Name', 'Four ParameterCurveFit/EC50', 'Five ParameterCurveFit/EC50', 'MaxFI', 'TrapezoidalCurveFit/AUC'];
            runPrefix = 'Titration/Run';
        }
        else
        {
            fields = ['Analyte', 'GuideSet', 'IncludeInGuideSetCalculation', 'SinglePointControl', 'SinglePointControl/Run/Conjugate', 'SinglePointControl/Run/Batch/Network', 'SinglePointControl/Run/Batch/CustomProtocol',
                'SinglePointControl/Run/NotebookNo', 'SinglePointControl/Run/AssayType', 'SinglePointControl/Run/ExpPerformer', 'Analyte/Data/AcquisitionDate', 'SinglePointControl/Run/Folder/Name',
                'SinglePointControl/Run/Isotype', 'SinglePointControl/Run/Name', 'AverageFiBkgd'];
            runPrefix = 'SinglePointControl/Run';
        }

        if (this.currentGuideSetIsInactive())
        {
            this.add({
                xtype: 'displayfield',
                hideLabel: true,
                value: 'The selected guide set is not a currently active guide set. Only current guide sets are editable at this time.'
            });
        }
        else
        {
            // add a grid for all of the runs that match the guide set criteria
            var allRunsStore = new Ext.data.JsonStore({
                storeId: 'allRunsStore',
                root: 'rows',
                fields: fields
            });

            // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
            var allRunsCols = [];
            allRunsCols.push({header:'', dataIndex:'RowId', renderer:this.renderAddRunIcon, scope: this, width:25});
            allRunsCols.push({header:'Assay Id', dataIndex:runPrefix + '/Name', renderer: this.encodingRenderer, width:200});
            allRunsCols.push({header:'Network', dataIndex:runPrefix + '/Batch/Network', width:75, renderer: this.encodingRenderer, hidden: !this.networkExists});
            allRunsCols.push({header:'Protocol', dataIndex:runPrefix + '/Batch/CustomProtocol', width:75, renderer: this.encodingRenderer, hidden: !this.protocolExists});
            allRunsCols.push({header:'Folder', dataIndex:runPrefix + '/Folder/Name', renderer: this.encodingRenderer, width:75});
            allRunsCols.push({header:'Notebook No.', dataIndex:runPrefix + '/NotebookNo', width:100, renderer: this.encodingRenderer});
            allRunsCols.push({header:'Assay Type', dataIndex:runPrefix + '/AssayType', width:100, renderer: this.encodingRenderer});
            allRunsCols.push({header:'Experiment Performer', dataIndex:runPrefix + '/ExpPerformer', width:100, renderer: this.encodingRenderer});
            allRunsCols.push({header:'Acquisition Date', dataIndex:'Analyte/Data/AcquisitionDate', renderer: this.dateRenderer, width:100});
            if (this.isTitrationControlType())
            {
                allRunsCols.push({header:'EC50 4PL', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'});
                allRunsCols.push({header:'EC50 5PL', dataIndex:'Five ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'});
                allRunsCols.push({header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: this.numberRenderer, align: 'right'});
                allRunsCols.push({header:'High MFI', dataIndex:'MaxFI', width:75, renderer: this.numberRenderer, align: 'right'});
            }
            else
            {
                allRunsCols.push({header:'MFI', dataIndex:'AverageFiBkgd', width:75, renderer: this.numberRenderer, align: 'right'});
            }
            var allRunsColModel = new Ext.grid.ColumnModel({
                defaults: {sortable: true},
                columns: allRunsCols,
                scope: this
            });

            // create the grid for the full list of runs that match the given guide set criteria
            this.allRunsGrid = new Ext.grid.GridPanel({
                autoScroll:true,
                height:200,
                width:1075,
                loadMask:{msg:"Loading runs..."},
                store: allRunsStore,
                colModel: allRunsColModel,
                disableSelection: true,
                viewConfig: {forceFit: true},
                stripeRows: true
            });
            this.allRunsGrid.on('cellclick', function(grid, rowIndex, colIndex, event){
                if (colIndex == 0)
                    this.addRunToGuideSet(grid.getStore().getAt(rowIndex));
            }, this);
            this.allRunsGrid.on('viewready', function(grid){
                if (!this.allRunsGrid.dataLoaded)
                    grid.getEl().mask("loading...", "x-mask-loading");
            }, this);

            this.add(new Ext.Panel({
                title: 'All Runs',
                id: 'AllRunsGrid',
                hidden: true,
                width:1075,
                items: [
                    {
                        xtype: 'displayfield',
                        style: 'padding: 5px;',
                        value: Ext.util.Format.htmlEncode('List of all of the runs from the "' + this.assayName + '" assay that contain '
                            + this.controlName + ' ' + this.analyte + ' '
                            + (this.isotype == "" ? '[None]' : this.isotype) + ' '
                            + (this.conjugate == "" ? '[None]' : this.conjugate) + '.')
                            + '<br/>Note that runs that are already members of a different guide set will not be displayed.'
                    },
                    this.allRunsGrid
                ]
            }));
        }
        this.add(new Ext.Spacer({
            height: 20,
            id: 'AllRunsGridSpacer',
            hidden: true
        }));

        // add a grid for the list of runs currently in the selected guide set
        var guideRunSetStore = new Ext.data.JsonStore({
            storeId: 'guideRunSetStore',
            root: 'rows',
            fields: fields
        });

        // column model for the list of columns to show in the grid (and a special renderer for the rowId column)
        var guideRunSetCols = [];
        guideRunSetCols.push({header:'', dataIndex:'RowId', renderer:this.renderRemoveIcon, scope: this, hidden: this.guideSetId && !this.guideSetRowData["CurrentGuideSet"], width:25});
        guideRunSetCols.push({header:'Assay Id', dataIndex:runPrefix + '/Name', renderer: this.encodingRenderer, width:200});
        guideRunSetCols.push({header:'Network', dataIndex:runPrefix + '/Batch/Network', width:75, renderer: this.encodingRenderer, hidden: !this.networkExists});
        guideRunSetCols.push({header:'Protocol', dataIndex:runPrefix + '/Batch/CustomProtocol', width:75, renderer: this.encodingRenderer, hidden: !this.protocolExists});
        guideRunSetCols.push({header:'Folder', dataIndex:runPrefix + '/Folder/Name', renderer: this.encodingRenderer, width:75});
        guideRunSetCols.push({header:'Notebook No.', dataIndex:runPrefix + '/NotebookNo', width:100, renderer: this.encodingRenderer});
        guideRunSetCols.push({header:'Assay Type', dataIndex:runPrefix + '/AssayType', width:100, renderer: this.encodingRenderer});
        guideRunSetCols.push({header:'Experiment Performer', dataIndex:runPrefix + '/ExpPerformer', width:100, renderer: this.encodingRenderer});
        guideRunSetCols.push({header:'Acquisition Date', dataIndex:'Analyte/Data/AcquisitionDate', renderer: this.dateRenderer, width:100});
        if (this.isTitrationControlType())
        {
            guideRunSetCols.push({header:'EC50 4PL', dataIndex:'Four ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'});
            guideRunSetCols.push({header:'EC50 5PL', dataIndex:'Five ParameterCurveFit/EC50', width:75, renderer: this.numberRenderer, align: 'right'});
            guideRunSetCols.push({header:'AUC', dataIndex:'TrapezoidalCurveFit/AUC', width:75, renderer: this.numberRenderer, align: 'right'});
            guideRunSetCols.push({header:'High MFI', dataIndex:'MaxFI', width:75, renderer: this.numberRenderer, align: 'right'});
        }
        else
        {
            guideRunSetCols.push({header:'MFI', dataIndex:'AverageFiBkgd', width:75, renderer: this.numberRenderer, align: 'right'});
        }
        var guideRunSetColModel = new Ext.grid.ColumnModel({
            defaults: {sortable: true},
            columns: guideRunSetCols,
            scope: this
        });

        // create the grid for the runs that are a part of the given guide set
        this.guideRunSetGrid = new Ext.grid.GridPanel({
            autoHeight:true,
            width:1075,
            loadMask:{msg:"Loading runs assigned to guide set..."},
            store: guideRunSetStore,
            colModel: guideRunSetColModel,
            disableSelection: true,
            viewConfig: {forceFit: true},
            stripeRows: true
        });
        this.guideRunSetGrid.on('cellclick', function(grid, rowIndex, colIndex, event){
            if (colIndex == 0)
                this.removeRunFromGuideSet(grid.getStore().getAt(rowIndex));
        }, this);
        this.guideRunSetGrid.on('viewready', function(grid){
            if (!this.guideRunSetGrid.dataLoaded)
                grid.getEl().mask("loading...", "x-mask-loading");
        }, this);

        this.add(new Ext.Panel({
            title: 'Runs Assigned to This Guide Set',
            id: 'GuideSetRunGrid',
            hidden: true,
            width:1075,
            items: [
                {
                    xtype: 'displayfield',
                    style: 'padding: 5px;',
                    value: 'List of all of the runs included in the guide set calculations for the selected guide set.'
                },
                this.guideRunSetGrid
            ]
        }));
        this.add(new Ext.Spacer({
            height: 20,
            id: 'GuideSetRunGridSpacer',
            hidden: true
        }));

        this.add(new Ext.Panel({
            title: 'Metric Values',
            id: 'MetricValues',
            hidden: true,
            layout: 'column',
            width: 1075,
            padding: 5,
            defaults: {
                columnWidth: .16,
                border: false
            },
            items: [
                this.getMetricLabelsPanel(),
                this.getMetricMeanValuesPanel(),
                this.getMetricStdDevValuesPanel()
            ]
        }));
        this.add(new Ext.Spacer({
            height: 20,
            id: 'MetricValuesSpacer',
            hidden: true
        }));

        // add a comment text field for the guide set
        this.commentTextField = new Ext.form.TextField({
            id: 'commentTextField',
            labelStyle: 'padding: 3px 0 0 0; margin: 0;',
            fieldLabel: 'Comment',
            value: this.guideSetRowData["Comment"],
            disabled: this.currentGuideSetIsInactive(),
            width: 965,
            enableKeyEvents: true,
            listeners: {
                scope: this,
                'keydown': function(){ Ext.getCmp('saveButton').enable(); },
                'change': function(){ Ext.getCmp('saveButton').enable(); }
            }
        });

        this.add(this.commentTextField);
        this.add(new Ext.Spacer({height: 10}));

        // add save and cancel buttons to the toolbar
        if (!this.guideSetId || this.guideSetRowData["CurrentGuideSet"])
        {
            this.addButton({
                id: 'cancelButton',
                text: 'Cancel',
                handler: function(){
                    this.fireEvent('closeManageGuideSetPanel');
                },
                scope: this
            });
            
            this.addButton({
                id: 'saveButton',
                text: this.guideSetId ? 'Save' : 'Create',
                disabled: true,
                handler: this.guideSetId ? this.saveGuideSetData : this.createGuideSet,
                scope: this
            });
        }

        this.toggleGuideSetSections();

        this.doLayout();

        this.queryAllRunsForCriteria();
    },

    isTitrationControlType : function() {
        return this.controlType == 'Titration';
    },

    currentGuideSetIsInactive : function() {
        return this.guideSetId && !this.guideSetRowData["CurrentGuideSet"];
    },

    createLabelField : function(txt, style) {
        return {
            xtype: 'displayfield',
            hideLabel: true,
            height: 25,
            cls: 'guideset-label',
            style: style || 'margin: 3px;',
            value: txt
        };
    },

    createNumberField : function(name) {
        return new Ext.form.NumberField({
            height: 25,
            cls: 'guideset-numberfield',
            itemId: name,
            name: name,
            hideLabel: true,
            disabled: this.currentGuideSetIsInactive(),
            value: this.guideSetRowData[name],
            decimalPrecision: 6, //match change from issue 16767
            enableKeyEvents: true,
            listeners: {
                scope: this,
                'keyup': this.onNumberFieldChange,
                'change': this.onNumberFieldChange
            }
        });
    },

    onNumberFieldChange : function(field) {
        // issue 20152: don't allow std dev if average is blank
        if (field.getName().indexOf("Average") > -1)
        {
            var avgFieldName = field.getName().replace("Average", "StdDev");
            var stdDevField = this.getMetricStdDevValuesPanel().getComponent(avgFieldName);
            this.updateStdDevField(stdDevField, field.getValue());
        }

        Ext.getCmp('saveButton').enable();
    },

    updateStdDevField : function(stdDevField, avgFieldVal) {
        var isAvgBlank = avgFieldVal == null || avgFieldVal.length == 0;
        stdDevField.setDisabled(isAvgBlank);
        if (isAvgBlank) stdDevField.setValue(null);
    },

    getMetricLabelsPanel : function() {
        if (!this.metricLabelsPanel)
        {
            this.metricLabelsPanel = new Ext.Panel({
                items: [
                    this.createLabelField('&nbsp;', 'margin: 3px;')
                ]
            });

            Ext.each(this.metrics, function(metric){
                if (this.isTitrationControlType() || metric.includeForSinglePointControl)
                    this.metricLabelsPanel.add(this.createLabelField(metric.label + ':'));
            }, this);
        }

        return this.metricLabelsPanel;
    },

    getMetricMeanValuesPanel : function() {
        if (!this.metricMeanValuesPanel)
        {
            this.metricMeanValuesPanel = new Ext.Panel({
                items: [
                    this.createLabelField('Mean',  'text-align: center; margin: 3px;')
                ]
            });

            Ext.each(this.metrics, function(metric){
                if (this.isTitrationControlType() || metric.includeForSinglePointControl)
                    this.metricMeanValuesPanel.add(this.createNumberField(metric.name + 'Average'));
            }, this);
        }

        return this.metricMeanValuesPanel;
    },

    getMetricStdDevValuesPanel : function() {
        if (!this.metricStdDevValuesPanel)
        {
            this.metricStdDevValuesPanel = new Ext.Panel({
                items: [
                    this.createLabelField('Std. Dev.',  'text-align: center; margin: 3px;')
                ]
            });

            Ext.each(this.metrics, function(metric){
                if (this.isTitrationControlType() || metric.includeForSinglePointControl)
                {
                    // issue 20152: don't allow std dev if average is blank
                    var stdDevField = this.createNumberField(metric.name + 'StdDev');
                    var avgFieldVal = this.getMetricMeanValuesPanel().getComponent(metric.name + 'Average').getValue();
                    this.updateStdDevField(stdDevField, avgFieldVal);

                    this.metricStdDevValuesPanel.add(stdDevField);
                }
            }, this);
        }

        return this.metricStdDevValuesPanel;
    },

    getMetricValues : function() {
        var values = {};

        Ext.each(this.metrics, function(metric){
            if (this.isTitrationControlType() || metric.includeForSinglePointControl)
            {
                var numFld = this.getMetricMeanValuesPanel().getComponent(metric.name + 'Average');
                values[numFld.getName()] = numFld.getValue();

                numFld = this.getMetricStdDevValuesPanel().getComponent(metric.name + 'StdDev');
                values[numFld.getName()] = numFld.getValue();
            }
        }, this);

        return values;
    },

    toggleGuideSetSections : function(radio, selectedValueBased) {
        var isValueBased = this.guideSetId && this.guideSetRowData["ValueBased"];
        this.valueBased = isValueBased || selectedValueBased;

        if (!this.currentGuideSetIsInactive())
        {
            Ext.getCmp('AllRunsGrid').setVisible(!this.valueBased);
            Ext.getCmp('AllRunsGridSpacer').setVisible(!this.valueBased);
        }

        Ext.getCmp('GuideSetRunGrid').setVisible(!this.valueBased);
        Ext.getCmp('GuideSetRunGridSpacer').setVisible(!this.valueBased);

        Ext.getCmp('MetricValues').setVisible(this.valueBased);
        Ext.getCmp('MetricValuesSpacer').setVisible(this.valueBased);
    },

    createGuideSet: function() {
        this.getEl().mask('Creating new guide set...', "x-mask-loading");

        // if there is already a current guide set, it needs to be disabled
        var commands = [];
        if (this.disableId)
        {
            commands.push({
                schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                queryName: 'GuideSet',
                command: 'update',
                rows: [{RowId: this.disableId, CurrentGuideSet: false}]
            });
        }

        // add a command to create the new guide set
        commands.push({
            schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
            queryName: 'GuideSet',
            command: 'insert',
            rows: [{
                ValueBased: this.valueBased != undefined ? this.valueBased : false,
                IsTitration: this.isTitrationControlType(),
                ControlName: this.controlName,
                AnalyteName: this.analyte,
                Isotype: this.isotype,
                Conjugate: this.conjugate,
                CurrentGuideSet: true
            }]
        });

        var that = this; // work-around for 'too much recursion' error
        LABKEY.Query.saveRows({
            commands: commands,
            success: function(data) {
                // get the newly created guide set Id from the response
                for (var i=0; i < data.result.length; i++)
                {
                    if (data.result[i].command == "insert")
                    {
                        that.guideSetId = data.result[i].rows[0].rowId;
                        break;
                    }
                }

                that.saveGuideSetData();
            },
            failure: function(response) {
                Ext.Msg.alert("Error", response.exception);
                if (that.getEl().isMasked())
                    that.getEl().unmask();
            }
        });        
    },

    saveGuideSetData: function() {
        this.getEl().mask('Saving guide set information...', "x-mask-loading");

        var commands = [{
            schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
            queryName: 'GuideSet',
            command: 'update',
            rows: [{
                RowId: this.guideSetId,
                Comment: this.commentTextField.getValue()
            }]
        }];

        // if we are creating a Value-based guide set, add the metric values
        // otherwise get the information from the run grids
        if (this.valueBased)
        {
            Ext.apply(commands[0].rows[0], this.getMetricValues());
        }
        else
        {
            // get the list of modified records and set up the save rows array
            var modRecords = this.allRunsGrid.getStore().getModifiedRecords();
            var dataRows = [];
            Ext.each(modRecords, function(record){

                var dataRow = {
                    Analyte: record.get("Analyte"),
                    IncludeInGuideSetCalculation: record.get("IncludeInGuideSetCalculation"),
                    GuideSetId: record.get("IncludeInGuideSetCalculation") ? this.guideSetId : record.get("GuideSet")
                };
                if (this.isTitrationControlType())
                {
                    dataRow.Titration = record.get("Titration");
                }
                else
                {
                    dataRow.SinglePointControl = record.get("SinglePointControl");
                }
                dataRows.push(dataRow);
            }, this);

            if (dataRows.length > 0)
            {
                commands.push({
                    schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
                    queryName: this.isTitrationControlType() ? 'AnalyteTitration' : 'AnalyteSinglePointControl',
                    command: 'update',
                    rows: dataRows
                });
            }
        }

        var that = this; // work-around for 'too much recursion' error
        LABKEY.Query.saveRows({
            commands: commands,
            success: function(data) {
                if (that.getEl().isMasked())
                    that.getEl().unmask();
                that.fireEvent('closeManageGuideSetPanel', data["result"]);
            },
            failure: function(response) {
                Ext.Msg.alert("Error", response.exception);
                if (that.getEl().isMasked())
                    that.getEl().unmask();
            }
        });
    },

    queryAllRunsForCriteria: function() {

        var columns;
        var runPrefix;
        if (this.isTitrationControlType())
        {
            columns = 'Analyte, Titration, Titration/Run/Name, Titration/Run/Folder/Name, Titration/Run/Folder/EntityId, '
                                + 'Titration/Run/Isotype, Titration/Run/Conjugate, Titration/Run/Batch/Network, Titration/Run/Batch/CustomProtocol, Titration/Run/NotebookNo, '
                                + 'Titration/Run/AssayType, Titration/Run/ExpPerformer, Analyte/Data/AcquisitionDate, GuideSet, IncludeInGuideSetCalculation, '
                                + 'Four ParameterCurveFit/EC50, Five ParameterCurveFit/EC50, MaxFI, TrapezoidalCurveFit/AUC';
            runPrefix = 'Titration/Run';
        }
        else
        {
            columns = 'Analyte, SinglePointControl, SinglePointControl/Run/Name, SinglePointControl/Run/Folder/Name, SinglePointControl/Run/Folder/EntityId, '
                                + 'SinglePointControl/Run/Isotype, SinglePointControl/Run/Conjugate, SinglePointControl/Run/Batch/Network, SinglePointControl/Run/Batch/CustomProtocol, SinglePointControl/Run/NotebookNo, '
                                + 'SinglePointControl/Run/AssayType, SinglePointControl/Run/ExpPerformer, Analyte/Data/AcquisitionDate, GuideSet, IncludeInGuideSetCalculation, '
                                + 'AverageFiBkgd';
            runPrefix = 'SinglePointControl/Run';
        }

        // query the server for the list of runs that meet the given criteria
        LABKEY.Query.selectRows({
            schemaName: 'assay.Luminex.' + LABKEY.QueryKey.encodePart(this.assayName),
            queryName: this.isTitrationControlType() ? 'AnalyteTitration' : 'AnalyteSinglePointControl',
            columns: columns,
            filterArray: [
                this.isTitrationControlType() ? LABKEY.Filter.create('Titration/Name', this.controlName) : LABKEY.Filter.create('SinglePointControl/Name', this.controlName),
                LABKEY.Filter.create('Analyte/Name', this.analyte),
                LABKEY.Filter.create(runPrefix + '/Isotype', this.isotype, (this.isotype == "" ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL)),
                LABKEY.Filter.create(runPrefix + '/Conjugate', this.conjugate, (this.conjugate == "" ? LABKEY.Filter.Types.MISSING : LABKEY.Filter.Types.EQUAL))
            ],
            sort: '-Analyte/Data/AcquisitionDate, -' + runPrefix + '/Created',
            containerFilter: LABKEY.Query.containerFilter.allFolders,
            success: this.populateRunGridStores,
            failure: function(response){
                Ext.Msg.alert("Error", response.exception);
                this.allRunsGrid.getEl().unmask();
                this.guideRunSetGrid.getEl().unmask();
            },
            scope: this
        });
    },

    populateRunGridStores: function(data) {
        // loop through the list of runs and determin which ones are candidates for inclusion in the current guide set and which ones are already included
        var allRunsStoreData = {rows: []};
        var guideRunSetStoreData = {rows: []};
        for (var i = 0; i < data.rows.length; i++)
        {
            var row = data.rows[i];

            // include in all runs list if not already a member of a different guide set
            if (!(row["GuideSet"] != this.guideSetId && row["IncludeInGuideSetCalculation"]))
                allRunsStoreData.rows.push(row);

            // include in guide set if that is the case
            if (row["GuideSet"] == this.guideSetId && row["IncludeInGuideSetCalculation"])
                guideRunSetStoreData.rows.push(row);    
        }

        if (this.allRunsGrid)
        {
            this.allRunsGrid.getStore().loadData(allRunsStoreData);
            this.allRunsGrid.dataLoaded = true;
            this.allRunsGrid.getEl().unmask();
        }

        if (this.guideRunSetGrid)
        {
            this.guideRunSetGrid.getStore().loadData(guideRunSetStoreData);
            this.guideRunSetGrid.dataLoaded = true;
            this.guideRunSetGrid.getEl().unmask();
        }
    },

    renderRemoveIcon: function(value, metaData, record, rowIndex, colIndex, store) {
        return "<span class='labkey-file-remove-icon labkey-file-remove-icon-enabled' id='guideRunSetRow_" + rowIndex + "'>&nbsp;</span>";
    },

    removeRunFromGuideSet: function(record) {
        // remove the record from the guide set store
        this.guideRunSetGrid.getStore().remove(record);

        // enable the add icon in the all runs store by setting the IncludeInGuideSetCalculation value 
        var index = this.allRunsGrid.getStore().findBy(function(rec, id){
            return (record.get("Analyte") == rec.get("Analyte") &&
                    ((this.isTitrationControlType() && record.get("Titration") == rec.get("Titration")) || ((record.get("SinglePointControl") == rec.get("SinglePointControl")))));
        }, this);

        if (index > -1)
            this.allRunsGrid.getStore().getAt(index).set("IncludeInGuideSetCalculation", false);

        // enable the save button
        Ext.getCmp('saveButton').enable();
    },

    renderAddRunIcon: function(value, metaData, record, rowIndex, colIndex, store) {
        if (record.get("IncludeInGuideSetCalculation"))
            return "<span class='labkey-file-add-icon labkey-file-add-icon-disabled' id='allRunsRow_" + rowIndex + "'>&nbsp;</span>";
        else
            return "<span class='labkey-file-add-icon labkey-file-add-icon-enabled' id='allRunsRow_" + rowIndex + "'>&nbsp;</span>";
    },

    addRunToGuideSet: function(record) {
        if (!record.get("IncludeInGuideSetCalculation"))
        {
            // disable the add icon in the all runs store by setting the IncludeInGuideSetCalculation value
            record.set("IncludeInGuideSetCalculation", true);

            // add the record to the guide set store
            this.guideRunSetGrid.getStore().insert(0, record.copy());

            // enable the save button
            Ext.getCmp('saveButton').enable();
        }
    },

    dateRenderer: function(val) {
        return val ? new Date(val).format("Y-m-d") : null;
    },

    numberRenderer: function(val) {
        // if this is a very small number, display more decimal places
        if (null == val)
        {
            return null;
        }
        else
        {
            if (val > 0 && val < 1)
            {
                return Ext.util.Format.number(Ext.util.Format.round(val, 6), '0.000000');
            }
            else
            {
                return Ext.util.Format.number(Ext.util.Format.round(val, 2), '0.00');
            }
        }
    },

    encodingRenderer: function(value, p, record) {
        return $h(value);
    }
});