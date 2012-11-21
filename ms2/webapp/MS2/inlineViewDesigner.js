/*
 * Copyright (c) 2011-2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var viewDesigners = {};

var LOAD_IN_PROGRESS = "LOAD_IN_PROGRESS";

function showViewDesigner(queryName, renderTo, viewSelectId, saveCallback)
{
    if (viewDesigners[viewSelectId])
    {
        if (viewDesigners[viewSelectId] === LOAD_IN_PROGRESS)
        {
            return;
        }
        viewDesigners[viewSelectId].getEl().remove();
        viewDesigners[viewSelectId] = undefined;
        return;
    }
    viewDesigners[viewSelectId] = LOAD_IN_PROGRESS;

    if (!saveCallback)
    {
        saveCallback = function()
        {
            window.location.reload();
        };
    }

    LABKEY.initializeViewDesigner(function ()
    {
        var viewName = viewSelectId == null || viewSelectId == '' ? null : document.getElementById(viewSelectId).value;
        LABKEY.Query.getQueryDetails(
        {
            schemaName: "ms2",
            queryName: queryName,
            viewName: viewName,
            success: function (json, response, options) {

                this.customizeView = new LABKEY.DataRegion.ViewDesigner({
                    renderTo: renderTo,
                    width: 700,
                    activeGroup: 1,
                    dataRegion: null,
                    schemaName: "ms2",
                    queryName: queryName,
                    viewName: viewName,
                    allowableContainerFilters: [['Current', 'Current Folder'], ['CurrentAndSubfolders', 'Current folder and subfolders']],
                    includeRevert: false,
                    includeViewGrid: false,
                    query: json
                });

                viewDesigners[viewSelectId] = this.customizeView;

                this.customizeView.on("viewsave", saveCallback, this);
                // Need to trigger a relayout that makes the split pane visible
                this.customizeView.setWidth(this.customizeView.getWidth());
            }
        }, this);
    });
}
