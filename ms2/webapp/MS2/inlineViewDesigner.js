/*
 * Copyright (c) 2011-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
+function() {
    var viewDesigners = {};

    window.showViewDesigner = function(queryName, renderTo, viewSelectId, saveCallback) {

        var LOAD_IN_PROGRESS = "LOAD_IN_PROGRESS";

        if (viewDesigners[viewSelectId]) {
            if (viewDesigners[viewSelectId] === LOAD_IN_PROGRESS) {
                return;
            }
            viewDesigners[viewSelectId].getEl().remove();
            viewDesigners[viewSelectId] = undefined;
            return;
        }
        viewDesigners[viewSelectId] = LOAD_IN_PROGRESS;

        if (!saveCallback) {
            saveCallback = function() { window.location.reload(); };
        }

        LABKEY.DataRegion.loadViewDesigner(function() {

            var viewName = viewSelectId == null || viewSelectId == '' ? null : document.getElementById(viewSelectId).value;
            LABKEY.Query.getQueryDetails({
                schemaName: 'ms2',
                queryName: queryName,
                viewName: viewName,
                success: function(json) {
                    viewDesigners[viewSelectId] = Ext4.create('LABKEY.internal.ViewDesigner.Designer', {
                        renderTo: renderTo,
                        schemaName: 'ms2',
                        queryName: queryName,
                        viewName: viewName,
                        query: json,
                        allowableContainerFilters: [['Current', 'Current Folder'], ['CurrentAndSubfolders', 'Current folder and subfolders']],
                        includeRevert: false,
                        includeViewGrid: false,
                        width: 700,
                        activeTab: 1,
                        listeners: {
                            viewsave: saveCallback,
                            scope: this
                        }
                    });
                }
            });
        }, this);
    }
}();
