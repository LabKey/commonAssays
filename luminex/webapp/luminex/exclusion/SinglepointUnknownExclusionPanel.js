/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of 'Exclude Singlepoint Unknowns' menu button to open the exclusion window
function openExclusionsSinglepointUnknownWindow(assayId, runId)
{
    // lookup the assay design information based on the Assay RowId
    LABKEY.Assay.getById({
        id: assayId,
        success: function(assay)
        {
            if (Ext.isArray(assay) && assay.length == 1)
            {
                var win = new LABKEY.Exclusions.BaseWindow({
                    title: 'Exclude Singlepoint Unknowns from Analysis',
                    width: Ext.getBody().getViewSize().width < 550 ? Ext.getBody().getViewSize().width * .9 : 500,
                    height: Ext.getBody().getViewSize().height > 700 ? 600 : Ext.getBody().getViewSize().height * .75,
                    items: new LABKEY.Exclusions.SinglepointUnknownPanel({
                        protocolSchemaName: assay[0].protocolSchemaName,
                        assayId: assayId,
                        runId: runId,
                        listeners: {
                            scope: this,
                            closeWindow: function()
                            {
                                win.close();
                            }
                        }
                    })
                });
                win.show(this);
            }
            else {
                Ext.Msg.alert('ERROR', 'Unable to find assay design information for id ' + assayId);
            }
        }
    });
}

/**
 * Class to display panel for selecting which singlepoint unknowns to exclude from a Luminex run
 * @params protocolSchemaName = the encoded protocol schema name to use (based on the assay design name)
 * @params assayId = the assay design RowId
 * @params runId = runId for the selected replicate group
 */
LABKEY.Exclusions.SinglepointUnknownPanel = Ext.extend(LABKEY.Exclusions.BasePanel, {

    DISPLAY_NOUN : 'Singlepoint Unknown',

    HEADER_TXT : 'Analytes excluded for a well, replicate group, titration, or at the assay level will not be re-included by '
                    + 'changes in singlepoint unknown exclusions.',

    ITEM_RECORD_KEY : 'SinglepointKey',

    EXCLUSION_TABLE_NAME : 'SinglepointUnknownExclusion',

    EXCLUSION_INCLUDES_DILUTION : true,

    initComponent: function ()
    {
        this.excluded = [];
        this.comments = [];
        this.excludedDataIds = [];
        this.present = [];
        this.preExcludedIds = [];
        this.preAnalyteRowIds = [];

        LABKEY.Exclusions.SinglepointUnknownPanel.superclass.initComponent.call(this);

        this.setupWindowPanelItems();
    },

    setupWindowPanelItems: function ()
    {
        this.addHeaderPanel(this.HEADER_TXT);

        // calling this initiates the loading the of necessary store data
        this.getExclusionsStore();

        this.add(this.getAvailableItemsGrid());
        this.add(this.getAvailableAnalytesGrid());
        this.addCommentPanel();
        this.addStandardButtons();
        this.doLayout();

        this.queryForRunAssayId();
    },

    getExclusionsStore : function()
    {
        if (!this.exclusionsStore)
        {
            this.exclusionsStore = new LABKEY.ext.Store({
                schemaName: this.protocolSchemaName,
                queryName: this.EXCLUSION_TABLE_NAME,
                columns: 'Description,Dilution,Analytes/RowId,RowId,Comment,DataId,DataId/Run',
                filterArray : [
                    LABKEY.Filter.create('DataId/Run', this.runId, LABKEY.Filter.Types.EQUALS)
                ],
                autoLoad : true,
                listeners : {
                    scope : this,
                    load : function(store, records)
                    {
                        // from r44407, multi valued fields come back as arrays. the LABKEY.ext.Store concats this back together
                        // so use the json displayValue (which is a comma separate list of the values) instead
                        Ext.each(records, function(record)
                        {
                            record.set('Analytes/RowId', record.json['Analytes/RowId'].displayValue);
                            record.set(this.ITEM_RECORD_KEY, record.get('DataId') + '|' + record.get('Description') + '|' + record.get('Dilution'))
                        }, this);

                        this.getDistinctItemGridStore().load();
                    },
                    distinctitemgridloaded : function()
                    {
                        var records = this.exclusionsStore.data.items;
                        var id;
                        for (var i = 0; i < records.length; i++)
                        {
                            var analyteRowIds = records[i].get('Analytes/RowId');
                            id = this.getCombinedItemAnalytesStore().findExact(this.ITEM_RECORD_KEY, records[i].get(this.ITEM_RECORD_KEY));
                            this.preAnalyteRowIds[id] = analyteRowIds;
                            this.preExcludedIds[id] = ("" + analyteRowIds).split(",");
                            this.comments[id] = records[i].get('Comment');
                        }
                    }
                }
            });
        }

        return this.exclusionsStore;
    },

    getDistinctItemGridStore : function()
    {
        if (!this.distinctItemGridStore)
        {
            this.distinctItemGridStore = new LABKEY.ext.Store({
                schemaName: this.protocolSchemaName,
                sql: 'SELECT DISTINCT (CONVERT(x.Data.RowId, VARCHAR) || \'|\' || x.Description || \'|\' || CONVERT(x.Dilution, VARCHAR)) AS SinglepointKey, '
                    + 'x.Description, x.Dilution, x.Data.RowId AS DataId, x.Data.Run.RowId AS RunId '
                    + 'FROM Data AS x WHERE x.Description IS NOT NULL AND x.Dilution IS NOT NULL AND x.Titration IS NULL AND x.WellRole = \'Unknown\' '
                    + 'AND x.Data.Run.RowId = ' + this.runId,
                sort: 'Description,Dilution',
                listeners: {
                    scope: this,
                    load: function (store, records)
                    {
                        var id;
                        for (var i = 0; i < this.getExclusionsStore().getCount(); i++)
                        {
                            id = store.findExact(this.ITEM_RECORD_KEY, this.getExclusionsStore().getAt(i).get(this.ITEM_RECORD_KEY));
                            if (id >= 0)
                            {
                                // coerce to string so that we can attempt to split by comma and space
                                var analyteRowIds = "" + this.getExclusionsStore().getAt(i).get("Analytes/RowId");

                                this.present[id] = analyteRowIds.split(",").length;
                                this.getExclusionsStore().getAt(i).set('Present', this.present[id]);
                                this.getExclusionsStore().getAt(i).commit();
                            }
                        }

                        var gridData = [];
                        for (i = 0; i < records.length; i++)
                        {
                            gridData[i] = [];
                            for (var index in records[i].data)
                                gridData[i].push(records[i].get(index));
                            gridData[i].push(this.present[i]);
                        }
                        this.getCombinedItemAnalytesStore().loadData(gridData);

                        this.getExclusionsStore().fireEvent('distinctitemgridloaded');

                    }
                }
            });
        }

        return this.distinctItemGridStore;
    },

    getCombinedItemAnalytesStore : function()
    {
        if (!this.combinedItemAnalytesStore)
        {
            this.combinedItemAnalytesStore = new Ext.data.ArrayStore({
                fields : [this.ITEM_RECORD_KEY, 'Description', 'Dilution', 'DataId', 'RunId', 'Present']
            });
        }

        return this.combinedItemAnalytesStore;
    },

    getAvailableItemsGrid : function()
    {
        if (!this.availableItemsGrid)
        {
            this.availableItemsGrid = new Ext.grid.GridPanel({
                style: 'padding: 10px 0;',
                title: "Select a " + this.DISPLAY_NOUN.toLowerCase() + " to view a list of available analytes.",
                headerStyle: 'font-weight: normal;',
                store: this.getCombinedItemAnalytesStore(),
                colModel: new Ext.grid.ColumnModel({
                    columns: [
                        {
                            header : 'Description',
                            dataIndex : 'Description'
                        },
                        {
                            header : 'Dilution',
                            dataIndex : 'Dilution'
                        },
                        {
                            xtype : 'templatecolumn',
                            header : 'Exclusions',
                            tpl : this.getExclusionsColumnTemplate()
                        }
                    ]
                }),
                viewConfig: {
                    forceFit: true
                },
                sm: this.getItemRowSelectionModel(),
                anchor: '100%',
                height: 165,
                frame: false,
                loadMask: true
            });
        }

        return this.availableItemsGrid;
    },

    getExclusionsColumnTemplate : function()
    {
        if (!this.exclusionsTpl)
        {
            this.exclusionsTpl = new Ext.XTemplate(
                '<span>{[this.getPresentValue(values)]}</span>',
                {
                    getPresentValue: function (values)
                    {
                        var x = values['Present'];
                        return (x != '') ? x + ' analyte' + (x == 1 ? '' : 's') + ' excluded' : '';
                    }
                }
            );
        }

        return this.exclusionsTpl;
    },

    getItemRowSelectionModel : function()
    {
        if (!this.itemRowSelectionModel)
        {
            this.itemRowSelectionModel = new Ext.grid.RowSelectionModel({
                singleSelect : true,
                header : 'Singlepoint Unknown',
                listeners : {
                    scope : this,
                    rowdeselect : function(tsl, rowId, record)
                    {
                        this.excluded[rowId] = this.getGridCheckboxSelModel().getSelections();
                        this.excluded[rowId][this.ITEM_RECORD_KEY] = record.get(this.ITEM_RECORD_KEY);
                        this.excluded[rowId]['Name'] = record.get('Description');
                        this.excluded[rowId]['Dilution'] = record.get('Dilution');

                        this.comments[rowId] = Ext.getCmp('comment').getValue();
                        this.present[rowId] = this.getGridCheckboxSelModel().getSelections().length;
                        record.set('Present', this.present[rowId]);
                        this.excludedDataIds[rowId] = record.get('DataId');
                    },
                    rowselect : function(tsl, rowId, record)
                    {
                        this.getAvailableAnalytesGrid().getStore().clearFilter();
                        this.getAvailableAnalytesGrid().getStore().filter({property: this.ITEM_RECORD_KEY, value: record.get(this.ITEM_RECORD_KEY), exactMatch: true});
                        this.getAvailableAnalytesGrid().setDisabled(false);

                        this.getGridCheckboxSelModel().suspendEvents(false);
                        if (typeof this.preExcludedIds[rowId] === 'object')
                        {
                            this.getGridCheckboxSelModel().clearSelections();
                            Ext.each(this.preExcludedIds[rowId], function(analyte)
                            {
                                var index = this.getAvailableAnalytesGrid().getStore().findBy(function(rec, id)
                                {
                                    return rec.get(this.ITEM_RECORD_KEY) == record.get(this.ITEM_RECORD_KEY) && rec.get('RowId') == analyte;
                                }, this);
                                this.getAvailableAnalytesGrid().getSelectionModel().selectRow(index, true);
                            }, this);
                            var id = this.getExclusionsStore().findExact(this.ITEM_RECORD_KEY, record.get(this.ITEM_RECORD_KEY));
                            this.preExcludedIds[rowId] = this.getExclusionsStore().getAt(id).get('RowId');
                            this.exclusionsExist = true;
                        }
                        else if (this.excluded[rowId])
                        {
                            this.getGridCheckboxSelModel().selectRecords(this.excluded[rowId], false);
                            this.exclusionsExist = true;
                        }
                        else
                        {
                            this.getGridCheckboxSelModel().clearSelections();
                            this.exclusionsExist = false;
                        }
                        this.getGridCheckboxSelModel().resumeEvents();

                        if (this.comments[rowId])
                            Ext.getCmp('comment').setValue(this.comments[rowId]);
                        else
                            Ext.getCmp('comment').setValue('');
                    }
                }
            });
        }

        return this.itemRowSelectionModel;
    },

    getGridCheckboxSelModel : function()
    {
        if (!this.gridCheckboxSelModel)
        {
            this.gridCheckboxSelModel = this.getGridCheckboxSelectionModel();
        }

        return this.gridCheckboxSelModel;
    },

    getAvailableAnalytesGrid : function()
    {
        if (!this.availableAnalytesGrid)
        {
            this.availableAnalytesGrid = new Ext.grid.GridPanel({
                title: "For the selected " + this.DISPLAY_NOUN.toLowerCase() + ", select the checkbox next to the analyte(s) to be excluded:",
                headerStyle: 'font-weight: normal;',
                store: new LABKEY.ext.Store({
                    schemaName: this.protocolSchemaName,
                    sql: "SELECT DISTINCT (CONVERT(x.Data.RowId, VARCHAR) || \'|\' || x.Description || \'|\' || CONVERT(x.Dilution, VARCHAR)) AS SinglepointKey, "
                        + "x.Description, x.Dilution, x.Data.RowId AS DataId, x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                        + " FROM Data AS x WHERE x.Description IS NOT NULL AND x.Dilution IS NOT NULL AND x.Titration IS NULL AND x.WellRole = \'Unknown\'"
                        + " AND x.Data.Run.RowId = " + this.runId,
                    sort: 'Description,Dilution,Name',
                    autoLoad: true
                }),
                colModel: new Ext.grid.ColumnModel({
                    defaults: {
                        sortable: false,
                        menuDisabled: true
                    },
                    columns: [
                        this.getGridCheckboxSelModel(),
                        {header: 'Analyte Name', dataIndex: 'Name'},
                        {header: this.ITEM_RECORD_KEY, dataIndex: this.ITEM_RECORD_KEY, hidden: true}
                    ]
                }),
                viewConfig: {
                    forceFit: true
                },
                sm: this.getGridCheckboxSelModel(),
                anchor: '100%',
                height: 165,
                frame: false,
                disabled: true,
                loadMask: true
            });
        }

        return this.availableAnalytesGrid;
    },

    toggleSaveBtn : function(sm, grid)
    {
        grid.getFooterToolbar().findById('saveBtn').enable();
    },

    insertUpdateExclusions: function()
    {
        var index = this.getAvailableItemsGrid().getStore().indexOf(this.getItemRowSelectionModel().getSelected());
        this.getItemRowSelectionModel().fireEvent('rowdeselect', this.getItemRowSelectionModel(), index, this.getItemRowSelectionModel().getSelected());
        this.openConfirmWindow();
    },

    getExcludedString : function()
    {
        var retString = '';

        for (var i = 0; i < this.present.length; i++)
        {
            // issue 21431
            if (this.present[i] == undefined)
                continue;

            if (!(this.preExcludedIds[i] == undefined && this.present[i] == 0))
            {
                var noun = 'analyte' + (this.present[i] != 1 ? 's' : '');
                var record = this.getAvailableItemsGrid().getStore().getAt(i);
                retString += this.getExcludedStringKey(record) + ': ' + this.present[i] + ' ' + noun + ' excluded.<br>';
            }
        }
        return retString;
    },

    getExcludedStringKey : function(record)
    {
        return record.get('Description') + ' (Dilution: ' + record.get('Dilution') + ')';
    },

    openConfirmWindow : function()
    {
        var excludedMessage = this.getExcludedString();
        if (excludedMessage == '')
        {
            this.fireEvent('closeWindow');
        }
        else
        {
            Ext.Msg.show({
                title:'Confirm Exclusions',
                msg: 'Please verify the excluded analytes for the following ' + this.DISPLAY_NOUN.toLowerCase() + 's. '
                        + 'Continue?<br><br> ' + excludedMessage,
                buttons: Ext.Msg.YESNO,
                fn: function(button)
                {
                    if (button == 'yes')
                        this.insertUpdateExclusionsConfirmed();
                },
                icon: Ext.MessageBox.QUESTION,
                scope : this
            });
        }
    },

    insertUpdateExclusionsConfirmed : function()
    {
        this.mask("Saving " + this.DISPLAY_NOUN.toLowerCase() + " exclusions...");

        var commands = [];
        for (var index = 0; index < this.excluded.length; index++)
        {
            var dataId = this.excludedDataIds[index];
            var analytesForExclusion = this.excluded[index];
            if (analytesForExclusion == undefined)
                continue;

            // generate a comma delim string of the analyte Ids to exclude
            var analyteRowIds = "";
            var analyteNames = "";
            var sep = "";
            Ext.each(analytesForExclusion, function(record)
            {
                analyteRowIds += sep.trim() + record.get('RowId');
                analyteNames += sep + record.get('Name');
                sep = ", ";
            });

            // determine if this is an insert, update, or delete
            var command = "insert";
            if (this.preExcludedIds[index] != undefined)
                command = analyteRowIds != "" ? "update" : "delete";

            // issue 21551: don't insert an exclusion w/out any analytes
            if (command == "insert" && analyteRowIds == "")
                continue;

            // config of data to save for a single titration exclusion
            var commandConfig = {
                command: command,
                key: this.preExcludedIds[index], // this will be undefined for the insert case
                dataId: dataId,
                description: analytesForExclusion['Name'],
                analyteRowIds: (analyteRowIds != "" ? analyteRowIds : null),
                analyteNames: (analyteNames != "" ? analyteNames : null), // for logging purposes only
                comment: this.comments[index]
            };

            if (this.EXCLUSION_INCLUDES_DILUTION)
                commandConfig.dilution = analytesForExclusion['Dilution'];

            commands.push(commandConfig);
        }

        if (commands.length > 0)
        {
            var config = {
                assayId: this.assayId,
                tableName: this.EXCLUSION_TABLE_NAME,
                runId: this.runId,
                commands: commands
            };

            this.saveExclusions(config, this.DISPLAY_NOUN.toLowerCase());
        }
        else
        {
            this.unmask();
            this.fireEvent('closeWindow');
        }
    }
});