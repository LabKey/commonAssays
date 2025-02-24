/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.ns('LABKEY.SignalData');

Ext4.define('LABKEY.SignalData.UploadLog', {

    extend: 'Ext.panel.Panel',

    modelClass: 'LABKEY.SignalData.model.Uploads',

    UPLOAD_TIME: 'uploadTime',
    DATA_FILE: 'DataFile',
    FILE_URL: 'DataFileURL',
    FILENAME: 'FileName',
    constructor: function (config) {
        if (!Ext4.ModelManager.isRegistered(this.modelClass)) {
            Ext4.define(this.modelClass, {
                extend: 'Ext.data.Model',
                fields: this.getFields(config.resultFields),
                publishMessage: function (message) {
                    var oldMsgs = this.get("messages");
                    oldMsgs.push(message);
                    this.setCommit("messages", oldMsgs);
                },

                setCommit: function (fieldName, newValue) {
                    this.set(fieldName, newValue);
                    this.commit();
                }
            });
        }

        this.items = [this.getGrid(config.results, config.resultFields)];
        this.callParent([config]);
        this.addEvents('removefile');
    },

    initComponent: function () {
        this.callParent();

        this.resolvePipeline(function (context) {
            this.fileSystem = Ext4.create('File.system.Webdav', {
                rootPath: context['webDavURL'],
                rootName: 'fileset'
            });

            this.createWorkingFolder();
        }, this);
    },

    getFields: function (resultFields) {
        if (!this.fields) {
            var fields = [];
            resultFields.forEach(function (field) {
                fields.push({
                    name: field.name,
                    type: 'string'
                });
            });

            this.fields = fields.concat([
                {name: this.FILE_URL, type: 'string'},
                {
                    name: 'messages',
                    convert: function (raw) {
                        var value = [];

                        if (Ext4.isArray(raw)) {
                            value = raw;
                        }
                        else if (!Ext4.isEmpty(raw)) {
                            value.push(raw);
                        }

                        return value;
                    }
                },
                {name: this.UPLOAD_TIME, type: 'date'},
                {name: 'progress', type: 'int'},
                {name: 'file', type:'file'}
            ]);
        }

        return this.fields;
    },

    getGrid: function (results, assayResultFields) {
        if (!this._grid) {
            this._grid = Ext4.create('Ext.grid.Panel', {
                height: 300,
                store: this.getStore(results),
                columns: this.getColumns(assayResultFields),
                invalidateScrollerOnRefresh: false,
                plugins: [
                    Ext4.create('Ext.grid.plugin.CellEditing', {
                        clicksToEdit: 1
                    })
                ],
                flex: 1
            });
        }

        return this._grid;
    },

    getColumns: function (assayResultFields) {
        if (!this.columns) {

            var columns = [];
            assayResultFields.forEach(function (field) {
                columns.push({
                    text: field.label,
                    dataIndex: field.fieldKey,
                    type: field.jsonType,
                    //TODO: create toggle option to enable editing?
                    //TODO: Instead make click open file chooser and associate selected file to row
                    editor: {
                        xtype: 'textfield'
                    }
                });
            });

            this.columns = columns.concat([{
                text: 'Upload Time',
                dataIndex: this.UPLOAD_TIME,
                renderer: Ext4.util.Format.dateRenderer('m/d/y g:i')
                , width: 150
            }, {
                text: 'Upload Progress',
                width: 150,
                dataIndex: 'progress',
                sortable: true,
                renderer: function (v, m, r) {
                    var calcValue = v / 100;
                    var pbRenderer = (function () {
                        var b = new Ext4.ProgressBar({height: 15});
                        return function (val) {
                            b.updateProgress(val);
                            return Ext4.DomHelper.markup(b.getRenderTree());
                        };
                    })();

                    return pbRenderer(calcValue);
                }
            }, {
                xtype: 'actioncolumn',
                width: 20,
                items: [{
                    iconCls: 'iconDelete',
                    tooltip: 'Remove uploaded file',
                    handler: function (grid, rowIndex, colIndex) {
                        var store = grid.getStore();
                        var row = store.getAt(rowIndex);
                        var fileName = row.get(this.DATA_FILE);
                        var me = this;

                        //Delete File
                        this.fileSystem.deletePath({
                            path: this.fileSystem.concatPaths(this.getFullWorkingPath(), fileName),
                            isFile: true,
                            success: function () {
                                row.set('progress', 0);
                                row.set(me.UPLOAD_TIME, '');
                                store.sync();
                                me.fireEvent('removefile', fileName, store.getCount());
                            }
                        });
                    },
                    scope: this
                }]
            }]);
        }

        return this.columns;
    },

    getStore: function (data) {
        if (!this._store) {
            var store = Ext4.create('Ext.data.Store', {
                model: this.modelClass,
                autoSync: true,
                proxy: {
                    type: 'sessionstorage',
                    id: 'pSizeProxy'
                }
            });

            this.loadData(store, data);

            this._store = store;
            // let the user see the most recent uploads at the top
            this._store.sort(this.UPLOAD_TIME, 'DESC');
        }

        return this._store;
    },
    loadData: function (store, content) {
        // Use 1st sheet
        var sheet = content.sheets[0];
        var data = sheet.data;

        //remove headers
        var headers = data.shift();

        data.forEach(function (dataRow) {
            var rowConfig = {};
            for (var i = 0; i < dataRow.length; i++) {
                rowConfig[headers[i].value] = dataRow[i].value;
            }

            store.add(this.getModelInstance(rowConfig));
        }, this);
    },

    getModelInstance: function (config) {
        return Ext4.create(this.modelClass, config);
    },

    /**
     * Moves files from temp folder to target folder
     * Assumes: assay container as base.
     *
     * @param tempFolderName
     * @param targetFolder
     */
    commitRun: function (targetDirectory, callback, scope, runProperties) {
        if (Ext4.isFunction(callback)) {
            var me = this;
            var destination = this.fileSystem.concatPaths(this.fileSystem.getBaseURL(), targetDirectory);
            this.fileSystem.renamePath({
                source: this.getWorkingPath(),
                destination: destination,
                isFile: false,
                success: function () {
                    me.resolveFileResources(targetDirectory, callback, scope, runProperties);
                }
            });
        }
    },

    resolveFileResources: function (targetDirectory, callback, callbackScope, runProperties) {
        var fileUri = this.fileSystem.concatPaths(this.fileSystem.getAbsoluteURL(), targetDirectory);
        LABKEY.Ajax.request({
            url: fileUri,
            method: 'GET',
            params: {method: 'JSON'},
            success: function (response) {
                var json = Ext4.decode(response.responseText);
                if (Ext4.isDefined(json) && Ext4.isArray(json.files)) {
                    if (!Ext4.isEmpty(json.files)) {
                        this.resolveDataFileURL(json.files, callback, callbackScope, runProperties);
                    }
                }
            },
            failure: function () {
            }
            , scope: this
        }, this);
    },

    /**
     * will append a 'dataFileURL' property to all files resolved as resources
     */
    resolveDataFileURL: function (files, callback, scope, runProperties) {
        if (Ext4.isFunction(callback)) {

            var received = 0;
            var newFiles = [];

            var me = this;

            function done(file, results) {

                var store = me.getStore();
                var idx = store.find(me.DATA_FILE, file.text);
                var process = store.getAt(idx);

                //Set upload time
                process.set(me.FILENAME, results[me.DATA_FILE]);
                process.set(me.FILE_URL, results['DataFileUrl']);
                process.set('file', file);
                newFiles.push(file);
                received++;

                if (received == files.length) {
                    callback.call(scope || me, newFiles, runProperties);
                }
            }

            //TODO: This should be refactored to use a single ajax call for the array
            files.forEach(function (file) {
                LABKEY.Ajax.request({
                    url: LABKEY.ActionURL.buildURL('SignalData', 'getSignalDataResource.api'),
                    method: 'POST',
                    params: {path: decodeURIComponent(file.id), test: true},
                    success: function (response) {
                        done(file, Ext4.decode(response.responseText));
                    },
                    scope: this
                });
            }, this);
        }
    },

    resolvePipeline: function (callback, scope) {
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
    },

    //TODO: this should probably just be an action
    createWorkingFolder: function () {
        this.checkOrCreateBaseFolder();
    },

    //Check if working folder exists
    checkOrCreateBaseFolder: function () {
        var targetURL = this.fileSystem.getBaseURL();

        LABKEY.Ajax.request({
            url: this.fileSystem.getURI(targetURL),
            method: 'GET',
            params: {method: 'JSON'},
            success: function (response) {
                //directory exists
                this.checkOrCreateTempFolder();
            },
            failure: function (b, xhr) {
                this.createFolder(targetURL, this.checkOrCreateTempFolder, this);
            },
            scope: this
        }, this);
    },

    //Check if working folder exists
    checkOrCreateTempFolder: function () {
        var targetURL = this.fileSystem.concatPaths(this.fileSystem.getBaseURL(), 'Temp');

        LABKEY.Ajax.request({
            url: this.fileSystem.getURI(targetURL),
            method: 'GET',
            params: {method: 'JSON'},
            success: function (response) {
                //directory exists
                this.checkOrCreateWorkingFolder();
            },
            failure: function (b, xhr) {
                this.createFolder(targetURL, this.checkOrCreateWorkingFolder, this);
            },
            scope: this
        }, this);
    },

    //Check if working folder exists
    checkOrCreateWorkingFolder: function () {
        var targetURL = this.getWorkingPath();

        LABKEY.Ajax.request({
            url: this.fileSystem.getURI(targetURL),
            method: 'GET',
            params: {method: 'JSON'},
            success: function (response) {
            },
            failure: function (b, xhr) {
                this.createFolder(targetURL, null, this);
            },
            scope: this
        }, this);
    },

    createFolder: function (targetDir, callback, scope, options) {
        //Working directory not found, create it.
        scope.fileSystem.createDirectory({
            path: scope.fileSystem.getURI(targetDir),
            success: function () {
                if (callback)
                    callback.call(scope, options);
            },
            failure: function () {
                Ext4.Msg.alert("Error", "Couldn't generate working directory");
            },
            scope: scope
        }, scope);
    },

    getWorkingPath: function () {
        return this.fileSystem.concatPaths(this.fileSystem.getBaseURL(), 'Temp/' + this.workingDirectory);
    },
    getFullWorkingPath: function () {
        return this.fileSystem.getURI(this.getWorkingPath());
    },
    workingDirectory: '',
    containsFile: function(file) {
        var store = this.getStore();
        var idx = store.find(this.DATA_FILE, file.name);
        return store.getAt(idx);
    }
});
