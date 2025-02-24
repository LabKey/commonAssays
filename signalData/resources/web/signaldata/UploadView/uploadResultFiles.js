/*
 * Copyright (c) 2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Created by iansigmon on 1/20/16.
 */
Ext4.ns('LABKEY.SignalData');

LABKEY.SignalData.initializeDataFileUploadForm = function (metadataFormId, elementId, assay, loadedRun, resultFileContents) {

    var hideOverlay = true;

    var setWorkingDirectory = function() {
        var dir = 'TEMP_SignalData_' + getRunFolderName();
        sessionStorage.signalDataWorkingDirectory = dir;
        return dir;
    };

    var getTempFolderName = function() {
        if (!sessionStorage.signalDataWorkingDirectory)
            setWorkingDirectory();
        return sessionStorage.signalDataWorkingDirectory;
    };

    var runFolder;
    var getRunFolderName = function() {
        if (!runFolder) {
            var now = new Date();
            var parts = [now.getFullYear(), now.getMonth() + 1 /*javascript uses 0 based month*/,
                    now.getDate(), now.getHours(),now.getMinutes(),now.getSeconds()];
            runFolder = parts.join('_');
        }
        return runFolder;
    };

    var uploadLog = Ext4.create('LABKEY.SignalData.UploadLog', {
        region: 'center',
        workingDirectory: getTempFolderName(),
        flex: 2,
        results: resultFileContents,
        resultFields: assay.domains[assay.name + ' Result Fields'],
        listeners: {
            removefile: function(name, count) {
                if (count == 0) {
                    hideOverlay = false;
                    LABKEY.internal.FileDrop.showDropzones();
                }
            }
        }
    });

    var clearCachedReports = function(cleanUpFiles, callback, scope) {
        uploadLog.getStore().removeAll();
        uploadLog.getStore().sync();
        form.getForm().reset();

        dropzone.removeAllFiles(true);
        LABKEY.internal.FileDrop.showDropzones();

        if (cleanUpFiles) {
            //delete contents of working folder
            uploadLog.fileSystem.deletePath({
                path: uploadLog.getFullWorkingPath(),
                isFile: false,
                success: callback,
                scope: scope
            });
        } else {
            callback.call(scope || this);
        }
    };

    var dropzone; var form;
    var init = function() {
        Ext4.QuickTips.init();

        var _assayForm;
        var getAssayForm = function() {

            if (!_assayForm) {
                _assayForm = Ext4.create('Ext.form.Panel', {
                    region: 'west',
                    title: 'Run Fields',
                    cls: 'signaldata-run-form',
                    border: false,
                    layout: 'vbox',
                    shrinkWrap: true,
                    shrinkWrapDock: true,
                    buttonAlign: 'center',
                    collapsible: false,
                    width: '25%',
                    minWidth: 300,
                    minHeight: 300,
                    flex: 1,
                    items: getAssayFormFields(),
                    buttons: [
                        {
                            //Add a reset button
                            xtype: 'button',
                            text: 'Clear Run',
                            cls: 'labkey-button',
                            handler: function () {
                                Ext4.Msg.show({
                                    title: 'Clear Run',
                                    msg: 'Are you sure you want to clear these results?',
                                    icon: Ext4.window.MessageBox.INFO,
                                    buttons: Ext4.Msg.YESNO,
                                    fn: function(btn) {
                                        if (btn == 'yes') {
                                            clearCachedReports(true, function() {
                                                uploadLog.workingDirectory = setWorkingDirectory();
                                                //Recreate working dir
                                                uploadLog.checkOrCreateWorkingFolder(uploadLog.getWorkingPath(), uploadLog);
                                                document.getElementById(metadataFormId).style.display = '';
                                                LABKEY.internal.FileDrop.hideDropzones();
                                                document.getElementById(elementId).innerHTML = ""; //Clear this form.
                                            }, this);
                                        }
                                    }
                                });
                            }
                        }, {
                            //Add a Save button
                            xtype: 'button',
                            text: 'Save Run',
                            cls: 'labkey-button',
                            formBind: true,
                            handler: function () {
                                var form = this.up('form').getForm();
                                closeRun(form);
                            }
                        }
                    ]
                });
            }

            return _assayForm;
        };

        var dropInit = function(){
            dropzone = LABKEY.internal.FileDrop.registerDropzone({
                url: LABKEY.ActionURL.buildURL("assay", "assayFileUpload"),
                uploadMultiple: false,
                maxFiles: 5000,
                // Allow uploads of 100GB files
                maxFilesize: 100*(1024*1024),
                previewsContainer: false,

                peer: function() {
                    // Get the grid component from the outer Browser component
                    var grid = uploadLog.getGrid();
                    return grid ? grid.el : uploadLog.ownerCt.el;
                },

                /**
                 * If the user is allowed to drop
                 * @param file
                 * @param done
                 */
                accept: function(file, done) {
                    // Filter out folder drag-drop on unsupported browsers (Firefox)
                    // See: https://github.com/enyo/dropzone/issues/528
                    if ( (!file.type && file.size == 0 && file.fullPath == undefined)) {
                        done("Drag-and-drop upload of folders is not supported by your browser. Please consider using Google Chrome or an external WebDAV client.");
                        return;
                    }

                    //Hide dropzone overlay
                    LABKEY.internal.FileDrop.hideDropzones();
                    hideOverlay = true;

                    done();
                },

                init: function() {
                    this.on('addedfile', function (file) {
                        if (!uploadLog.containsFile(file)) {
                            dropzone.removeFile(file);
                            Ext4.Msg.show({msg: file.name + ' not uploaded'});
                        }
                    });

                    this.on('processing', function (file) {
                        var cwd = uploadLog.getFullWorkingPath();
                        if (cwd) {
                            // Folder the file will be POSTed into
                            var uri = uploadLog.fileSystem.concatPaths(cwd, file.name);
                            this.options.url = uploadLog.fileSystem.getParentPath(uri);
                        }

                        //get modelInstance from store that matches file.name
                        var store = uploadLog.getStore();
                        var idx = store.find('DataFile', file.name);
                        var process = store.getAt(idx);

                        //Set upload time
                        process.set(uploadLog.UPLOAD_TIME, new Date());
                        file.workingId = process.get('id');
                        uploadLog.getStore().sync();

                        //Update in-progress tracker
                        processingCounter.value++;
                        form.isValid();
                    });

                    //Update file progress bar
                    this.on('uploadprogress',function(file, progress, bytesSent){
                        var model = uploadLog.getStore().getById(file.workingId);
                        if (model)
                            model.set('progress', progress);
                    });

                    //Update in-progress tracker
                    this.on('success', function(file, response, evt) {
                        processingCounter.value--;
                        form.isValid();
                    });
                },
                show: !(uploadLog.getStore().getCount() > 0)
            });

            dropzone.uploadPanel = uploadLog;
            dropzone.form = form;
        };

        form = Ext4.create('Ext.form.Panel', {
            renderTo: elementId,
            tempFolder: getTempFolderName(),
            layout: 'border',
            height: 300,
            minHeight: 300,
            border: false,
            bodyStyle: 'background-color: transparent;',
            items: [
                getAssayForm(),
                uploadLog
            ],
            listeners: {
                actioncomplete: function (_form, action, eOpts) {
                    var process = uploadLog.getModelInstance({
                        uploadTime: new Date(),
                        fileName: action.result.name
                    });

                    uploadLog.getStore().add(process);
                    uploadLog.getStore().sync();
                },
                actionfailed: function (_form, action, eOpts) {
                    LABKEY.Utils.alert('Server Failed', 'Failed to create run.');
                }
            }
        });

        window.onbeforeunload = function(){
            if(form.isDirty() || uploadLog.getStore().getCount() > 0) {
                return 'Unsaved changes will be lost. Continue?';
            }
        };

        window.onunload = function(){
            clearCachedReports(true);
        };

        dropInit();

        Ext4.EventManager.onWindowResize(function(w, h) {
            LABKEY.ext4.Util.resizeToViewport(this, w, this.getHeight(), 20, 35);
            if (!hideOverlay && dropzone) {
                LABKEY.internal.FileDrop.hideDropzones();
                LABKEY.internal.FileDrop.showDropzones();
            }
        }, form);
    };

    var processingCounter = Ext4.create('Ext.form.field.Hidden', {
        xtype:'hidden',
        validate: function(){
            return this.value === 0;
        },
        validateOnChange:true,
        value: 0
    });

    var getAssayFormFields = function() {

        var batchFields = assay.domains[assay.name + ' Batch Fields'];
        var runFields = assay.domains[assay.name + ' Run Fields'];

        var configs = getConfigs(runFields);

        configs.push(processingCounter);

        return configs;
    };

    var showNoFilesError = function() {
        LABKEY.Utils.alert('No Files', 'Please add run result file(s)');
    };

    var getConfigs = function(fields) {
        if (!fields || fields.length == 0)
            return [];

        var configs = [];
        Ext4.each(fields, function(metaField) {
            configs.push(getExtConfig(metaField));
        });

        return configs;
    };

    var getExtConfig = function(meta) {
        setLookupConfig(meta);
        var config = LABKEY.ext4.Util.getFormEditorConfig(meta);

        return Ext4.apply(config, {
            id: config.name,
            validateOnBlur: false
        });
    };

    var setLookupConfig = function(meta) {
        // the getDefaultEditorConfig code in util.js expects a lookup object, so create one here
        // if our metadata has lookup information
        if (meta.lookupQuery && meta.lookupSchema && !meta.lookup) {
            meta.lookup = {
                container : meta.lookupContainer,
                schemaName : meta.lookupSchema,
                queryName : meta.lookupQuery
            };
        }
    };

    var closeRun = function(form) {
        var fieldValues = form.getFieldValues();
        var runFolder = getRunFolderName();

        uploadLog.commitRun(runFolder, generateAndSaveRun, this, fieldValues);
    };

    function saveRun(run) {
        LABKEY.Experiment.saveBatch({
            assayId: assay.id,
            batch: {
                batchProtocolId: assay.id,
                runs: [{
                    name: run.name,
                    properties: run.properties,
                    dataRows: run.dataRows,
                    dataInputs: run.dataInputs
                }]
            },
            success: function() {
                clearCachedReports(false, function() {
                    let returnUrl = LABKEY.ActionURL.getReturnUrl();
                    if (returnUrl) {
                        if (window.location.hash)
                            returnUrl = returnUrl.concat(window.location.hash);
                    }
                    else {
                        returnUrl = LABKEY.ActionURL.buildURL('assay', 'assayBegin', null, {rowId: assay.id});
                    }
                    uploadLog.workingDirectory = setWorkingDirectory();
                    window.location = returnUrl;
                },this);
            },
            failure: function(response){
                //TODO: Should probably do something here...
            }
        }, this);
    }

    var generateAndSaveRun = function(files, fieldValues) {

        var dataRows = [];
        var dataInputs = [];

        var rows = uploadLog.getStore().getRange();

        var runFolder = getRunFolderName();
        rows.forEach(function (row){
            var dataRow = {};
            row.fields.eachKey(function(key){
                dataRow[key] = row.get(key);
            });

            if(row.get('file')) {
                dataRow[uploadLog.DATA_FILE] = decodeURI(dataRow[uploadLog.FILE_URL]).replace('file:','');

                dataInputs.push({
                    name: row.get(uploadLog.DATA_FILE),  //This is how DataFile will be looked up in Chromatogram
                    dataFileURL: row.get(uploadLog.FILE_URL)
                });
            }

            dataRows.push(dataRow);

        }, this);

        var run = new LABKEY.Exp.Run({
            name: runFolder,
            properties: fieldValues,
            dataRows: dataRows,
            dataInputs: dataInputs
        });

        saveRun(run);
    };

    init();
};