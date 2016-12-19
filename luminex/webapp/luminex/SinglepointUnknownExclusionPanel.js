
Ext.namespace('LABKEY');

// function called onclick of 'Exclude Singlepoint Unknowns' menu button to open the exclusion window
function openExclusionsSinglepointUnknownWindow(assayId, runId)
{
    // lookup the assay design information based on the Assay RowId
    LABKEY.Assay.getById({
        id: assayId,
        success: function(assay) {
            if (Ext.isArray(assay) && assay.length == 1) {
                var win = new Ext.Window({
                    cls: 'extContainer',
                    title: 'Exclude Singlepoint Unknowns from Analysis',
                    layout:'fit',
                    width: Ext.getBody().getViewSize().width < 490 ? Ext.getBody().getViewSize().width * .9 : 440,
                    height: Ext.getBody().getViewSize().height > 700 ? 600 : Ext.getBody().getViewSize().height * .75,
                    padding: 15,
                    modal: true,
                    closeAction:'close',
                    bodyStyle: 'background-color: white;',
                    items: new LABKEY.Exclusions.SinglepointUnknownPanel({
                        protocolSchemaName: assay[0].protocolSchemaName,
                        assayId: assayId,
                        runId: runId,
                        listeners: {
                            scope: this,
                            'closeWindow': function(){
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

    initComponent: function ()
    {
        LABKEY.Exclusions.SinglepointUnknownPanel.superclass.initComponent.call(this);

        this.setupWindowPanelItems();
    },

    setupWindowPanelItems: function ()
    {
        //this.addHeaderPanel('Analytes excluded for a replicate group or at the assay level will not be re-included by changes in singlepoint unknown exclusions');
        this.addHeaderPanel('Coming soon...');
    }
});