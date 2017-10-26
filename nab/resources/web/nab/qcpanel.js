/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Main panel for the NAb QC interface.
 */
Ext4.QuickTips.init();

Ext4.define('LABKEY.ext4.NabQCPanel', {

    extend: 'Ext.panel.Panel',

    border: false,

    bodyStyle : 'background-color: transparent;',

    layout : {
        type : 'card',
        deferredRender : true
    },

    initComponent: function() {
        this.activeItem = 'selectionpanel';

        this.prevBtn = new Ext4.Button({text: 'Previous', disabled: true, scope: this, handler: function(){
            if (this.activeItem === 'confirmationpanel'){
                this.activeItem = 'selectionpanel';
                this.prevBtn.setDisabled(true);
                this.nextBtn.setText('Next');
            }
            this.getLayout().setActiveItem(this.activeItem);
        }});

        this.nextBtn = new Ext4.Button({text: 'Next', scope: this, handler: function(){
            if (this.activeItem === 'selectionpanel'){
                this.activeItem = 'confirmationpanel';
                this.prevBtn.setDisabled(false);
                this.nextBtn.setText('Finish');

                this.getLayout().setActiveItem(this.activeItem);
            }
            else {
                this.onSave();
            }
        }});

        this.cancelBtn = new Ext4.Button({text: 'Cancel', scope: this, handler: function(){
            window.location = this.returnUrl;
        }});

        var bbar = {
            xtype: 'toolbar',
            dock: 'bottom',
            border: false,
            style: 'background-color: transparent;',
            ui: 'footer',
            items: []
        };
        if (this.edit) {
            bbar.items = ['->', this.cancelBtn, this.prevBtn, this.nextBtn];
        }
        else {
            bbar.items = ['->', {text: 'Done', scope: this, handler: function(){
                window.location = this.returnUrl;
            }}];
        }

        this.dockedItems = [bbar];

        this.items = [
            this.getSelectionPanel(),
            this.getConfirmationPanel()
        ];

        // create a delayed task for checkbox change events
        this.checkboxChangeTask = new Ext4.util.DelayedTask(this.exclusionChanged, this);
        this.checkboxChangeQueue = {};

        this.callParent(arguments);
    },

    exclusionChanged : function(){
        var queue = Ext4.clone(this.checkboxChangeQueue);
        this.checkboxChangeQueue = {};

        var store = this.getFieldSelectionStore();
        if (store){
            var refreshConfirmationPanel = false;
            var removed = [];
            var added = [];

            for (var key in queue){
                if (queue.hasOwnProperty(key)){

                    var item = queue[key];
                    var rec = store.findRecord('key', item.key, 0, false, true, true);
                    if (rec && !item.checked){
                        removed.push(rec);
                        // seriously?
                        refreshConfirmationPanel = true;
                    }
                    else if (item.checked && !rec){
                        // add the record to the store
                        added.push({
                            key     : item.key,
                            plate   : item.plate,
                            row     : item.row,
                            rowlabel : item.rowlabel,
                            col      : item.col,
                            specimen : item.specimen,
                            excluded : item.checked
                        });
                    }
                }
            }
            if (removed.length > 0)
                store.remove(removed);
            if (added.length > 0)
                store.add(added);

            if (refreshConfirmationPanel){
                var fieldPanel = this.confirmationPanel.getComponent('field-selection-view');
                if (fieldPanel){
                    fieldPanel.refresh();
                }
            }
        }
    },

    getSelectionPanel : function(){

        if (!this.selectionPanel){
            // create a placeholder component and swap it out after we get the QC information for the run
            this.selectionPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'selectionpanel',
                border: false,
                bodyStyle : 'background-color: transparent;',
                items : [{
                    xtype : 'panel',
                    border: false,
                    bodyStyle : 'background-color: transparent;',
                    height : 700

                }],
                listeners : {
                    scope   : this,
                    render  : function(cmp) {
                        cmp.getEl().mask('Requesting QC information');
                        this.getQCInformation();
                    }
                }
            });
        }
        return this.selectionPanel;
    },

    getQCInformation : function(){

        LABKEY.Ajax.request({
            url: LABKEY.ActionURL.buildURL('nabassay', 'getQCControlInfo.api'),
            method: 'POST',
            params: {
                rowId : this.runId
            },
            scope: this,
            success: function(response){
                var json = Ext4.decode(response.responseText);
                var items = [];
                this.plates = json.plates;
                var data = {
                    runName         : this.runName,
                    runProperties   : this.runProperties,
                    controls        : this.controlProperties,
                    plates          : this.plates,
                    dilutionSummaries  : json.dilutionSummaries
                };

                var selectionPanel = this.getSelectionPanel();
                selectionPanel.getEl().unmask();
                selectionPanel.removeAll();

                if (this.edit) {
                    var tplText = [];

                    tplText.push('<table class="qc-panel"><tr><td colspan="2">');
                    tplText.push(this.getSummaryTpl());
                    tplText.push('</td></tr>');
                    tplText.push(this.getControlWellsTpl());
                    tplText.push(this.getDilutionSummaryTpl());
                    tplText.push('</table>');

                    // create the template and add the template functions to the configuration
                    items.push({
                        xtype : 'panel',
                        border : false,
                        bodyStyle : 'background-color: transparent;',
                        tpl : new Ext4.XTemplate(tplText.join(''),
                                {
                                    getId : function(cmp) {
                                        return Ext4.id();
                                    },
                                    getColspan : function(cmp, values){
                                        return values.columnLabel.length / 2;
                                    },
                                    me : this
                                }
                        ),
                        data : data,
                        listeners : {
                            scope   : this,
                            render  : function(cmp) {
                                this.renderControls();
                            }
                        }
                    });
                    selectionPanel.add(items);
                }
                else {
                    // create the store to get the initial load
                    var store = this.getFieldSelectionStore();
                    store.on('load', function(){
                        this.activeItem = 'confirmationpanel';
                        this.getLayout().setActiveItem(this.activeItem);
                    }, this, {single : true})
                }
            }
        });
    },

    /**
     * Generate the template for the run summary section
     * @returns {Array.<*>}
     */
    getSummaryTpl : function(){
        var tpl = [];
        tpl.push(
            '<div class="panel panel-default">',
                '<div class="panel-heading"><h3 class="panel-title pull-left">Run Summary: {runName}</h3><div class="clearfix"></div></div>',
                '<div class=" panel-body">',
                    '<table class="run-summary">',
                    '<tpl for="runProperties">',
                        '<td class="prop-name">{name}</td>',
                        '<td class="prop-value">{value}</td>',
                        '<tpl if="xindex % 2 === 0"></tr><tr></tpl>',
                    '</tpl>',
                    '</table>',
                '</div>',
            '</div>'
        );
        return tpl.join('');
    },

    getControlWellsTpl : function(){
        var tpl = [];

        tpl.push(
            '<tpl for="plates">',
                '<tpl if="xindex % 2 === 1"><tr><td></tpl>',
                '<tpl if="xindex % 2 != 1"><td></tpl>',
                '<tpl for="controls">',
                    '<div class="panel panel-default">',
                        '<div class="panel-heading"><h3 class="panel-title pull-left">{parent.plateName} Controls</h3><div class="clearfix"></div></div>',
                        '<div class=" panel-body">',
                            '<table class="plate-controls">',
                                '<tr><td class="prop-name" colspan="{[this.getColspan(this, values) + 1]}">Virus Control</td><td class="prop-name" colspan="{[this.getColspan(this, values)]}">Cell Control</td></tr>',
                                '<tr><td class="prop-value" colspan="{[this.getColspan(this, values) + 1]}">{virusControlMean} &plusmn; {virusControlPlusMinus}</td><td class="prop-value" colspan="{[this.getColspan(this, values)]}">{cellControlMean} &plusmn; {cellControlPlusMinus}</td></tr>',
                                '<tr><td><div class="plate-columnlabel"></div></td>',
                                    '<tpl for="columnLabel">',
                                        '<td><div class="plate-columnlabel">{.}</div></td>',
                                    '</tpl>',
                                '</tr>',
                                '<tpl for="rows">',
                                    '<tr>',
                                        '<tpl for=".">',
                                            '<tpl if="xindex === 1"><td>{rowlabel}</td></tpl>',
                                            '<td class="control-checkbox" id="{[this.getId(this)]}" label="{value}" col="{col}" rowlabel="{rowlabel}" row="{row}" specimen="{sampleName}" plate="{plate}"></td>',
                                        '</tpl>',           // end cols
                                    '</tr>',
                                '</tpl>',                   // end rows
                            '</table>',
                        '</div>',
                    '</div>',
                '</tpl>',                       // end controls
                '<tpl if="xindex % 2 != 0"></td></tpl>',
                '<tpl if="xindex % 2 === 0"></td></tr></tpl>',
            '</tpl>'           // end plates
        );
        return tpl.join('');
    },

    /**
     * Create the template for the dilution curve selection UI
     */
    getDilutionSummaryTpl : function(){
        var tpl = [];

        tpl.push(
            '<tpl for="dilutionSummaries">',
                '<tr><td colspan="2">',
                '<div class="panel panel-default">',
                    '<div class="panel-heading"><h3 class="panel-title pull-left">{name}</h3><div class="clearfix"></div></div>',
                    '<div class=" panel-body">',
                        '<table class="dilution-summary">',
                            '<tr><td><img src="{graphUrl}" height="300" width="425"></td>',
                                '<td valign="top"><table class="labkey-data-region">',
                                '<tr>',
                                    '<td class="prop-name">{methodLabel}</td><td class="prop-name">{neutLabel}</td>',
                                    '<td class="dilution-checkbox-addall" id="{[this.getId(this)]}" specimen="{name}" samplenum="{sampleNum}"></td><td></td></tr>',
                                    '<tpl for="dilutions">',
                                        '<tr><td>{dilution}</td><td>{neut} &plusmn; {neutPlusMinus}</td>',
                                        '<tpl for="wells">',
                                            '<td class="dilution-checkbox" id="{[this.getId(this)]}" label="{value}" col="{col}" rowlabel="{rowlabel}" row="{row}" specimen="{parent.sampleName}" plate="{plateNum}" samplenum="{parent.sampleNum}"></td>',
                                        '</tpl>',           // end wells
                                        '</tr>',
                                    '</tpl>',               // end dilutions
                                '</table></td></tr>',
                        '</table>',
                    '</div>',
                '</div>',
                '</td></tr>',
            '</tpl>'
        );
        return tpl.join('');
    },

    /**
     * Add check controls and register listeners
     */
    renderControls : function(){

        // create the checkbox controls for the virus and cell controls
        Ext4.each(Ext4.DomQuery.select('td.control-checkbox', this.getEl().dom), function(cmp)
        {
            if (cmp.hasAttribute('id'))
                this.createExclusionCheckbox(cmp);
        }, this);

        // create the checkbox controls for the sample dilutions
        Ext4.each(Ext4.DomQuery.select('td.dilution-checkbox', this.getEl().dom), function(cmp)
        {
            if (cmp.hasAttribute('id'))
                this.createExclusionCheckbox(cmp);
        }, this);

        var addCheckboxSelectAllEls = Ext4.DomQuery.select('td.dilution-checkbox-addall', this.getEl().dom);
        Ext4.each(addCheckboxSelectAllEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : 'Select all',
                    specimen : cmp.getAttribute('specimen'),
                    sampleNum: cmp.getAttribute('samplenum'),
                    listeners : {
                        scope   : this,
                        change  : function(cmp, newValue) {
                            // get all of the checkboxes with the same specimen and plate
                            var checkboxes = Ext4.ComponentQuery.query('checkbox[sampleNum=' + cmp.sampleNum + ']');
                            Ext4.each(checkboxes, function(ck){

                                ck.setValue(newValue);
                            }, this);
                        }
                    }
                });
            }
        }, this);
    },

    /**
     * Create the checkbox control for well level exclusions
     */
    createExclusionCheckbox : function(cmp){

        return Ext4.create('widget.checkbox', {
            renderTo : cmp.getAttribute('id'),
            boxLabel : cmp.getAttribute('label'),
            row      : cmp.getAttribute('row'),
            rowlabel : cmp.getAttribute('rowlabel'),
            col      : cmp.getAttribute('col'),
            specimen : cmp.getAttribute('specimen'),
            plate    : cmp.getAttribute('plate'),
            sampleNum: cmp.getAttribute('samplenum'),
            listeners : {
                scope   : this,
                change  : function(cmp, checked) {
                    var key = LABKEY.nab.QCUtil.getModelKey(cmp);
                    this.checkboxChangeQueue[key] = {
                        key     : key,
                        checked : checked,
                        plate   : cmp.plate,
                        row     : cmp.row,
                        rowlabel : cmp.rowlabel,
                        col      : cmp.col,
                        specimen : cmp.specimen
                    };
                    this.checkboxChangeTask.delay(500);
                }
            }
        });
    },

    getConfirmationPanel : function(){

        if (!this.confirmationPanel){

            var tplText = [];
            tplText.push('<table class="qc-panel"><tr><td colspan="2">');
            tplText.push(this.getSummaryTpl());
            tplText.push('</td></tr></table>');

            this.confirmationPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'confirmationpanel',
                border : false,
                bodyStyle : 'background-color: transparent;',
                items : [{
                    xtype : 'panel',
                    border  : false,
                    bodyStyle : 'background-color: transparent;',
                    tpl : new Ext4.XTemplate(tplText.join('')),
                    data : {
                        runName         : this.runName,
                        runProperties   : this.runProperties
                    }
                },{
                    xtype   : 'dataview',
                    itemId  : 'field-selection-view',
                    border  : false,
                    tpl     : this.getFieldSelectionTpl(),
                    autoScroll : true,
                    store   : this.getFieldSelectionStore(),
                    itemSelector :  'tr.field-exclusion',
                    disableSelection: true,
                    listeners : {
                        scope   : this,
                        itemclick : function(view, rec, item, idx, event){
                            if (event.target.getAttribute('class') == 'fa labkey-link fa-times') {
                                view.getStore().remove(rec);
                                this.setWellCheckbox(rec, false);
                            }
                        },
                        refresh : function(cmp){
                            var commentFieldEls = Ext4.DomQuery.select('input.field-exclusion-comment', this.getEl().dom);
                            Ext4.each(commentFieldEls, function(cmp) {
                                Ext4.EventManager.addListener(cmp, 'change', this.onCommentChange, this);
                            }, this);
                        },
                        itemadd : function(rec, idx, nodes){
                            Ext4.each(nodes, function(node){
                                this.addCommentFieldListener(node);
                            }, this);
                        },
                        itemupdate : function(rec, idx, node){
                            this.addCommentFieldListener(node);
                        }
                    }
                }],
                listeners : {
                    scope   : this,
                    render  : function(cmp) {
                        // add the raw plate information lazily because it depends on the initial ajax request for
                        // selection data
                        var plateTpl = [];
                        plateTpl.push('<table class="qc-panel"><tr><td colspan="2">');
                        plateTpl.push(this.getPlateTpl());
                        plateTpl.push('</td></tr></table>');

                        cmp.add({
                            xtype : 'panel',
                            border  : false,
                            bodyStyle : 'background-color: transparent;',
                            tpl : new Ext4.XTemplate(plateTpl.join(''),
                                    {
                                        getKey : function(rec) {
                                            return LABKEY.nab.QCUtil.getModelKey(rec);
                                        }
                                    }
                            ),
                            data : {
                                plates : this.plates
                            },
                            listeners : {
                                scope   : this,
                                render : function(cmp){
                                    // need to pick up any selections made prior to component render
                                    var store = this.getFieldSelectionStore();
                                    if (store){
                                        store.each(function(rec){

                                            var key = LABKEY.nab.QCUtil.getModelKey(rec.getData());
                                            LABKEY.nab.QCUtil.setWellExclusion(key, true, rec.get('comment'), this);

                                        }, this);
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
        return this.confirmationPanel;
    },

    getFieldSelectionTpl : function(){
        return new Ext4.XTemplate(
            '<div class="panel panel-default">',
                '<div class="panel-heading"><h3 class="panel-title pull-left">Excluded Field Wells</h3><div class="clearfix"></div></div>',
                '<div class=" panel-body">',
                    '<p>The following wells will be excluded from the curve fit calculations:</p>',
                    '<table class="field-exclusions">',
                        '<tr><td></td><td class="prop-name">Row</td><td class="prop-name">Column</td><td class="prop-name">Specimen</td><td class="prop-name">Plate</td><td class="prop-name">Comment</td></tr>',
                        '<tpl for=".">',
                            '<tr class="field-exclusion">',
                                '<td class="remove-exclusion" data-qtip="Click to delete">{[this.getDeleteIcon(values)]}</td>',
                                '<td style="text-align: left">{rowlabel}</td>',
                                '<td style="text-align: left">{[this.getColumnLabel(values)]}</td>',
                                '<td style="text-align: left">{specimen}</td>',
                                '<td style="text-align: left">Plate {plate}</td>',
                                '<td style="text-align: left"><input class="field-exclusion-comment" key="{[this.getKey(values)]}" {[this.getReadonly()]} type="text" name="comment" size="60" value="{comment:htmlEncode}"></td>',
                            '</tr>',
                        '</tpl>',
                    '</table>',
                '</div>',
            '</div>',
            {
                // don't show the remove icon if we aren't in edit mode
                getDeleteIcon : function(rec){
                    if (this.me.edit){
                        return '<div><span class="fa labkey-link fa-times"></span></div>';
                    }
                    return '';
                },
                getKey : function(cmp){
                    return LABKEY.nab.QCUtil.getModelKey(cmp);
                },
                getReadonly : function(){
                    return this.me.edit ? '' : 'readonly';
                },
                getColumnLabel : function(rec){

                    return parseInt(rec.col) + 1;
                },
                me : this
            }
        );
    },

    getPlateTpl : function(){
        var tpl = [];

        tpl.push(
            '<tpl for="plates">',
                '<tpl for="rawdata">',
                    '<div class="panel panel-default">',
                        '<div class="panel-heading"><h3 class="panel-title pull-left">{parent.plateName}</h3><div class="clearfix"></div></div>',
                        '<div class=" panel-body">',
                            '<table class="labkey-data-region-legacy labkey-show-borders plate-summary">',
                                '<tr><td>&nbsp;</td>',
                                    '<tpl for="columnLabel">',
                                        '<td class="plate-columnlabel">{.}</td>',
                                    '</tpl>',
                                '</tr>',
                                '<tpl for="data">',
                                    '<tr>',
                                        '<tpl for=".">',
                                            '<tpl if="xindex === 1"><td class="plate-rowlabel">{rowlabel}</td></tpl>',
                                                '<td class="{[this.getKey(values)]}">{value}</td>',
                                        '</tpl>',           // end cols
                                    '</tr>',
                                '</tpl>',               // end data
                            '</table>',
                        '</div>',
                    '</div>',
                '</tpl>',                   // end rawdata
            '</tpl>'                            // end plates
        );
        return tpl.join('');
    },

    getFieldSelectionStore : function() {

        if (!this.fieldSelectionStore) {
            this.fieldSelectionStore = Ext4.create('Ext.data.Store', {
                model : 'LABKEY.Nab.SelectedFields',
                proxy : {
                    type : 'ajax',
                    url : LABKEY.ActionURL.buildURL('nabassay', 'getExcludedWells.api'),
                    extraParams : {rowId : this.runId},
                    reader : { type : 'json', root : 'excluded' }
                },
                autoLoad: true,
                pageSize: 10000,
                sorters : [{
                    sorterFn : function(o1, o2){
                        var aso = o1.get('key');
                        var bso = o2.get('key');

                        return LABKEY.internal.SortUtil.naturalSort(aso, bso);
                    }
                }],
                listeners : {
                    scope   : this,
                    add : function(store, records){
                        Ext4.each(records, function(rec){

                            var key = LABKEY.nab.QCUtil.getModelKey(rec.getData());
                            LABKEY.nab.QCUtil.setWellExclusion(key, true, rec.get('comment'), this);
                        }, this);
                    },
                    remove : function(store, records){
                        Ext4.each(records, function(rec){

                            var key = LABKEY.nab.QCUtil.getModelKey(rec.getData());
                            LABKEY.nab.QCUtil.setWellExclusion(key, false, undefined, this);
                        }, this);
                    },
                    load : function(store, records){
                        Ext4.each(records, function(rec){

                            this.setWellCheckbox(rec, true);
                        }, this);
                    }
                }
            });
        }
        return this.fieldSelectionStore;
    },

    onSave : function(){

        var excluded = [];
        this.getFieldSelectionStore().each(function(rec){

            excluded.push({
                row : rec.get('row'),
                col : rec.get('col'),
                plate : rec.get('plate'),
                comment : rec.get('comment'),
                specimen: rec.get('specimen')
            });
        }, this);

        this.getEl().mask('Saving QC information');

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('nabassay', 'saveQCControlInfo.api'),
            method: 'POST',
            jsonData: {
                runId : this.runId,
                excluded : excluded
            },
            success: function (response) {
                window.location = this.returnUrl;
            },
            failure: this.failureHandler,
            scope: this
        });
    },

    failureHandler : function(response)
    {
        this.getEl().unmask();
        var msg = response.status === 403 ? response.statusText : Ext4.decode(response.responseText).exception;
        Ext4.Msg.show({
            title:'Error',
            msg: msg,
            buttons: Ext4.Msg.OK,
            icon: Ext4.Msg.ERROR
        });
    },

    /**
     * Set the checked state of the checkbox matching the location specified
     */
    setWellCheckbox : function(rec, checked){
        // get all of the checkboxes with the same specimen and plate
        var checkboxes = Ext4.ComponentQuery.query('checkbox[plate=' + rec.get('plate') + '][row=' + rec.get('row') + '][col=' + rec.get('col') + ']');
        Ext4.each(checkboxes, function(ck){

            ck.setValue(checked);
        }, this);

    },

    addCommentFieldListener : function(node){
        Ext4.EventManager.addListener(node, 'change', this.onCommentChange, this, {single : true});
    },

    onCommentChange : function(event, cmp){
        var key = cmp.getAttribute('key');
        if (key){
            var store = this.getFieldSelectionStore()
            var rec = store.findRecord('key', key);
            if (rec && cmp.value){
                rec.set('comment', cmp.value);
            }
        }
    }
});

if (!LABKEY.nab)
    LABKEY.nab = {};

/**
 * Utility class for well level QC
 * @type {QCUtil}
 */
LABKEY.nab.QCUtil = new function() {

    function _getKey(plate, row, col)
    {
        return plate + '-' + row + '-' + col;
    }

    return {
        /**
         * mark the well as excluded (or clear) on the plate diagram
         */
        setWellExclusion : function(key, excluded, comment, scope){

            var elements = Ext4.select('.' + key, true);
            if (elements){
                elements.each(function(el){
                    if (excluded && el) {
                        el.addCls('excluded');
                        el.dom.setAttribute('data-qtip', comment ? Ext4.htmlEncode(comment) : 'excluded from calculations');
                    }
                    else if (el) {
                        el.removeCls('excluded');
                        el.dom.removeAttribute('data-qtip');
                    }
                }, scope ? scope : this);
            }
        },

        getModelKey : function(rec){
            return _getKey(rec.plate, rec.row, rec.col);
        },

        getKey : function(plate, row, col){
            return _getKey(plate, row, col);
        }
    }
};

Ext4.define('LABKEY.Nab.SelectedFields', {
    extend: 'Ext.data.Model',
    fields : [
        {name: 'key'},
        {name: 'row'},
        {name: 'rowlabel', mapping : 'rowLabel'},
        {name: 'col'},
        {name: 'specimen'},
        {name: 'plate'},
        {name: 'comment'},
        {name: 'excluded'}
    ]
});

