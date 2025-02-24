/*
 * Copyright (c) 2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
//TODO: this should be renamed
Ext4.define('LABKEY.SignalData.SampleCreator', {
    extend: 'Ext.panel.Panel',

    title: 'SignalData Qualitative Analysis',

    layout: {
        type: 'border',
        regionWeights: {
            west: 20,
            north: 10,
            south: -10,
            east: 20
        }
    },

    border: false,

    initComponent: function() {

        this.items = [
            this.getWest(),
            this.getNorth(),
            this.getCenter(),
            this.getEast()
        ];

        this.callParent();

        this.curveTask = new Ext4.util.DelayedTask(function() {
            var xleft = Ext4.getCmp('aucleft').getValue();
            var xright = Ext4.getCmp('aucright').getValue();
            this.fireEvent('curvechange', xleft, xright);
        }, this);

        this.rangeTask = new Ext4.util.DelayedTask(function() {
            var low = Ext4.getCmp('mvrangelow').getValue();
            var high = Ext4.getCmp('mvrangehigh').getValue();
            this.fireEvent('rangechange', low, high);
        }, this);
    },

    getWest : function() {

        if (!this.westpanel) {
            this.westpanel = Ext4.create('Ext.panel.Panel', {
                region: 'west',
                title: 'Available Inputs',
                header: false,
                id: 'sampleinputs',
                width: 250,
                border: false, frame: false,
                style: 'border-right: 1px solid lightgrey; overflow-x: hidden; overflow-y: auto;',
                bodyStyle: 'overflow-y: auto;',
                items: [{
                    itemId: 'inputsgrid',
                    xtype: 'grid',
                    store: {
                        xtype: 'store',
                        model: 'LABKEY.SignalData.ProvisionalRun',
                        data: this.context.rawInputs
                    },
                    columns: [
                        {text: 'Inputs', dataIndex: 'name', width: 205}
                    ],
                    selModel: {
                        selType: 'checkboxmodel',
                        mode: 'MULTI'
                    },
                    hideHeaders: true,
                    listeners: {
                        viewready : function(g) {
                            //
                            // Filter to remove PRE_, POST_, and BLANK tags
                            //
                            g.getStore().filter([{
                                filterFn: function(item) {
                                    return item.get('name').indexOf('PRE_') == -1 && item.get('name').indexOf('POST_') == -1;
                                }
                            },{
                                filterFn: function(item) {
                                    return item.get('name').indexOf('BLANK') == -1;
                                }
                            }]);
                        },
                        selectionchange: function(g, recs) {
                            this.fireEvent('inputchange', recs);
                            Ext4.getCmp('startqcbtn').setDisabled(recs.length == 0);
                        },
                        scope: this
                    }
                }],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [{
                        id: 'startqcbtn',
                        text: 'Overlay Selected',
                        disabled: true,
                        handler: function(b) {
                            this.fireEvent('startqc', this.getSelectedInputResults());
                        },
                        scope: this
                    }]
                }],
                scope: this
            });

            this.on('startqc', function() {
                this.updateZoom(0, 30, 0, 1200);
                this.westpanel.collapse();
            }, this);
        }

        return this.westpanel;
    },

    getNorth : function() {

        if (!this.northpanel) {
            var xLabel = this.getXLabel();
            var yLabel = this.getYLabel();

            this.northpanel = Ext4.create('Ext.panel.Panel', {
                region: 'north',
                height: 120,
                items: [{
                    xtype: 'panel',
                    columnWidth: 0.5,
                    border: false, frame: false,
                    items: [{
                        id: 'sampleform',
                        itemId: 'sampleform',
                        xtype: 'form',
                        border: false, frame: false,
                        padding: '15 10',
                        fieldDefaults: {
                            labelWidth: 150
                        },
                        items: [{
                            xtype: 'fieldcontainer',
                            fieldLabel: xLabel,
                            layout: 'hbox',
                            width: 300,
                            items: [{
                                id: 'aucleft',
                                name: 'aucleft',
                                xtype: 'numberfield',
                                hideTrigger: true,
                                emptyText: 'left',
                                value: 0,
                                flex: 1,
                                listeners: {
                                    change: function() {
                                        this.curveTask.delay(300);
                                    },
                                    scope: this
                                }
                            },{
                                xtype: 'splitter'
                            },{
                                id: 'aucright',
                                name: 'aucright',
                                xtype: 'numberfield',
                                hideTrigger: true,
                                emptyText: 'right',
                                value: 30,
                                flex: 1,
                                listeners: {
                                    change: function() {
                                        this.curveTask.delay(300);
                                    },
                                    scope: this
                                }
                            }]
                        },{
                            xtype: 'fieldcontainer',
                            fieldLabel: yLabel,
                            layout: 'hbox',
                            width: 300,
                            items: [{
                                id: 'mvrangelow',
                                xtype: 'numberfield',
                                hideTrigger: true,
                                emptyText: 'left',
                                value: 0,
                                flex: 1,
                                listeners: {
                                    change: function() {
                                        this.rangeTask.delay(300);
                                    },
                                    scope: this
                                }
                            },{
                                xtype: 'splitter'
                            },{
                                id: 'mvrangehigh',
                                xtype: 'numberfield',
                                hideTrigger: true,
                                emptyText: 'right',
                                value: 1200,
                                flex: 1,
                                listeners: {
                                    change: function() {
                                        this.rangeTask.delay(300);
                                    },
                                    scope: this
                                }
                            }]
                        }]
                    }]
                }]
            });


            this.on('startqc', function(runs) {
                //
                // clear the form fields
                //
                this.getQCForm().getForm().reset();
            }, this);
        }
        return this.northpanel;
    },

    getInputsSelectionModel : function() {
        return this.getWest().getComponent('inputsgrid').getSelectionModel();
    },

    getSelectedInputResults : function() {
        return this.getInputsSelectionModel().getSelection();
    },

    getQCForm : function() {
        return Ext4.getCmp('sampleform');
    },

    getCenter : function() {

        if (!this.centerpanel) {
            var xLabel = this.getXLabel();
            var yLabel = this.getYLabel();

            this.centerpanel = Ext4.create('Ext.panel.Panel', {
                region: 'center',
                border: false, frame: false,
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [{
                        text: 'Reset Zoom',
                        handler: function() {
                            this.updateZoom(0, 30, 0, 1200);
                        },
                        scope: this
                    }]
                }],
                items: [{
                    id: 'plotarea',
                    xtype: 'spectrum',
                    xLabel: xLabel,
                    yLabel: yLabel,
                    leftBoundField: 'aucleft',
                    rightBoundField: 'aucright',
                    lowBoundField: 'mvrangelow',
                    highBoundField: 'mvrangehigh',
                    listeners: {
                        zoom: this.updateZoom,
                        scope: this
                    }
                }]
            });

            this.on('startqc', function(provisionalRuns) {
                //
                // load the appropriate content for each selected sample
                //
                var received = 0,
                    expected = provisionalRuns.length,
                    allContent = [],
                    contentMap = {};

                var done = function(content, pr) {
                    received++;
                    content.runName = pr.runName;
                    allContent.push(content);
                    contentMap[pr.runName] = content;
                    if (received == expected) {
                        this.allContent = allContent;
                        this.contentMap = contentMap;
                        this.renderPlot(allContent, true);
                    }
                };

                Ext4.each(provisionalRuns, function(run){
                    var pr = run.get('expDataRun');
                    if (pr) {
                        // use the run name as the key for all data
                        pr.runName = run.get('name');
                        SignalDataService.FileContentCache(pr, done, this);
                    }
                    else {
                        console.error('Failed to load expDataRun from provisional run.');
                    }
                }, this);

            }, this);
        }

        return this.centerpanel;
    },

    getXLabel: function(){
        return this.getAxisLabel('XAxis');
    },
    getYLabel: function(){
        return this.getAxisLabel('YAxis');
    },

    getAxisLabel: function(axisField) {
        var assay = this.context.AssayDefinition;
        if (assay && assay.domains && assay.domains[assay.name + ' Batch Fields']) {
            var batchFields = assay.domains[assay.name + ' Batch Fields'];
            for(var i=0; i< batchFields.length;i++) {
                var field = batchFields[i];
                if (field.fieldKey === axisField) {
                    return field.defaultValue;
                }
            }
        }
    },

    getEast : function() {

        if (!this.eastpanel) {

            var view = Ext4.create('Ext.view.View', {
                store: {
                    xtype: 'store',
                    model: 'LABKEY.SignalData.Sample'
                },
                itemSelector: 'tr.item',
                autoScroll: true,
                tpl: new Ext4.XTemplate(
                    '<table style="width: 100%;">',
                        '<tr>',
                            '<th style="text-align: left;">Name</th>',
                            '<th style="text-align: left;">Left</th>',
                            '<th style="text-align: left;">Right</th>',
                            '<th style="text-align: left;">Base</th>',
                            '<th style="text-align: left;">Include</th>',
                            '<th style="text-align: left;">Response</th>',
                            '<th></th>',
                        '</tr>',
                        '<tpl for=".">',
                            '<tr class="item" modelname="{name}">',
                                '<td>{name}</td>',
                                '<td><input value="{xleft}" placeholder="xleft" name="xleft" style="width: 40px;"/></td>',
                                '<td><input value="{xright}" placeholder="xright" name="xright" style="width: 40px;"/></td>',
                                '<td><input value="{base}" name="base" style="width: 40px;"/></td>',
                                '<td><input value="{include}" name="include" type="checkbox" {include:this.renderChecked}/></td>',
                                '<td><span name="response">{peakResponse:this.renderPeakResponse}</span></td>',
                                '<td><button title="copy">C</button></td>',
                            '</tr>',
                        '</tpl>',
                    '</table>',
                        {
                            renderChecked : function(checked) {
                                var ret = '';
                                if (checked === true) {
                                    ret = ' checked="checked" ';
                                }
                                return ret;
                            },
                            renderPeakResponse : function(response) {
                                return response.toFixed(3);
                            }
                        }
                ),
                listeners: {
                    viewready: function(v) { this.qcresultview = v; },
                    itemclick: this.onQCResultItemSelect,
                    select: this.bindCalc,
                    scope: this
                }
            });

            this.eastpanel = Ext4.create('Ext.panel.Panel', {
                title: 'Basic integration',
                region: 'east',
                autoScroll: true,
                width: 400,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                items: [view],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [{
                       // text: 'Calculate',
                       // handler: this.runAnalysis,
                       // scope: this
                    // },{
                        text: 'Clear Highlight',
                        handler: function() {
                            this.highlighted = undefined;
                            this.renderPlot(this.allContent);
                        },
                        scope: this
                    }]
                }]
            });

            this.on('startqc', function(runs) {

                var _runs = [];
                for (var r=0; r < runs.length; r++) {
                    _runs.push(Ext4.clone(runs[r].data));
                }
                view.getStore().loadData(_runs);

            }, this);
            this.on('curvechange', function(xleft, xright) { this.renderPlot(this.allContent); }, this);
            this.on('rangechange', function(low, high) { this.renderPlot(this.allContent); }, this);
        }

        return this.eastpanel;
    },

    /**
     * Fires whenever a user selects any input for a give QC Result row. Primarily used to check if they clicked
     * 'C' meaning to copy that rows results as a convenience.
     */
    onQCResultItemSelect : function(view, model, z, a, evt) {
        if (evt.target && Ext4.isString(evt.target.tagName) && evt.target.tagName.toLowerCase() === "button") {
            this.updateModels();
        }
        else {
            // for some reason, focus is not maintained even after a user clicks an input
            Ext4.defer(function() { Ext4.get(evt.target).dom.focus(); }, 50);
        }
    },

    bindCalc : function(view, sample) {
        var modelname = sample.get('name');

        if (modelname) {
            this.highlighted = sample.get('name');
            this.renderPlot(this.allContent);
        }
    },

    updateModels : function(callback, scope, toCopy) {
        //
        // Iterate over all QC Results updating their peakResponse based on given values for that row/result
        //
        if (Ext4.isDefined(this.qcresultview)) {
            var view = this.qcresultview;
            var store = view.getStore();
            var models = store.getRange();

            var xleft, xright, base, include, response;

            Ext4.each(models, function(model) {

                if (Ext4.isDefined(toCopy)) {
                    xleft = toCopy.get('xleft');
                    xright = toCopy.get('xright');
                    base = toCopy.get('base');
                }
                else {
                    xleft = +this._getNodeValue(view, model, 'input[name="xleft"]');
                    xright = +this._getNodeValue(view, model, 'input[name="xright"]');
                    base = +this._getNodeValue(view, model, 'input[name="base"]');
                }

                include = this._getNode(view, model, 'input[name="include"]');
                response = this._getNode(view, model, 'span[name="response"]');

                var fileContent = this.contentMap[model.get('name')];
                var data = SignalDataService.getData(fileContent, xleft, xright, false);
                var aucPeak = LABKEY.SignalData.Stats.getAUC(data, base);
                response.update(+aucPeak.auc.toFixed(3));

                model.suspendEvents(true);
                model.set('xleft', xleft);
                model.set('xright', xright);
                model.set('base', base);
                model.set('auc', aucPeak.auc);
                model.set('peakResponse', aucPeak.auc);
                model.set('peakMax', aucPeak.peakMax);
                model.set('include', include.dom.checked);
                model.resumeEvents();
            }, this);

            if (Ext4.isFunction(callback)) {
                callback.call(scope);
            }
        }
        else {
            console.error('qcresultview not initialized before update.');
        }
    },

    _getNode : function(view, model, selector) {
        return Ext4.get(Ext4.DomQuery.select(selector, view.getNodeByRecord(model))[0]);
    },

    _getNodeValue : function(view, model, selector) {
        return this._getNode(view, model, selector).getValue();
    },

    renderPlot : function(contents, isStartQC) {
        if (isStartQC) {
            var mvHight = SignalDataService.getMaxHeight(contents);
            var mvHighCmp = Ext4.getCmp('mvrangehigh');
            mvHighCmp.setValue(mvHight);
        }

        var spectrumPlot = Ext4.getCmp('plotarea');
        spectrumPlot.leftRight = [Ext4.getCmp('aucleft').getValue(), Ext4.getCmp('aucright').getValue()];
        spectrumPlot.lowHigh = [Ext4.getCmp('mvrangelow').getValue(), Ext4.getCmp('mvrangehigh').getValue()];
        spectrumPlot.setHighlight(this.highlighted);
        spectrumPlot.renderPlot(contents);

        this.fireEvent('samplesrendered');
    },

    clearPlot : function() {
        Ext4.getCmp('plotarea').clearPlot();
    },

    updateZoom : function(l, r, b, t) {
        var leftCmp = Ext4.getCmp('aucleft');
        var rightCmp = Ext4.getCmp('aucright');
        var mvLowCmp = Ext4.getCmp('mvrangelow');
        var mvHighCmp = Ext4.getCmp('mvrangehigh');
        leftCmp.setValue(l);
        rightCmp.setValue(r);
        mvLowCmp.setValue(b);
        mvHighCmp.setValue(t);
    }
});
