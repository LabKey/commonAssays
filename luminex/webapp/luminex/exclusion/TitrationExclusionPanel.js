/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext.namespace('LABKEY');

// function called onclick of 'Exclude Titrations' menu button to open the exclusion window
function openExclusionsTitrationWindow(assayId, runId)
{
    // lookup the assay design information based on the Assay RowId
    LABKEY.Assay.getById({
        id: assayId,
        success: function(assay)
        {
            if (Ext.isArray(assay) && assay.length == 1)
            {
                var win = new LABKEY.Exclusions.BaseWindow({
                    title: 'Exclude Titrations from Analysis',
                    width: Ext.getBody().getViewSize().width < 500 ? Ext.getBody().getViewSize().width * .9 : 450,
                    height: Ext.getBody().getViewSize().height > 700 ? 600 : Ext.getBody().getViewSize().height * .75,
                    items: new LABKEY.Exclusions.TitrationPanel({
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
 * Class to display panel for selecting which titrations to exclude from a Luminex run
 * @params protocolSchemaName = the encoded protocol schema name to use (based on the assay design name)
 * @params assayId = the assay design RowId
 * @params runId = runId for the selected replicate group
 */
LABKEY.Exclusions.TitrationPanel = Ext.extend(LABKEY.Exclusions.SinglepointUnknownPanel, {

    DISPLAY_NOUN : 'Titration',

    HEADER_TXT : 'Analytes excluded for a well, replicate group, singlepoint unknown, or at the assay level will not be '
                    + 're-included by changes in titration exclusions.',

    ITEM_RECORD_KEY : 'Name',

    EXCLUSION_TABLE_NAME : 'TitrationExclusion',

    EXCLUSION_INCLUDES_DILUTION : false,

    getExclusionsStore : function()
    {
        if (!this.exclusionsStore)
        {
            this.exclusionsStore = new LABKEY.ext.Store({
                schemaName: this.protocolSchemaName,
                queryName: this.EXCLUSION_TABLE_NAME,
                columns: 'Description,Analytes/RowId,RowId,Comment,DataId,DataId/Run',
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
                        });

                        this.getDistinctItemGridStore().load();
                    },
                    distinctitemgridloaded : function()
                    {
                        var records = this.exclusionsStore.data.items;
                        for (var i = 0; i < records.length; i++)
                        {
                            var analyteRowIds = records[i].get('Analytes/RowId');
                            var idx = this.getCombinedItemAnalytesStore().findBy(function(rec) {
                                return rec.get(this.ITEM_RECORD_KEY) === records[i].get('Description')
                                        && rec.get('DataId') === records[i].get('DataId');
                            }, this);
                            this.preAnalyteRowIds[idx] = analyteRowIds;
                            this.preExcludedIds[idx] = ("" + analyteRowIds).split(",");
                            this.comments[idx] = records[i].get('Comment');
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
                sql : 'SELECT DISTINCT x.Titration.Name, x.Data.RowId AS DataId, x.Data.Run.RowId AS RunId '
                    + 'FROM Data AS x WHERE x.Titration IS NOT NULL AND x.Titration.Standard != true '
                    + 'AND x.Data.Run.RowId = ' + this.runId,
                sort: this.ITEM_RECORD_KEY,
                listeners : {
                    scope : this,
                    load : function(store, records)
                    {
                        for (var i = 0; i < this.getExclusionsStore().getCount(); i++)
                        {
                            var descVal = this.getExclusionsStore().getAt(i).get('Description');
                            var dataIdVal = this.getExclusionsStore().getAt(i).get('DataId');
                            var idx = store.findBy(function(rec) {
                                return rec.get(this.ITEM_RECORD_KEY) === descVal && rec.get('DataId') === dataIdVal;
                            }, this);
                            if (idx >= 0)
                            {
                                // coerce to string so that we can attempt to split by comma and space
                                var analyteRowIds = "" + this.getExclusionsStore().getAt(i).get("Analytes/RowId");

                                this.present[idx] = analyteRowIds.split(",").length;
                                this.getExclusionsStore().getAt(i).set('Present', this.present[idx]);
                                this.getExclusionsStore().getAt(i).commit();
                            }
                        }

                        var gridData = [];
                        var distinctTitrationNames = [];
                        for (i = 0; i < records.length; i++)
                        {
                            gridData[i] = [];
                            for (var index in records[i].data)
                                gridData[i].push(records[i].get(index));
                            gridData[i].push(this.present[i]);

                            // Issue 51084
                            var titrationName = records[i].get(this.ITEM_RECORD_KEY);
                            if (distinctTitrationNames.indexOf(titrationName) === -1) {
                                distinctTitrationNames.push(titrationName);
                                gridData[i].push(true); // Visible
                            } else {
                                gridData[i].push(false); // Hidden
                            }
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
                fields : [this.ITEM_RECORD_KEY, 'DataId', 'RunId', 'Present', 'Visible']
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
                title: "Select a " + this.DISPLAY_NOUN.toLowerCase() + " to view a list of available analytes:",
                headerStyle: 'font-weight: normal;',
                store: this.getCombinedItemAnalytesStore(),
                colModel: new Ext.grid.ColumnModel({
                    columns: [
                        this.getItemRowSelectionModel(),
                        {
                            xtype : 'templatecolumn',
                            header : 'Exclusions',
                            tpl : this.getExclusionsColumnTemplate()
                        }
                    ]
                }),
                autoExpandColumn: this.ITEM_RECORD_KEY,
                viewConfig: {
                    forceFit: true,
                    getRowClass: function(record, index) {
                        return record.get('Visible') ? 'visible' : 'hidden';
                    }
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

    getItemRowSelectionModel : function()
    {
        if (!this.itemRowSelectionModel)
        {
            this.itemRowSelectionModel = new Ext.grid.RowSelectionModel({
                singleSelect : true,
                header : 'Titration',
                listeners : {
                    scope : this,
                    rowdeselect : function(tsl, _, record)
                    {
                        var titrationName = record.get(this.ITEM_RECORD_KEY);
                        var gridSelections = this.getGridCheckboxSelModel().getSelections();
                        var comment = Ext.getCmp('comment').getValue();

                        // Issue 51084: update the excluded analytes for all record indices matching this titration Name
                        this.getCombinedItemAnalytesStore().getRange().forEach(function(rec, rowIndex) {
                            if (rec.get(this.ITEM_RECORD_KEY) === titrationName) {
                                this.excluded[rowIndex] = gridSelections;
                                this.excluded[rowIndex][this.ITEM_RECORD_KEY] = titrationName;
                                this.comments[rowIndex] = comment;
                                this.present[rowIndex] = gridSelections.length;
                                rec.set('Present', this.present[rowIndex]);

                                // convert the preExcludedIds from a listing of excluded analytes to the RowId of the exclusion record
                                if (typeof this.preExcludedIds[rowIndex] === 'object') {
                                    var idx = this.getExclusionsStore().findBy(function(rec2) {
                                        return rec2.get('Description') === rec.get(this.ITEM_RECORD_KEY)
                                                && rec2.get('DataId') === rec.get('DataId');
                                    }, this);
                                    this.preExcludedIds[rowIndex] = this.getExclusionsStore().getAt(idx).get('RowId');
                                }
                            }
                        }, this);
                    },
                    rowselect : function(tsl, rowIndex, record)
                    {
                        this.getAvailableAnalytesGrid().getStore().clearFilter();
                        this.getAvailableAnalytesGrid().getStore().filter({property: 'Titration', value: record.get(this.ITEM_RECORD_KEY), exactMatch: true});
                        this.getAvailableAnalytesGrid().setDisabled(false);

                        this.getGridCheckboxSelModel().suspendEvents(false);
                        if (typeof this.preExcludedIds[rowIndex] === 'object')
                        {
                            this.getGridCheckboxSelModel().clearSelections();
                            Ext.each(this.preExcludedIds[rowIndex], function(analyte) {
                                var index = this.getAvailableAnalytesGrid().getStore().findBy(function(rec, id) {
                                    return rec.get('Titration') == record.get(this.ITEM_RECORD_KEY) && rec.get('RowId') == analyte;
                                }, this);
                                this.getAvailableAnalytesGrid().getSelectionModel().selectRow(index, true);
                            }, this);
                            this.exclusionsExist = true;

                            // convert the preExcludedIds from a listing of excluded analytes to the RowId of the exclusion record
                            var idx = this.getExclusionsStore().findBy(function(rec) {
                                return rec.get('Description') === record.get(this.ITEM_RECORD_KEY)
                                        && rec.get('DataId') === record.get('DataId');
                            }, this);
                            this.preExcludedIds[rowIndex] = this.getExclusionsStore().getAt(idx).get('RowId');
                        }
                        else if (this.excluded[rowIndex])
                        {
                            this.getGridCheckboxSelModel().selectRecords(this.excluded[rowIndex], false);
                            this.exclusionsExist = true;
                        }
                        else
                        {
                            this.getGridCheckboxSelModel().clearSelections();
                            this.exclusionsExist = false;
                        }
                        this.getGridCheckboxSelModel().resumeEvents();

                        if (this.comments[rowIndex])
                            Ext.getCmp('comment').setValue(this.comments[rowIndex]);
                        else
                            Ext.getCmp('comment').setValue('');
                    }
                }

            });
        }

        return this.itemRowSelectionModel;
    },

    getAvailableAnalytesGrid : function()
    {
        if (!this.availableAnalytesGrid)
        {
            this.availableAnalytesGrid = new Ext.grid.GridPanel({
                title: "For the selected " + this.DISPLAY_NOUN.toLowerCase() + ", select the checkbox next to the analyte(s) to be excluded:",
                headerStyle: 'font-weight: normal;',
                store:  new LABKEY.ext.Store({
                    schemaName: this.protocolSchemaName,
                    sql: "SELECT DISTINCT x.Titration.Name AS Titration, x.Analyte.RowId AS RowId, x.Analyte.Name AS Name "
                        + " FROM Data AS x WHERE x.Titration IS NOT NULL AND x.Data.Run.RowId = " + this.runId,
                    sort: 'Titration,Name',
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
                        {header: 'Titration', dataIndex: 'Titration', hidden: true}
                    ]
                }),
                autoExpandColumn: 'Name',
                viewConfig: {
                    forceFit: true
                },
                sm: this.getGridCheckboxSelModel(),
                anchor: '100%',
                height: 165,
                frame: false,
                disabled : true,
                loadMask: true
            });
        }

        return this.availableAnalytesGrid;
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
                if (record.get('Visible')) {
                    retString += this.getExcludedStringKey(record) + ': ' + this.present[i] + ' ' + noun + ' excluded.<br>';
                }
            }
        }
        return retString;
    },

    getExcludedStringKey : function(record)
    {
        return record.get(this.ITEM_RECORD_KEY);
    }
});