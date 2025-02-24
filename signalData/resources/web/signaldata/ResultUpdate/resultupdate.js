/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by iansigmon on 8/26/16.
 */
Ext4.ns('LABKEY.SignalData');

var getAssay = function() {
    LABKEY.Assay.getById({
        id:LABKEY.ActionURL.getParameter('protocolId'),
        success:function(def) {
            getResultData(def[0]);
        }
    });
};

var loadBatch = function(callback, scope){
    LABKEY.Experiment.loadBatch({
        assayId: LABKEY.ActionURL.getParameter('protocolId'),
        batchId: LABKEY.ActionURL.getParameter('batchId'),
        success: function(result) {
            callback.call(scope || this, result);
        }
    });
};

var getResultData = function(assay) {
    LABKEY.Query.selectRows({
        schemaName: assay.protocolSchemaName,
        queryName: 'data',
        requiredVersion: 13.2,
        filterArray: [LABKEY.Filter.create('RowId', LABKEY.ActionURL.getParameter('rowId'))],
        success: function(results){
            if (results.length > 0) {
                init(assay, results.getRow(0));
            }
            else {
                // Use an ExtJS alert instead of a raw browser alert to avoid alarming the crawler
                Ext4.Msg.alert('Error', 'Row not found');
            }
        },
        scope: this
    });
};

var getFileInputFromExtFileField = function(field) {
    return document.getElementById(field.button.fileInputEl.id);
};

var init = function(assay, row){

    var assay = assay;
    var row = row;
    var saveRun = function(form, dataFiles) {
        if(!dataFiles) {
            var updateRow = setRunFields(form, dataFiles);
            updateRunResult(updateRow, dataFiles);
        }
        else {
            var file = dataFiles[0];
            LABKEY.Ajax.request({
                url: LABKEY.ActionURL.buildURL('SignalData', 'getSignalDataResource.api'),
                method: 'POST',
                params: {path: decodeURIComponent(file.internalId), test: true},
                success: function (response) {
                    var fileResource = Ext4.decode(response.responseText);
                    var updatedRow = setRunFields(form, fileResource);
                    updateRunResult(updatedRow, fileResource);
                },
                scope: this
            });
        }
    };

    var updateRunResult = function(updatedRow, file) {
        LABKEY.Query.updateRows({
            schemaName: assay.protocolSchemaName,
            queryName: 'data',
            rows: [updatedRow],
            success: function (rowResults, request, options) {
                if (!file) {
                    //No file to save, return to grid
                    navToReturnURL();
                }
                else {
                    //Save file
                    updateRunInputs(updatedRow, file);
                }
            },
            failure: function(response){
                //TODO: Do more here
                alert("Failed to update data");
            }
        });
    };

    var updateRunInputs = function(updatedRow, dataFile) {
        var callback = function(batch) {
            var runId = LABKEY.ActionURL.getParameter('runId');
            for (var i = 0; batch && i < batch.runs.length; i++) {

                //Find run to add input to.
                var run = batch.runs[i];
                if (runId == run.id) {
                    run.dataInputs.push({
                        dataFileURL: dataFile.DataFileUrl
                    });

                    //save updated batch with new input object
                    LABKEY.Experiment.saveBatch({
                        assayId: assay.id,
                        batch: batch,
                        success: function (batch, response) {
                            navToReturnURL();
                        },
                        scope: this
                    });

                    return;
                }
            }

            alert("Could not find run to update");
        };

        //Get fresh batch object
        loadBatch(callback, this);
    };

    var setRunFields = function(form, dataFiles) {
        var newRow = {
            RowId: LABKEY.ActionURL.getParameter('rowId')
        };

        if (dataFiles)
            newRow.DataFile = decodeURI(dataFiles['DataFileUrl']).replace( new RegExp('^file:/+'), '/');


        var fieldValues = form.getForm().getFieldValues(true);
        Object.keys(fieldValues).forEach(function(key){
            newRow[key] = fieldValues[key];
        });

        return newRow;
    };

    var getConfigs = function(fields) {
        if (!fields || fields.length == 0)
            return [];

        var configs = [];
        Ext4.each(fields, function(metaField) {
            var config = getExtConfig(metaField);
            configs.push(config);
        });

        return configs;
    };

    var fileFields = [];
    var getExtConfig = function(meta) {
        var config;
        if (meta.inputType === 'file' || meta.fieldKey === 'DataFile') {
            var dataFile = row.get(meta.fieldKey);
            config =
            {
                xtype:'fieldcontainer',
                fieldLabel: meta.name,
                name: meta.fieldKey,
                border:false,
                items:[{
                    xtype:'fieldcontainer',
                    tpl: '<div id="{fieldKey:htmlEncode}Template" >' +
                        '<tpl if="file.hasDisplayValue">' +
                            '<a href="{file.url}">' +
                                '{file.displayValue:htmlEncode}' +
                            '</a>' +
                        '<tpl else>' +
                            '{file.displayValue:htmlEncode}' +
                        '</tpl>' +
                    ' [<a class="removeFile">remove file</a>]' +
                    '</div>',
                    data:{
                        file:
                        {
                            url: dataFile.url,
                            hasDisplayValue: dataFile.displayValue,
                            displayValue: dataFile.displayValue || (dataFile.value + " (unavailable) ")
                        },
                        fieldKey: meta.fieldKey
                    },
                    listeners:{
                        render: function(field) {
                            field.up().template = field;
                        }
                    }
                }, {
                    xtype: 'fileuploadfield',
                    buttonText: "Choose a File",
                    id: meta.name + 'File',
                    width: 350,
                    hidden: true,
                    listeners:{
                        render: function(field) {
                            fileFields.push(field);
                            field.up().fileField = field;
                        }
                    }
                }],
                listeners:{
                    render: function(field){
                        // attach listeners to all of the delete span buttons
                        var selection = Ext4.select('a[class^="removeFile"]', this.getEl());
                        if (selection) {
                            Ext4.each(selection.elements, function (el) {
                                Ext4.EventManager.addListener(el, 'click', this.removeFile, this);
                            }, this);
                        }
                    }
                },
                removeFile: function() {
                    this.template.hide();
                    this.fileField.show();
                }
            };
        } else {
            setLookupConfig(meta);
            config = LABKEY.ext4.Util.getFormEditorConfig(meta);
            config.value = row.get(meta.name).value;
        }

        return Ext4.apply(config, {
            id: config.name,
            validateOnBlur: false,
            labelCls: 'labkey-form-label',
            formBind: true

        });
    };

    var setLookupConfig = function(meta) {
        // the getDefaultEditorConfig code in util.js expects a lookup object, so create one here
        // if our metadata has lookup information
        if (meta.lookupQuery && meta.lookupSchema && !meta.lookup) {
            meta.lookup = {
                container: meta.lookupContainer,
                schemaName: meta.lookupSchema,
                queryName: meta.lookupQuery
            };
        }
    };

    var resolvePipeline = function (callback, scope) {
        if (Ext4.isFunction(callback)) {
            Ext4.Ajax.request({
                url: LABKEY.ActionURL.buildURL('SignalData', 'getSignalDataPipelineContainer.api'),
                method: 'GET',
                success: function (response) {
                    var context = Ext4.decode(response.responseText);
                    if (Ext4.isObject(context) && !Ext4.isEmpty(context.containerPath) && !Ext4.isEmpty(context.webDavURL)) {
                        callback.call(scope || this, context);
                    }
                    else {
                        Ext4.Msg.alert('Error', 'Failed to load the pipeline context for Signal Data');
                    }
                },
                failure: function (error, response) {
                    Ext4.Msg.alert('Error', 'Failed to load the pipeline context for Signal Data');
                }
            });
        }
    };

    var getResultFieldConfigs = function(){
        var resultFields = assay.domains[assay.name + ' Result Fields'];
        return getConfigs(resultFields);
    };

    var form = Ext4.create('Ext.form.Panel', {
        renderTo: 'update',
        border:false,
        items: getResultFieldConfigs(),
        bodyStyle: 'background-color: transparent;',
        buttons: [{
            xtype: 'button',
            text: 'Cancel',
            handler: function () {
                window.location = LABKEY.ActionURL.getReturnUrl();
            }
        }, {
            xtype: 'button',
            text: 'Save',
            handler: function () {
                //Generate a form to upload files with
                var uploadForm;

                //For each file field in the main form add files to uploader
                fileFields.forEach(function(field) {
                    var fileInput = getFileInputFromExtFileField(field);
                    Object.keys(fileInput.files).forEach(function(key) {
                        var fileToUpload = fileInput.files[key];
                        uploadForm = uploadForm || new FormData();
                        uploadForm.append(fileToUpload.name, fileToUpload);
                    });
                });

                if (uploadForm) {
                    LABKEY.Ajax.request({
                        url: form.url + '?overwrite=t',
                        method: 'POST',
                        form: uploadForm,
                        header:false,
                        success: function (response, operation) {
                            var results;
                            if (response.responseXML != null) {
                                var reader = new Ext4.data.reader.Xml({
                                    record: '>response',
                                    root: 'multistatus',
                                    type: 'xml',
                                    model: 'File.data.webdav.XMLResponse'
                                });

                                results = reader.read(response.responseXML.documentElement);
                            }

                            saveRun(form, results ? results.records : null);
                        },
                        failure: function (response, operation) {
                            //TODO: Do something...
                        }
                    });
                }
                else {
                    saveRun(form);
                }
            }
        }],
        buttonAlign: 'left'
    });

    resolvePipeline(function (context) {
        this.fileSystem = Ext4.create('File.system.Webdav', {
            rootPath: context['webDavURL'],
            rootOffset: LABKEY.ActionURL.getParameter('runName'),
            rootName: 'fileset'
        });

        this.url = this.fileSystem.getAbsoluteURL();
    }, form);

    var navToReturnURL = function(){
        window.location = LABKEY.ActionURL.getReturnUrl();
    };
};

getAssay();
