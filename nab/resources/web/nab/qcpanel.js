/**
 * Main panel for the NAb QC interface.
 */
Ext4.define('LABKEY.ext4.NabQCPanel', {

    extend: 'Ext.panel.Panel',

    border: false,

    header : false,

    layout : 'card',

    alias: 'widget.labkey-nab-qc-panel',

    padding: 10,

    constructor: function (config)
    {
        this.callParent([config]);

        // objects to track QC selections
        this.fieldSelections = {};
    },

    initComponent: function ()
    {
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
            }
            this.getLayout().setActiveItem(this.activeItem);
        }});

        this.cancelBtn = new Ext4.Button({text: 'Cancel', scope: this, handler: function(){
        }});

        this.bbar = ['->', this.nextBtn, this.prevBtn, this.cancelBtn];

        this.items = [];
        this.items.push(this.getSelectionPanel());
        this.items.push(this.getConfirmationPanel());

        this.callParent(arguments);
    },

    getSelectionPanel : function(){

        if (!this.selectionPanel){
            // create a placeholder component and swap it out after we get the QC information for the run
            this.selectionPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'selectionpanel',
                items : [{
                    xtype : 'panel',
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

    getConfirmationPanel : function(){

        if (!this.confirmationPanel){

            this.confirmationPanel = Ext4.create('Ext.panel.Panel', {
                itemId : 'confirmationpanel',
                items : [{
                    xtype : 'panel',
                    height : 700

                }],
                listeners : {
                    scope   : this,
                    render  : function(cmp) {
                        cmp.getEl().mask('Requesting QC confirmation information');
                    }
                }
            });
        }
        return this.confirmationPanel;
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
                var data = {
                    runName         : this.runName,
                    runProperties   : this.runProperties,
                    controls        : this.controlProperties,
                    plates          : json.plates,
                    dilutionSummaries  : json.dilutionSummaries
                };

                var tplText = [];

                tplText.push('<table class="qc-panel"><tr><td colspan="2">');
                tplText.push(this.getSummaryTpl());
                tplText.push('</td></tr><tr><td valign="top">');
                tplText.push(this.getPlateControlsTpl());
                tplText.push('</td><td>');
                tplText.push(this.getControlWellsTpl());
                tplText.push('</td></tr><tr><td colspan="2">');
                tplText.push(this.getDilutionSummaryTpl());
                tplText.push('</td></tr></table>');

                // create the template ant add the template functions to the configuration
                items.push({
                    xtype : 'panel',
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
                var selectionPanel = this.getSelectionPanel();
                selectionPanel.getEl().unmask();
                selectionPanel.removeAll();
                selectionPanel.add(items);
            }
        });
    },

    /**
     * Generate the template for the run summary section
     * @returns {Array.<*>}
     */
    getSummaryTpl : function(){
        var tpl = [];
        tpl.push('<table>',
                '<tr class="labkey-data-region-header-container" style="text-align:center;"><th colspan="8">Run Summary: {runName}</th></tr>',
                '<tr><td>',
                '<tr>',
                '<tpl for="runProperties">',
                '<th style="text-align: left">{name}</th>',
                '<td>{value}</td>',
                '<tpl if="xindex % 2 === 0"></tr><tr></tpl>',
                '</tpl>',
                '</td></tr>',
                '</table>'
        );
        return tpl.join('');
    },

    getPlateControlsTpl : function(){
        var tpl = [];
        tpl.push('<table class="labkey-data-region labkey-show-borders">',
                '<tr><th></th><th>Plate</th><th>Range</th><th>Virus Control</th><th>Cell Control</th></tr>',
                '<tpl for="controls">',
                '<tr>',
                '<td class="plate-checkbox" plateNumm="{[xindex]}" id="{[this.getId(this)]}"></td>',
                '<td style="font-weight:bold">{[xindex]}</td>',
                '<td align="left">{controlRange}</td>',
                '<td align="left">{virusControlMean} &plusmn; {virusControlPlusMinus}</td>',
                '<td align="left">{cellControlMean} &plusmn; {cellControlPlusMinus}</td>',
                '</tr>',
                '</tpl>',
                '</table>'
        );
        return tpl.join('');
    },

    getControlWellsTpl : function(){
        var tpl = [];

        tpl.push(//'<table class="labkey-data-region labkey-show-borders">',
                '<tpl for="plates">',
                '<table>',
                '<tr><th colspan="5" class="labkey-data-region-header-container" style="text-align:center;">{plateName}</th></tr>',
                '<tr><td colspan="{[this.getColspan(this, values) + 1]}">Virus Control</td><td colspan="{[this.getColspan(this, values)]}">Cell Control</td></tr>',
                '<tr><td><div class="plate-columnlabel"></div></td>',
                '<tpl for="columnLabel">',
                '<td><div class="plate-columnlabel">{.}</div></td>',
                '</tpl>',
                '</tr>',
                '<tpl for="rows">',
                '<tr>',
                '<tpl for=".">',
                '<tpl if="xindex === 1"><td>{row}</td></tpl>',
                '<td class="control-checkbox" id="{[this.getId(this)]}" label="{value}"></td>',
                '</tpl>',           // end cols
                '</tr>',
                '</tpl>',           // end rows
                '</table>',
                '</tpl>'           // end plates
                //'</table>'
        );
        return tpl.join('');
    },

    /**
     * Create the template for the dilution curve selection UI
     */
    getDilutionSummaryTpl : function(){
        var tpl = [];

        tpl.push('<tpl for="dilutionSummaries">',
                '<table class="dilution-summary">',
                '<tr><td><img src="{graphUrl}" height="300" width="425"></td>',
                    '<td><table class="labkey-data-region">',
                    '<tr><td colspan="5" class="labkey-data-region-header-container" style="text-align:center;">{name}</td></tr>',
                    '<tr><td>{methodLabel}</td><td>{neutLabel}</td><td></td><td></td></tr>',
                    '<tpl for="dilutions">',
                    '<tr><td>{dilution}</td><td>{neut} &plusmn; {neutPlusMinus}</td>',
                    '<tpl for="wells">',
                    '<td class="dilution-checkbox" id="{[this.getId(this)]}" label="{value}" col="{col}" row="{row}" specimen="{parent.sampleName}" plate="{plateNum}"></td>',
                    '</tpl>',           // end wells
                    '</tr>',
                    '</tpl>',           // end dilutions
                    '</table></td></tr>',
                '</table>',
                '</tpl>'
        );
        return tpl.join('');
    },

    /**
     * Add check controls and register listeners
     */
    renderControls : function(){

        var addCheckboxEls = Ext4.DomQuery.select('td.plate-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id')
                });

                // add a listener...
            }
        }, this);

        var addCheckboxEls = Ext4.DomQuery.select('td.control-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : cmp.getAttribute('label')
                });

                // add a listener...
            }
        }, this);

        var addCheckboxEls = Ext4.DomQuery.select('td.dilution-checkbox', this.getEl().dom);
        Ext4.each(addCheckboxEls, function(cmp)
        {
            if (cmp.hasAttribute('id')){

                var field = Ext4.create('widget.checkbox', {
                    renderTo : cmp.getAttribute('id'),
                    boxLabel : cmp.getAttribute('label'),
                    row : cmp.getAttribute('row'),
                    col : cmp.getAttribute('col'),
                    specimen : cmp.getAttribute('specimen'),
                    plate : cmp.getAttribute('plate'),
                    listeners : {
                        scope   : this,
                        change  : function(cmp, newValue) {
                            this.fieldSelections[this.getKey(cmp.plate, cmp.row, cmp.col)] = newValue;
                        }
                    }
                });
            }
        }, this);

        this.doLayout();
    },

    getKey : function(plate, row, col){
        return plate + '-' + row + '-' + col;
    }
});
