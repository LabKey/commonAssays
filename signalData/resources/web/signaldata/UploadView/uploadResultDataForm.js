/*
 * Copyright (c) 2016-2018 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by iansigmon on 8/8/16.
 */
Ext4.ns('LABKEY.SignalData');

LABKEY.SignalData.initializeUploadForm = function(metadataFormElId, metadataFileId, metadataTSVId, nextButtonElId, renderToId, templateDivId) {

    var assayType = 'Signal Data';
    var assay;

    var loadAssay = function(cb, scope) {
        if (LABKEY.page && LABKEY.page.assay) {
            assay = LABKEY.page.assay;
            cb.call(scope || this);
        }
        else {
            //TODO: may need to rework this for webpart...
            LABKEY.Assay.getById({
                id: LABKEY.ActionURL.getParameter("rowId"),
                success: function(definitions) {
                    if (definitions.length == 0) {
                        var link = LABKEY.Utils.textLink({
                            text: 'New assay design',
                            href: LABKEY.ActionURL.buildURL('assay', 'chooseAssayType')
                        });
                        Ext4.get(renderToId).update('To get started, create a "' + assayType + '" assay. ' + link);
                    }
                    else if (definitions.length == 1) {
                        assay = definitions[0];
                        cb.call(scope || this);
                    }
                    else {
                        // In the future could present a dropdown allowing the user to switch between active assay design
                        Ext4.get(renderToId).update('This webpart does not currently support multiple "' + assayType + '" assays in the same folder.');
                    }
                }
            });
        }
    };

    var formSubmitted = function (data) {
        var run = new LABKEY.Exp.Run();
        run.name = data.name;
        run.dataInputs = [ data ];

        if (!data.content)
        {
            data.getContent({
                format: 'jsonTSVExtended',
                success: function (content) {
                    data.content = content;
                    LABKEY.SignalData.initializeDataFileUploadForm(metadataFormElId, renderToId, assay, run, content);
                    document.getElementById(metadataFormElId).style.display = 'none';
                },
                failure: function (error, format) {
                }
            });
        }
    };

    var formFailed = function (form, action) {
        alert('Upload failed!');
    };

    LABKEY.SignalData.disableFileUpload = function() {
        tsvForm.setVisible(true);
        fileForm.setVisible(false);

        activeForm = tsvForm;
    };

    LABKEY.SignalData.enableFileUpload = function() {
        tsvForm.setVisible(false);
        fileForm.setVisible(true);

        activeForm = fileForm;
    };


    var activeForm;
    var fileForm;
    var tsvForm;
    loadAssay(function() {
        //Load the rowId into the template link
        var templateLink = document.getElementById(templateDivId);
        templateLink.setAttribute('href', LABKEY.ActionURL.buildURL("assay", "template", LABKEY.ActionURL.getContainer(),
                {rowId: assay.id}));

        fileForm = Ext4.create('Ext.form.Panel',{
            renderTo: metadataFileId,
            border: false,
            bodyStyle: 'background-color: transparent;',
            fileUpload: true,
            url: LABKEY.ActionURL.buildURL("assay", "assayFileUpload", LABKEY.ActionURL.getContainer(), { protocolId: assay.id }),
            flex:1,
            items: [{
                xtype: 'filefield',
                id: 'file',
                width: 400,
                buttonConfig: {
                    text: 'Upload txt, tsv, xls, xlsx'
                }
            }, {
                xtype: 'hidden',
                name: 'X-LABKEY-CSRF',
                value: LABKEY.CSRF
            }],
            listeners: {
                actioncomplete : function (form, action) {
                    var data = new LABKEY.Exp.Data(action.result);
                    formSubmitted(data);
                },
                actionfailed: formFailed
            }
        });

        tsvForm = Ext4.create('Ext.form.Panel',{
            renderTo: metadataTSVId,
            border: false,
            bodyStyle: 'background-color: transparent;',
            getTsvInput: function(){
                return Ext4.getCmp('tsv').value;
            },
            items: [{
                xtype: 'textarea',
                id: 'tsv',
                height: 300,
                minHeight: 300,
                width: 400,
                minWidth:150
            }, {
                xtype: 'hidden',
                name: 'X-LABKEY-CSRF',
                value: LABKEY.CSRF
            }],
            submit:function(){
                LABKEY.Ajax.request({
                    url: LABKEY.ActionURL.buildURL("assay", "assayFileUpload", LABKEY.ActionURL.getContainer()),
                    params: { protocolId: assay.id, fileName: metadataTSVId + '.txt', fileContent: this.getTsvInput() },
                    success: function(response) {
                        var data = new LABKEY.Exp.Data(Ext.util.JSON.decode(response.responseText));
                        formSubmitted(data);
                    },
                    failure: formFailed
                });
            },
            scope: this
        });

        Ext4.create('Ext.button.Button', {
            xtype: 'button',
            renderTo: nextButtonElId,
            text: 'next',
            handler: function () {
                activeForm.submit();
            }
        });

        LABKEY.SignalData.enableFileUpload();
    });
};
