/*
 * Copyright (c) 2012-2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('LABKEY.elisa.RunDataPanel', {

    extend : 'Ext.panel.Panel',

    constructor : function(config) {

        Ext4.applyIf(config, {
            layout  : 'auto',
            border  : false,
            frame   : false,
            autoScroll : true,
            cls     : 'iScroll'
        });

        this.callParent([config]);
    },

    initComponent : function() {

        this.items = [
                this.createDataView()
        ];

        this.callParent([arguments]);
    },

    createDataView : function() {

        var dataGrid = Ext4.create('Ext.Component', {
            autoScroll  : true,
            cls         : 'iScroll',
            listeners   : {
                render : {fn : function(cmp){this.renderDataGrid(cmp.getId());}, scope : this}
            }
        });

        return Ext4.create('Ext.panel.Panel', {
            flex        : 1.2,
            layout      : 'fit',
            padding     : '20, 0',
            height      : 650,
            border      : false,
            frame       : false,
            items       : dataGrid
        });
    },

    renderDataGrid : function(renderTo) {

        var sampleColumns = '';

        for (var i=0; i < this.sampleColumns.length; i++)
        {
            var name = this.sampleColumns[i];
            sampleColumns = sampleColumns.concat(' MAX(SpecimenLsid.Property.' + name + ') AS ' + name + ',');
        }

        var sql = 'SELECT ' + sampleColumns +
                'Data.WellgroupLocation,' +
                "(ROUND(AVG(Concentration), 3)) AS Concentration," +
                '(ROUND(AVG(Absorption), 3)) AS Absorption ' +
                'FROM Data GROUP BY WellgroupLocation ORDER BY WellgroupLocation';

        LABKEY.Query.executeSql({
                schemaName: this.schemaName,
                sql: sql,
                saveInSession : true,
                maxRows : 0,
                scope   : this,
                success : function(r) {

                    var wp = new LABKEY.QueryWebPart({
                        schemaName  : this.schemaName,
                        queryName   : r.queryName,
                        frame       : 'none',
                        renderTo    : renderTo,
                        showDetailsColumn       : false,
                        showUpdateColumn        : false,
                        showRecordSelectors     : false
/*
                        buttonBar   : {
                            position : 'none'
                        }
*/
                    });

                    wp.render(renderTo);
                }
        });
    }
});
