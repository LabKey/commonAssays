/*
 * Copyright (c) 2008-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2.pipeline.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.labkey.api.gwt.client.pipeline.GWTPipelineConfig;
import org.labkey.api.gwt.client.pipeline.PipelineGWTService;
import org.labkey.api.gwt.client.pipeline.PipelineGWTServiceAsync;
import org.labkey.api.gwt.client.ui.HelpPopup;
import org.labkey.api.gwt.client.ui.ImageButton;
import org.labkey.api.gwt.client.util.ErrorDialogAsyncCallback;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: billnelson@uky.edu
 * Date: Jan 29, 2008
 */
public class Search implements EntryPoint
{
    public static final String VALIDATION_FAILURE_PREFIX = "Validation failure: ";

    private static final    String                  INPUT_WIDTH = "575px";
    private                 VerticalPanel           subPanel = new VerticalPanel();
    private                 FormPanel               searchFormPanel = new FormPanel();
    private                 FlexTable               formGrid = new FlexTable();
    private                 Hidden                  pathHidden = new Hidden("path");
    private                 Hidden                  searchEngineHidden = new Hidden();
    private                 Hidden                  runSearch = new Hidden();
    private                 Hidden                  csfrHidden = new Hidden();
    private                 VerticalPanel           messagesPanel = new VerticalPanel();
    private                 ProtocolComposite       protocolComposite;
    // Nullable
    private                 SequenceDbComposite     sequenceDbComposite;
    private                 MzXmlComposite          mzXmlComposite = new MzXmlComposite();
    // Nullable
    private                 EnzymeComposite         enzymeComposite;
    // Nullable
    private                 ResidueModComposite     residueModComposite;
    private                 TPPComposite            tppComposite;
    private                 OtherParametersComposite otherParametersComposite;
    private                 Label                   searchEngineLabel = new Label();
    private                 Label                   actualSearchEngineLabel = new Label();
    private                 InputXmlComposite       inputXmlComposite;
    private                 HTML                    helpHTML = new HTML();
    private                 Label                   saveProtocolCheckBoxLabel = new Label();
    private                 CheckBox                saveProtocolCheckBox = new CheckBox();
    private                 HorizontalPanel         buttonPanel = new HorizontalPanel();
    private                 CancelButton            cancelButton = new CancelButton();
    private                 String                  returnURL;
    private                 String                  searchEngine;
    private                 SearchButton            searchButton = new SearchButton();
    private                 CopyButton              copyButton = new CopyButton();
    private                 boolean                 errors = false;
    private                 String                  path;  // Relative path of directory containing the files
    private                 String[]                fileNames;  // Names of files to be searched

    private                 HTML                    spacer;

    /** Map from subdirectory path to FASTA files */
    private Map<String, GWTSearchServiceResult> databaseCache = new HashMap<String, GWTSearchServiceResult>();
    private boolean sequencePathsLoaded = false;

    private PipelineGWTServiceAsync pipelineService = null;
    private SearchServiceAsync service = null;
    private List<SearchFormComposite> inputs = new ArrayList<SearchFormComposite>();

    private SearchServiceAsync getSearchService()
    {
        if (service == null)
        {
            service = GWT.create(SearchService.class);
            ServiceUtil.configureEndpoint(service, "searchService");
        }
        return service;
    }

    private PipelineGWTServiceAsync getPipelineService()
    {
        if (pipelineService == null)
        {
            pipelineService = GWT.create(PipelineGWTService.class);
            ServiceUtil.configureEndpoint(pipelineService, "pipelineConfiguration", "pipeline");
        }
        return pipelineService;
    }


    public void onModuleLoad()
    {
        spacer = new HTML("&nbsp;");
        spacer.setStylePrimaryName("labkey-message-strong");

        returnURL = PropertyUtil.getReturnURL();
        searchEngine = PropertyUtil.getServerProperty("searchEngine");
        String pipelineId = PropertyUtil.getServerProperty("pipelineId");
        SearchFormCompositeFactory compositeFactory = new SearchFormCompositeFactory(searchEngine);
        sequenceDbComposite = compositeFactory.getSequenceDbComposite(this);
        inputXmlComposite = compositeFactory.getInputXmlComposite();
        enzymeComposite = compositeFactory.getEnzymeComposite();
        residueModComposite = compositeFactory.getResidueModComposite(this);
        tppComposite = new TPPComposite();
        protocolComposite = new ProtocolComposite();
        otherParametersComposite = new OtherParametersComposite(inputs, inputXmlComposite);

        //form
        searchFormPanel.setAction(PropertyUtil.getServerProperty("targetAction"));
        searchFormPanel.setMethod(FormPanel.METHOD_POST);
        SearchFormHandler formHandler = new SearchFormHandler();
        searchFormPanel.addSubmitCompleteHandler(formHandler);
        searchFormPanel.addSubmitHandler(formHandler);
        searchFormPanel.setWidth("100%");

        runSearch.setName("runSearch");
        runSearch.setValue("true");

        protocolComposite.setName("protocol");
        protocolComposite.setWidth(INPUT_WIDTH);
        protocolComposite.setVisibleLines(4);

        searchEngineLabel.setText("Search engine");
        searchEngineLabel.setStylePrimaryName("labkey-form-label-nowrap");
        actualSearchEngineLabel.setStylePrimaryName("labkey-read-only");
        actualSearchEngineLabel.setText(searchEngine);

        if (sequenceDbComposite != null)
        {
            sequenceDbComposite.setName("sequenceDB");
            sequenceDbComposite.setWidth(INPUT_WIDTH);
            sequenceDbComposite.setVisibleItemCount(4);
        }

        if (enzymeComposite != null)
        {
            enzymeComposite.setWidth(INPUT_WIDTH);
        }

        if (residueModComposite != null)
        {
            residueModComposite.setWidth(INPUT_WIDTH);
        }

        inputXmlComposite.setName("configureXml");
        inputXmlComposite.setWidth("100%");

        helpHTML.setHTML("For detailed explanations of all available input parameters, see the <a href=\"" +
            PropertyUtil.getServerProperty("helpTopic") + "\" target=\"_blank\">" + searchEngine + " Documentation</a>.");

        saveProtocolCheckBoxLabel.setText("Save protocol");
        saveProtocolCheckBoxLabel.setStylePrimaryName("labkey-form-label-nowrap");
        saveProtocolCheckBox.setName("saveProtocol");
        saveProtocolCheckBox.setValue(Boolean.valueOf(PropertyUtil.getServerProperty("saveProtocol")));
        buttonPanel.setSpacing(5);
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);

        loadSubPanel();
        searchFormPanel.add(subPanel);

        //hidden fields
        path = PropertyUtil.getServerProperty("path");
        pathHidden.setValue(path);
        searchEngineHidden.setName("searchEngine");
        searchEngineHidden.setValue(searchEngine);

        csfrHidden.setName("X-LABKEY-CSRF");
        csfrHidden.setValue(ServiceUtil.getCsrfToken());

        fileNames = PropertyUtil.getServerProperty("file").split("/");
        for (String fileName : fileNames)
        {
            subPanel.add(new Hidden("file", fileName));
        }

        loading();
        getSearchService().getSearchServiceResult(searchEngine, path, fileNames, new SearchServiceAsyncCallback());

        getPipelineService().getLocationOptions(pipelineId, new ErrorDialogAsyncCallback<GWTPipelineConfig>()
        {
            public void onSuccess(GWTPipelineConfig result)
            {
                for (SearchFormComposite input : inputs)
                {
                    if (input instanceof PipelineConfigCallback)
                    {
                        ((PipelineConfigCallback) input).setPipelineConfig(result);
                    }
                }
            }
        });

        protocolComposite.addChangeHandler(new ProtocolChangeListener());

        if (sequenceDbComposite != null)
        {
            sequenceDbComposite.addChangeListener(new SequenceDbChangeListener());
            sequenceDbComposite.addRefreshClickHandler(new RefreshSequenceDbPathsClickListener());
            sequenceDbComposite.addClickHandler(new SequenceDbClickListener());
            sequenceDbComposite.addTaxonomyChangeHandler(new TaxonomyChangeListener());
        }

        if (enzymeComposite != null)
        {
            enzymeComposite.addChangeListener(new EnzymeChangeListener());
        }
        tppComposite.addChangeListener(new LocationChangeListener());
        otherParametersComposite.addChangeListener(new LocationChangeListener());

        inputXmlComposite.addChangeListener(new InputXmlChangeListener());


        RootPanel panel = RootPanel.get("org.labkey.ms2.pipeline.Search-Root");

        panel.add(searchFormPanel);
        setReadOnly(true);
    }

    public void setReadOnly(boolean readOnly)
    {
        setReadOnly(readOnly, false);
    }

    private void setReadOnly(boolean readOnly, boolean force)
    {
        protocolComposite.setReadOnly(readOnly);
        for (SearchFormComposite input : inputs)
        {
            input.setReadOnly(readOnly);
        }
        inputXmlComposite.setReadOnly(readOnly);
        saveProtocolCheckBox.setEnabled(!readOnly);
        if(protocolComposite.getSelectedProtocolValue().equals("new"))
            buttonPanel.remove(copyButton);
        else
            buttonPanel.insert(copyButton, 1);
        if (mzXmlComposite.hasRun() && !protocolComposite.getSelectedProtocolValue().equals("new"))
            searchButton.setText("Retry");
        else
            searchButton.setText("Search");
        if(hasErrors() || !mzXmlComposite.hasWork())
        {
            if(force) searchButton.setEnabled(true);
            else searchButton.setEnabled(false);
        }
        else
        {
            searchButton.setEnabled(true);
        }
    }

    private void newProtocol()
    {
        protocolComposite.newProtocol();
        inputXmlComposite.setDefault();
        changeProtocol();
    }

    private void copyAndEdit()
    {
        protocolComposite.copy();
        changeProtocol();

    }

    private void changeProtocol()
    {
        if (residueModComposite != null)
        {
            residueModComposite.clear();
        }
        mzXmlComposite.clearStatus();
        if (sequenceDbComposite != null)
        {
            sequenceDbComposite.setLoading(true);
            sequenceDbComposite.setEnabled(false, false);
            getSearchService().getSequenceDbs(sequenceDbComposite.getSelectedDb(), searchEngine, false,
                    new SequenceDbServiceCallback());
        }
        buttonPanel.remove(copyButton);
        protocolComposite.setFocus(true);
        String error = syncXml2Form();
        if(error.length() > 0)
        {
            clearErrors();
            appendError(error);
            setReadOnly(false);
            setSearchButtonEnabled(false);
        }
        else
        {
            clearErrors();
            setReadOnly(false);
        }
    }

    private void cancelForm()
    {
        if (null == returnURL || returnURL.length() == 0)
            History.back();
        else
            Window.Location.replace(returnURL);
    }

    private void loadSubPanel()
    {
        subPanel.add(pathHidden);
        subPanel.add(searchEngineHidden);
        subPanel.add(csfrHidden);
        subPanel.add(runSearch);
        subPanel.add(messagesPanel);
        subPanel.add(new Label("An MS2 search protocol is defined by a set of  options for the search engine and a set of protein databases to search."));
        subPanel.add(new Label("Choose an existing protocol or define a new one."));
        subPanel.setWidth("100%");

        inputs.add(mzXmlComposite);
        if (sequenceDbComposite != null)
        {
            inputs.add(sequenceDbComposite);
        }
        if (enzymeComposite != null)
        {
            inputs.add(enzymeComposite);
        }
        if (residueModComposite != null)
        {
            inputs.add(residueModComposite);
        }
        inputs.add(tppComposite);
        inputs.add(otherParametersComposite);

        int row = 0;
        formGrid.setStylePrimaryName("lk-fields-table");
        formGrid.setWidget(row, 0, protocolComposite.getLabel());
        formGrid.setWidget(row++, 1, protocolComposite);
        formGrid.setWidget(row, 0, searchEngineLabel);
        formGrid.setWidget(row++, 1, actualSearchEngineLabel);
        for (SearchFormComposite input : inputs)
        {
            formGrid.setWidget(row, 0, input.getLabel());
            formGrid.setWidget(row, 1, input);
            input.configureCompositeRow(formGrid, row++);
        }
        formGrid.setWidget(row, 0, inputXmlComposite.getLabel());
        formGrid.setWidget(row++, 1, inputXmlComposite);

        HorizontalPanel saveLabelPanel = new HorizontalPanel();
        saveLabelPanel.add(saveProtocolCheckBoxLabel);
        saveLabelPanel.add(new HelpPopup("Save protocol", "Whether or not this set of analysis parameters should be saved to use for future searches."));

        formGrid.setWidget(row, 0, saveLabelPanel);
        formGrid.setWidget(row++, 1, saveProtocolCheckBox);
        formGrid.getColumnFormatter().setWidth(1,"100%");

        for(int i = 0; i< formGrid.getRowCount(); i++)
        {
            formGrid.getCellFormatter().setStylePrimaryName(i,0, "labkey-form-label-nowrap");
            formGrid.getCellFormatter().setVerticalAlignment(i,0,HasVerticalAlignment.ALIGN_TOP);
            formGrid.getCellFormatter().setVerticalAlignment(i,1,HasVerticalAlignment.ALIGN_TOP);
        }

        subPanel.add(formGrid);
        subPanel.add(buttonPanel);
        subPanel.add(helpHTML);
    }

    public void appendError(String error)
    {
        if(error == null || error.trim().length() == 0)
        {
            return;
        }
        setErrors(true);
        new ErrorDialogBox(error);
    }

    private void appendMessage(String message)
    {
        if(message == null || message.trim().length() == 0)
        {
            return;
        }
        Label label = new Label(message);
        label.setStylePrimaryName("labkey-message-strong");
        if (messagesPanel.getWidgetCount() == 1 && messagesPanel.getWidget(0) == spacer)
        {
            messagesPanel.remove(spacer);
        }
        messagesPanel.add(label);
    }

    public void clearErrors()
    {
        setErrors(false);
        messagesPanel.clear();
        messagesPanel.add(spacer);
    }

    private void setError(String error)
    {
        clearErrors();
        appendError(error);
    }

    private void setDisplay(String text)
    {
        clearErrors();
        appendMessage(text);
    }

    private boolean hasErrors()
    {
        return errors;
    }

    private void setErrors(boolean errors)
    {
        this.errors = errors;
    }


    private void loading()
    {
        setDisplay("LOADING...");
    }

    public String syncForm2Xml()
    {
        try
        {
            for (SearchFormComposite input : inputs)
            {
                input.syncFormToXml(inputXmlComposite.params);
            }
            inputXmlComposite.writeXml();
        }
        catch(SearchFormException e)
        {
          return "Trouble adding selected parameter to input XML.\n" + e.getMessage();
        }
        if (residueModComposite != null)
        {
            return residueModComposite.validate();
        }
        return "";
    }

    public String syncXml2Form()
    {
        StringBuilder error = new StringBuilder();
        for (SearchFormComposite input : inputs)
        {
            error.append(input.syncXmlToForm(inputXmlComposite.params));
        }
        try
        {
            inputXmlComposite.writeXml();
        }
        catch(SearchFormException e)
        {
            error.append("Trouble writing XML: ").append(e.getMessage());
        }

        return error.toString();
    }

    public void setSearchButtonEnabled(boolean enabled)
    {
        searchButton.setEnabled(enabled);
    }

    public void getSequenceDbs(String sequenceDb)
    {
        getSearchService().getSequenceDbs(sequenceDb, searchEngine, false, new SequenceDbServiceCallback());
    }

    private class SearchButton extends ImageButton
    {
        SearchButton()
        {
            super("Search");
        }

        public void onClick(Widget sender)
        {
            clearErrors();
            boolean foundError = false;
            for (SearchFormComposite input : inputs)
            {
                String validation = input.validate();
                if (validation != null && !validation.isEmpty())
                {
                    appendError(validation);
                    foundError = true;
                }
            }
            if (!foundError)
            {
                searchFormPanel.submit();
            }
        }

//        public void setEnabled(boolean enabled)
//        {
//            super.setEnabled(enabled);
//            this.enabled = enabled;
//        }
//
//        public boolean isEnabled()
//        {
//            return enabled;
//        }
    }

    private class CopyButton extends ImageButton
    {
        CopyButton()
        {
            super("Copy & Edit");
        }

        public void onClick(Widget sender)
        {
            copyAndEdit();
        }
    }

    private class CancelButton extends ImageButton
    {
        CancelButton()
        {
            super("Cancel");
        }

        public void onClick(Widget sender)
        {
            cancelForm();
        }
    }

    public static class ErrorDialogBox
    {
        private DialogBox dialog = new DialogBox();
        private final String error;

        public ErrorDialogBox(String errorString)
        {
            error = errorString;
            Label label = new Label(error);
            label.setStylePrimaryName("labkey-error");
            dialog.setText("Error");
            VerticalPanel vPanel = new VerticalPanel();
            vPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER );
            vPanel.add(label);
            ImageButton okButton = new ImageButton("OK") {
                public void onClick(Widget sender)
                {
                    dialog.hide();
                    dialog = null;
                }
            };
            vPanel.add(okButton);
            dialog.setWidget(vPanel);
            dialog.center();
            okButton.setFocus(true);
        }
    }    
    private class SearchFormHandler implements FormPanel.SubmitCompleteHandler, FormPanel.SubmitHandler
    {
        public void onSubmit(FormPanel.SubmitEvent event)
        {
            clearErrors();
            appendError(protocolComposite.validate());
            if (sequenceDbComposite != null)
            {
                appendError(sequenceDbComposite.validate());
            }
            appendError(tppComposite.validate());
            if (hasErrors())
            {
                event.cancel();
            }
        }

        public void onSubmitComplete(FormPanel.SubmitCompleteEvent event)
        {
            String results = event.getResults();
            if(results == null)
            {
                appendError("The web server is not responding.");
            }
            else if(results.contains("SUCCESS="))
            {
                String destination = results.substring(results.indexOf("SUCCESS=") + 8);
                destination  = destination.trim();
                if(destination.length() == 0)
                {
                    appendError("The submit did not return a destination.");
                    setReadOnly(false, true);
                }
                Window.Location.replace(destination);
                appendMessage("Navigating to " + destination);

            }
            else if(results.contains("ERROR="))
            {
                String errorString = results.substring(results.indexOf("ERROR=") + 6);
                errorString  = errorString.trim();
                appendError(errorString);
                if (errorString.startsWith(VALIDATION_FAILURE_PREFIX) && Boolean.TRUE.equals(saveProtocolCheckBox.getValue()))
                {
                    // The server didn't like our protocol definition. Refresh so we get a read-only view of it.
                    getSearchService().getSearchServiceResult(searchEngine, path, fileNames, new SearchServiceAsyncCallback());
                }
                else
                {
                    setReadOnly(false, true);
                }
            }
            else if(results.contains("User does not have permission"))
            {
                cancelForm();
            }
            else
            {
                appendError("Unexpected response from server: " + results);
                setReadOnly(false);
            }
        }
    }

    private class SequenceDbServiceCallback extends ErrorDialogAsyncCallback<GWTSearchServiceResult>
    {
        public void reportFailure(String message, Throwable caught)
        {
            if(caught.getMessage().contains("User does not have permission"))
            {
                cancelForm();
            }
            else if(!(caught.getClass().getName().equals("com.google.gwt.user.client.rpc.InvocationException")
                    && caught.getMessage().length() == 0))
            {
                super.reportFailure(message, caught);
            }
        }

        public void onSuccess(GWTSearchServiceResult gwtResult)
        {
            databaseCache.put(gwtResult.getCurrentPath(), gwtResult);
            updateDatabases(gwtResult);
        }
    }

    private void updateDatabases(GWTSearchServiceResult gwtResult)
    {
        String selectedDb = sequenceDbComposite.getSelectedDbPath();
        if (sequencePathsLoaded && selectedDb != null && !gwtResult.getCurrentPath().equals(selectedDb))
        {
            // This is a listing for a directory that we're not trying to render anymore. The user has probably
            // changed the selected path again before the response from the first request finished. 
            return;
        }

        if (sequenceDbComposite.isReadOnly())
        {
            // The user probably messed up the XML and we're not showing the database selection UI, so don't try to do anything 
            return;
        }

        sequenceDbComposite.setLoading(false);
        List<String> sequenceDbs = gwtResult.getSequenceDBs();
        List<String> sequenceDbPaths = gwtResult.getSequenceDbPaths();

        sequenceDbComposite.setSequenceDbPathListBoxContents(sequenceDbPaths,
                gwtResult.getCurrentPath());

        sequencePathsLoaded = true;

        if(sequenceDbs != null)
        {
            sequenceDbComposite.setSequenceDbsListBoxContents(sequenceDbs, gwtResult.getDefaultSequenceDb());
        }
        appendError(gwtResult.getErrors());
        sequenceDbComposite.selectDefaultDb(gwtResult.getDefaultSequenceDb());

        // Check to be sure that all of the FASTAs referenced by the saved protocol are still available
        // (and already selected) in the FASTA list
        Set<String> inputDbs = new HashSet<String>(Arrays.asList(inputXmlComposite.params.getInputParameter(ParameterNames.SEQUENCE_DB).split(";")));
        Set<String> listBoxDbs = new HashSet<String>(Arrays.asList(sequenceDbComposite.getSelectedDb().split(";")));

        if(!inputDbs.isEmpty() && !inputDbs.equals(listBoxDbs))
        {
            appendError("The database entered for the input XML label \"" + ParameterNames.SEQUENCE_DB + "\" cannot be found"
                + " at this FASTA root. " + inputDbs + " vs " + listBoxDbs);
            inputXmlComposite.params.removeInputParameter(ParameterNames.SEQUENCE_DB);
            try
            {
                inputXmlComposite.writeXml();
            }
            catch(SearchFormException ignored) {}
            sequenceDbComposite.setEnabled(true, true);
            return;
        }
        appendError(syncForm2Xml());
        sequenceDbComposite.setEnabled(true, true);
    }

    private class ProtocolServiceAsyncCallback extends ErrorDialogAsyncCallback<GWTSearchServiceResult>
    {
        public void onSuccess(GWTSearchServiceResult gwtResult)
        {
            clearErrors();
            appendError(gwtResult.getErrors());
            List<String> protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getFileInputNames(), gwtResult.getFileInputStatus(),
                    gwtResult.isActiveJobs());
            appendError(inputXmlComposite.update(gwtResult.getProtocolXml()));
            appendError(syncXml2Form());
            String defaultDb = gwtResult.getDefaultSequenceDb();
            if (sequenceDbComposite != null)
            {
                sequenceDbComposite.setSequenceDbsListBoxContents(null, defaultDb);
            }
            if(defaultProtocol == null || defaultDb.equals(""))
                setReadOnly(false);
            else
                setReadOnly(true);
        }
    }

    private class SearchServiceAsyncCallback extends ErrorDialogAsyncCallback<GWTSearchServiceResult>
    {
        public void onSuccess(GWTSearchServiceResult gwtResult)
        {
            setError(PropertyUtil.getServerProperty("errors"));
            List<String> sequenceDbs = gwtResult.getSequenceDBs();
            List<String> sequenceDbPaths = gwtResult.getSequenceDbPaths();
            String defaultDb = gwtResult.getDefaultSequenceDb();
            List<String> taxonomy = gwtResult.getMascotTaxonomyList();
            if (sequenceDbComposite != null)
            {
                sequenceDbComposite.update(sequenceDbs, sequenceDbPaths, defaultDb, taxonomy);
            }
            List<String> protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getFileInputNames(), gwtResult.getFileInputStatus(),
                    gwtResult.isActiveJobs());
            if (enzymeComposite != null)
            {
                enzymeComposite.update(gwtResult.getEnzymeMap());
            }
            if (residueModComposite != null)
            {
                residueModComposite.update(gwtResult.getMod0Map(), gwtResult.getMod1Map());
            }
            appendError(inputXmlComposite.update(gwtResult.getProtocolXml()));
            appendError(syncXml2Form());
            appendError(gwtResult.getErrors());
            if(defaultProtocol == null || defaultProtocol.equals(""))
                setReadOnly(false);
            else
                setReadOnly(true);
        }
    }

    private class SequenceDbChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent e)
        {
            String dbDirectory = sequenceDbComposite.getSelectedDbPath();
            sequenceDbComposite.setLoading(true);
            sequenceDbComposite.setEnabled(true, false);
            inputXmlComposite.params.removeInputParameter(ParameterNames.SEQUENCE_DB);

            if (databaseCache.containsKey(dbDirectory))
            {
                final GWTSearchServiceResult gwtResult = databaseCache.get(dbDirectory);
                if (gwtResult != null)
                {
                    updateDatabases(gwtResult);
                }
            }
            else
            {
                service.getSequenceDbs(dbDirectory, searchEngine, false, new SequenceDbServiceCallback());

                // Stick in a null so that we don't request it again
                databaseCache.put(dbDirectory, null);
            }
        }
    }

    private class ProtocolChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent e)
        {
            clearErrors();
            String protocolName = protocolComposite.getSelectedProtocolValue();
            if(protocolName.equals("new"))
            {
                newProtocol();
                return;
            }
            loading();
            service.getProtocol(searchEngine, protocolName, path, fileNames, new ProtocolServiceAsyncCallback());
        }
    }

    private class SequenceDbClickListener implements ClickHandler
    {
        public void onClick(ClickEvent e)
        {
            String db = sequenceDbComposite.getSelectedDb();
            if(db.length() > 0 && !db.equals("None found."))
            {
                clearErrors();
                inputXmlComposite.params.removeInputParameter(ParameterNames.SEQUENCE_DB);
                String error = syncForm2Xml();
                if(error.length() > 0 )
                {
                    appendError(error);
                    searchButton.setEnabled(false);
                }
                else
                {
                    setReadOnly(false);
                }
            }
        }
    }

    private class TaxonomyChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent e)
        {
            String tax = sequenceDbComposite.getSelectedTaxonomy();
            if(tax.length() > 0)
            {
                inputXmlComposite.params.removeInputParameter(ParameterNames.TAXONOMY);
                clearErrors();
                String error = syncForm2Xml();
                if(error.length() > 0 )
                {
                    appendError(error);
                    searchButton.setEnabled(false);
                }
                else
                {
                    setReadOnly(false);
                }
            }
        }
    }

    private class LocationChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent event)
        {
            syncForm2Xml();
        }
    }

    private class EnzymeChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent e)
        {
            String enz = enzymeComposite.getSelectedEnzyme();
            if(enz.length() > 0)
            {
                inputXmlComposite.params.removeInputParameter(ParameterNames.ENZYME);
                clearErrors();
                String error = syncForm2Xml();
                if(error.length() > 0 )
                {
                    appendError(syncForm2Xml());
                    searchButton.setEnabled(false);
                }
                else
                {
                    setReadOnly(false);
                }
            }
        }
    }

    private class RefreshSequenceDbPathsClickListener implements ClickHandler
    {
        public void onClick(ClickEvent e)
        {
            databaseCache.clear();
            sequencePathsLoaded = false;
            String defaultSequenceDb = inputXmlComposite.params.getInputParameter(ParameterNames.SEQUENCE_DB);
            service.getSequenceDbs(defaultSequenceDb, searchEngine, true, new SequenceDbServiceCallback());
            sequenceDbComposite.setLoading(true);
            sequenceDbComposite.setEnabled(false, false);
        }
    }

    private class InputXmlChangeListener implements ChangeHandler
    {
        public void onChange(ChangeEvent e)
        {
            String error = inputXmlComposite.validate();
            if(error.length() > 0)
            {
                clearErrors();
                appendError(error);
                setReadOnly(true);
                inputXmlComposite.setReadOnly(false);
                return;
            }
            error = syncXml2Form();
            if(error.length() > 0)
            {
                clearErrors();
                appendError(error);
                searchButton.setEnabled(false);
                return;
            }
            clearErrors();
            setReadOnly(false);
        }
    }
}
