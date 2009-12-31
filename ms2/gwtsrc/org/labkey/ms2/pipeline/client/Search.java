/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.Window;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;
import org.labkey.api.gwt.client.ui.ImageButton;
import org.labkey.api.gwt.client.ui.WindowUtil;

import java.util.*;

/**
 * User: billnelson@uky.edu
 * Date: Jan 29, 2008
 */
public class Search implements EntryPoint
{
    private static final    String                  INPUT_WIDTH = "575px";
    private                 VerticalPanel           subPanel = new VerticalPanel();
    private                 FormPanel               searchFormPanel = new FormPanel();
    private                 Grid                    formGrid = new Grid();
    private                 Hidden                  pathHidden = new Hidden("path");
    private                 Hidden                  searchEngineHidden = new Hidden();
    private                 Hidden                  runSearch = new Hidden();
    private                 VerticalPanel           messagesPanel = new VerticalPanel();
    private                 ProtocolComposite       protocolComposite;
    private                 SequenceDbComposite     sequenceDbComposite;
    private                 MzXmlComposite          mzXmlComposite = new MzXmlComposite();
    private                 EnzymeComposite         enzymeComposite;
    private                 ResidueModComposite     residueModComposite;
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

    private SearchServiceAsync service = null;
    private SearchServiceAsync getSearchService()
    {
        if (service == null)
        {
            service = (SearchServiceAsync) GWT.create(SearchService.class);
            ServiceUtil.configureEndpoint(service, "searchService");
        }
        return service;
    }


    public void onModuleLoad()
    {
        spacer = new HTML("&nbsp;");
        spacer.setStylePrimaryName("labkey-message-strong");

        returnURL = PropertyUtil.getServerProperty("returnURL");
        searchEngine = PropertyUtil.getServerProperty("searchEngine");
        SearchFormCompositeFactory compositeFactory = new SearchFormCompositeFactory(searchEngine);
        sequenceDbComposite = compositeFactory.getSequenceDbComposite();
        inputXmlComposite = compositeFactory.getInputXmlComposite();
        enzymeComposite = compositeFactory.getEnzymeComposite();
        residueModComposite = compositeFactory.getResidueModComposite(this);
        protocolComposite = new ProtocolComposite();

        //form
        searchFormPanel.setAction(PropertyUtil.getServerProperty("targetAction"));
        searchFormPanel.setMethod(FormPanel.METHOD_POST);
        searchFormPanel.addFormHandler(new SearchFormHandler());
        searchFormPanel.setWidth("100%");

        runSearch.setName("runSearch");
        runSearch.setValue("true");

        protocolComposite.setName("protocol");
        protocolComposite.setWidth(INPUT_WIDTH);
        protocolComposite.setVisibleLines(4);

        searchEngineLabel.setText("Search engine:");
        searchEngineLabel.setStylePrimaryName("labkey-form-label-nowrap");
        actualSearchEngineLabel.setStylePrimaryName("labkey-read-only");
        actualSearchEngineLabel.setText(searchEngine);

        sequenceDbComposite.setName("sequenceDB");
        sequenceDbComposite.setWidth(INPUT_WIDTH);
        sequenceDbComposite.setVisibleItemCount(4);

        enzymeComposite.setWidth(INPUT_WIDTH);

        residueModComposite.setWidth(INPUT_WIDTH);

        inputXmlComposite.setName("configureXml");
        inputXmlComposite.setWidth("100%");

        helpHTML.setHTML("For detailed explanations of all available input parameters, see the <a href=\"" +
            PropertyUtil.getServerProperty("helpTopic") + "\">" + searchEngine + " Documentation</a>");

        saveProtocolCheckBoxLabel.setText("Save protocol:");
        saveProtocolCheckBoxLabel.setStylePrimaryName("labkey-form-label-nowrap");
        saveProtocolCheckBox.setName("saveProtocol");
        saveProtocolCheckBox.setChecked(new Boolean(PropertyUtil.getServerProperty("saveProtocol")).booleanValue());
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

        fileNames = PropertyUtil.getServerProperty("file").split("/");
        for (String fileName : fileNames)
        {
            subPanel.add(new Hidden("file", fileName));
        }

        loading();
        getSearchService().getSearchServiceResult(searchEngine, path, fileNames, new SearchServiceAsyncCallback());

        protocolComposite.addChangeListener(new ProtocolChangeListener());

        sequenceDbComposite.addChangeListener(new SequenceDbChangeListener());
        sequenceDbComposite.addRefreshClickListener(new RefreshSequenceDbPathsClickListener());
        sequenceDbComposite.addClickListener(new SequenceDbClickListener());
        sequenceDbComposite.addTaxonomyChangeListener(new TaxonomyChangeListener());
        enzymeComposite.addChangeListener(new EnzymeChangeListener());

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
        sequenceDbComposite.setReadOnly(readOnly);
        enzymeComposite.setReadOnly(readOnly);
        residueModComposite.setReadOnly(readOnly);
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
        residueModComposite.clear();
        mzXmlComposite.clearStatus();
        sequenceDbComposite.setLoading(true);
        sequenceDbComposite.setEnabled(false, false);
        getSearchService().getSequenceDbs(sequenceDbComposite.getSelectedDb(), searchEngine, false,
                new SequenceDbServiceCallback());
        buttonPanel.remove(copyButton);
        protocolComposite.setFocus(true);
        String error = syncXml2Form();
        if(error.length() > 0)
        {
            clearDisplay();
            appendError(error);
            setReadOnly(false);
            setSearchButtonEnabled(false);
        }
        else
        {
            clearDisplay();
            setReadOnly(false);
        }
    }

    private void cancelForm()
    {
        if (null == returnURL || returnURL.length() == 0)
            back();
        else
            navigate(returnURL);
    }


    public static native void navigate(String url) /*-{
      $wnd.location.href = url;
    }-*/;


    public static native void back() /*-{
        $wnd.history.back();
    }-*/;



    private void loadSubPanel()
    {
        int rows = 8;
        int cols = 2;
        String labelStyle = "labkey-form-label-nowrap";

        subPanel.add(pathHidden);
        subPanel.add(searchEngineHidden);
        subPanel.add(runSearch);
        subPanel.add(messagesPanel);
        subPanel.add(new Label("An MS2 search protocol is defined by a set of  options for the search engine and a set of protein databases to search."));
        subPanel.add(new Label("Choose an existing protocol or define a new one."));
        subPanel.setWidth("100%");

        formGrid.resize(rows , cols);
        formGrid.setWidget(0, 0, protocolComposite.getLabel(labelStyle));
        formGrid.setWidget(0, 1, protocolComposite);
        formGrid.setWidget(1, 0, searchEngineLabel);
        formGrid.setWidget(1, 1, actualSearchEngineLabel);
        formGrid.setWidget(2, 0, mzXmlComposite.getLabel(labelStyle));
        formGrid.setWidget(2, 1, mzXmlComposite);
        formGrid.setWidget(3, 0, sequenceDbComposite.getLabel(labelStyle));
        formGrid.setWidget(3, 1, sequenceDbComposite);
        formGrid.setWidget(4, 0, enzymeComposite.getLabel(labelStyle));
        formGrid.setWidget(4, 1, enzymeComposite);
        formGrid.setWidget(5, 0, residueModComposite.getLabel(labelStyle));
        formGrid.setWidget(5, 1, residueModComposite);
        formGrid.setWidget(6, 0, inputXmlComposite.getLabel(labelStyle));
        formGrid.setWidget(6, 1, inputXmlComposite);
        formGrid.setWidget(7, 0, saveProtocolCheckBoxLabel);
        formGrid.setWidget(7, 1, saveProtocolCheckBox);
        formGrid.getColumnFormatter().setWidth(1,"100%");

        for(int i = 0; i< rows; i++)
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

    public void clearDisplay()
    {
        setErrors(false);
        messagesPanel.clear();
        messagesPanel.add(spacer);
    }

    private void setError(String error)
    {
        clearDisplay();
        appendError(error);
    }

    private void setDisplay(String text)
    {
        clearDisplay();
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
        String sequenceDb = sequenceDbComposite.getSelectedDb();
        String taxonomy   = sequenceDbComposite.getSelectedTaxonomy();
        String enzyme     = enzymeComposite.getSelectedEnzyme();
        Map<String, String> staticMods    = residueModComposite.getStaticMods();
        Map<String, String> dynamicMods   = residueModComposite.getDynamicMods();

        try
        {
            inputXmlComposite.setSequenceDb(sequenceDb);
            inputXmlComposite.setTaxonomy(taxonomy);
            inputXmlComposite.setEnzyme(enzyme);
            inputXmlComposite.setStaticMods(staticMods);
            inputXmlComposite.setDynamicMods(dynamicMods);
            inputXmlComposite.writeXml();
        }
        catch(SearchFormException e)
        {
          return "Trouble adding selected parameter to input XML.\n" + e.getMessage();
        }
        return residueModComposite.validate();
    }

    public String syncXml2Form()
    {
        StringBuffer error = new StringBuffer();
        error.append(syncTaxonomyXML2Form());
        error.append(syncSequenceDbXML2Form());
        error.append(syncEnzymeXML2Form());
        error.append(syncResidueMod2Form());
        try
        {
            inputXmlComposite.writeXml();
        }
        catch(SearchFormException e)
        {
            error.append("Trouble writing XML: " + e.getMessage());
        }

        return error.toString();
    }

    private String syncResidueMod2Form()
    {
        Map<String, String> modMap = residueModComposite.getModMap(residueModComposite.STATIC);
        Map<String, String> staticMods = inputXmlComposite.getStaticMods(modMap);
        residueModComposite.setSelectedStaticMods(staticMods);

        modMap = residueModComposite.getModMap(residueModComposite.DYNAMIC);
        Map<String, String> dynamicMods = inputXmlComposite.getDynamicMods(modMap);
        residueModComposite.setSelectedDynamicMods(dynamicMods);
        try
        {
           inputXmlComposite.setStaticMods(staticMods);
           inputXmlComposite.setDynamicMods(dynamicMods);
        }
        catch(SearchFormException e)
        {
          return "Trouble adding residue modification params to input XML.\n" + e.getMessage();
        }

        return residueModComposite.validate();
    }

    private String syncEnzymeXML2Form()
    {
        String enzyme = inputXmlComposite.getEnzyme();
        if(enzyme == null || enzyme.equals(""))
        {
            enzyme = enzymeComposite.getSelectedEnzyme();
            if(enzyme == null || enzyme.equals(""))
            {
                return "";
            }
            else
            {
                try
                {
                    inputXmlComposite.setEnzyme(enzyme);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set the enzyme in XML: " + e.getMessage();
                }
            }
        }
        else if(!enzyme.equals(enzymeComposite.getSelectedEnzyme()))
        {
            return enzymeComposite.setSelectedEnzyme(enzyme);
        }
        return "";
    }

    private String syncSequenceDbXML2Form()
    {
        String sequenceDb = inputXmlComposite.getSequenceDb();
        if(sequenceDb == null || sequenceDb.equals(""))
        {
            sequenceDb = sequenceDbComposite.getSelectedDb();
            if(sequenceDb == null || sequenceDb.equals(""))
            {
                return "";
            }
            else
            {
                try
                {
                    inputXmlComposite.setSequenceDb(sequenceDb);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set pipeline, database in XML: " + e.getMessage();
                }
            }
        }
        else if(sequenceDb.equals(sequenceDbComposite.getSelectedDb()))
        {
            return "";
        }
        else
        {
            getSearchService().getSequenceDbs(sequenceDb, searchEngine, false, new SequenceDbServiceCallback());
        }
        return "";
    }

    private String syncTaxonomyXML2Form()
    {
        String error = "";
        String taxonomy = inputXmlComposite.getTaxonomy();
        if(taxonomy == null || taxonomy.equals(""))
        {
            taxonomy = sequenceDbComposite.getSelectedTaxonomy();
            if(taxonomy != null && !taxonomy.equals(""))
            {
                try
                {
                    inputXmlComposite.setTaxonomy(taxonomy);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set protein, taxon in XML: " + e.getMessage();
                }
            }
        }
        else if(!taxonomy.equals(sequenceDbComposite.getSelectedTaxonomy()))
        {
            error = sequenceDbComposite.setDefaultTaxonomy(taxonomy);
            if(error.length() > 0)
            {
                return error;
            }
        }
        return error;
    }

    public void setSearchButtonEnabled(boolean enabled)
    {
        searchButton.setEnabled(enabled);
    }

       private class SearchButton extends ImageButton
    {
        boolean enabled = true;
        SearchButton()
        {
            super("Search");
        }

        public void onClick(Widget sender)
        {
            searchFormPanel.submit();
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
    private class ErrorDialogBox
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
    private class SearchFormHandler implements FormHandler
    {
        public void onSubmit(FormSubmitEvent event)
        {
            clearDisplay();
            appendError(protocolComposite.validate());
            appendError(sequenceDbComposite.validate());
            event.setCancelled(hasErrors());
            WindowUtil.scrollTo(0, 0);
        }

        public void onSubmitComplete(FormSubmitCompleteEvent event)
        {
            String results = event.getResults();
            if(results == null)
            {
                appendError("The LabKey server is not responding.");
            }
            else if(results.indexOf("SUCCESS=") != -1)
            {
                String destination = results.substring(results.indexOf("SUCCESS=") + 8);
                destination  = destination.trim();
                if(destination.length() == 0)
                {
                    appendError("The submit did not return a destination.");
                    setReadOnly(false, true);
                }
                navigate(destination);
                appendMessage("Navigating to " + destination);

            }
            else if(results.indexOf("ERROR=") != -1)
            {
                String errorString = results.substring(results.indexOf("ERROR=") + 6);
                errorString  = errorString.trim();
                appendError(errorString);
                setReadOnly(false, true);
            }
            else if(results.indexOf("User does not have permission") != -1)
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

    private class SequenceDbServiceCallback implements AsyncCallback<GWTSearchServiceResult>
    {
        public void onFailure(Throwable caught)
        {
            if(caught.getMessage().indexOf("User does not have permission") != -1)
            {
                cancelForm();
            }
            else if(!(GWT.getTypeName(caught).equals("com.google.gwt.user.client.rpc.InvocationException")
                    && caught.getMessage().length() == 0))
            {
                Window.alert(caught.getMessage() + GWT.getTypeName(caught));
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

        if(inputXmlComposite.getSequenceDb().length() > 0 &&
            !inputXmlComposite.getSequenceDb().equals(sequenceDbComposite.getSelectedDb()))
        {
            appendError("The database entered for the input XML label \"pipeline, database\" cannot be found"
            + " at this fasta root.");
            inputXmlComposite.removeSequenceDb();
            try{
                inputXmlComposite.writeXml();
            }catch(SearchFormException e){}
            sequenceDbComposite.setEnabled(true, true);
            return;
        }
        appendError(syncForm2Xml());
        sequenceDbComposite.setEnabled(true, true);
    }

    private class ProtocolServiceAsyncCallback implements AsyncCallback<GWTSearchServiceResult>
    {
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
        }

        public void onSuccess(GWTSearchServiceResult gwtResult)
        {
            clearDisplay();
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
            sequenceDbComposite.setSequenceDbsListBoxContents(null, defaultDb);
            if(defaultProtocol == null || defaultDb.equals(""))
                setReadOnly(false);
            else
                setReadOnly(true);
        }
    }

    private class SearchServiceAsyncCallback implements AsyncCallback<GWTSearchServiceResult>
    {
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
        }

        public void onSuccess(GWTSearchServiceResult gwtResult)
        {
            setError(PropertyUtil.getServerProperty("errors"));
            List<String> sequenceDbs = gwtResult.getSequenceDBs();
            List<String> sequenceDbPaths = gwtResult.getSequenceDbPaths();
            String defaultDb = gwtResult.getDefaultSequenceDb();
            List<String> taxonomy = gwtResult.getMascotTaxonomyList();
            sequenceDbComposite.update(sequenceDbs,sequenceDbPaths, defaultDb, taxonomy );
            List<String> protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getFileInputNames(), gwtResult.getFileInputStatus(),
                    gwtResult.isActiveJobs());
            enzymeComposite.update(gwtResult.getEnzymeMap());
            residueModComposite.update(gwtResult.getMod0Map(), gwtResult.getMod1Map());
            appendError(inputXmlComposite.update(gwtResult.getProtocolXml()));
            appendError(syncXml2Form());
            appendError(gwtResult.getErrors());
            if(defaultProtocol == null || defaultProtocol.equals(""))
                setReadOnly(false);
            else
                setReadOnly(true);
        }
    }

    private class SequenceDbChangeListener implements ChangeListener
    {
        public void onChange(Widget widget)
        {
            ListBox listBox = (ListBox)widget;
            String dbDirectory = listBox.getValue(listBox.getSelectedIndex());
            sequenceDbComposite.setLoading(true);
            sequenceDbComposite.setEnabled(true, false);
            inputXmlComposite.removeSequenceDb();

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

    private class ProtocolChangeListener implements ChangeListener
    {
        public void onChange(Widget widget)
        {
            clearDisplay();
            ListBox listBox = (ListBox)widget;
            String protocolName = listBox.getValue(listBox.getSelectedIndex());
            if(protocolName.equals("new"))
            {
                newProtocol();
                return;
            }
            loading();
            service.getProtocol(searchEngine, protocolName, path, fileNames, new ProtocolServiceAsyncCallback());
        }
    }

    private class SequenceDbClickListener implements ClickListener
    {

        public void onClick(Widget widget)
        {
            String db = sequenceDbComposite.getSelectedDb();
            if(db.length() > 0 && !db.equals("None found."))
            {
                clearDisplay();
                inputXmlComposite.removeSequenceDb();
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

    private class TaxonomyChangeListener implements ChangeListener
    {
        public void onChange(Widget widget)
        {
            String tax = sequenceDbComposite.getSelectedTaxonomy();
            if(tax.length() > 0)
            {
                inputXmlComposite.removeTaxonomy();
                clearDisplay();
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

    private class EnzymeChangeListener implements ChangeListener
    {
        public void onChange(Widget widget)
        {
            String enz = enzymeComposite.getSelectedEnzyme();
            if(enz.length() > 0)
            {
                inputXmlComposite.removeEnzyme();
                clearDisplay();
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

    private class RefreshSequenceDbPathsClickListener implements ClickListener
    {
        public void onClick(Widget widget)
        {
            databaseCache.clear();
            sequencePathsLoaded = false;
            String defaultSequenceDb = inputXmlComposite.getSequenceDb();
            service.getSequenceDbs(defaultSequenceDb, searchEngine, true, new SequenceDbServiceCallback());
            sequenceDbComposite.setLoading(true);
            sequenceDbComposite.setEnabled(false, false);
        }
    }

    private class InputXmlChangeListener implements ChangeListener
    {

        public void onChange(Widget widget)
        {
            String error = inputXmlComposite.validate();
            if(error.length() > 0)
            {
                clearDisplay();
                appendError(error);
                setReadOnly(true);
                inputXmlComposite.setReadOnly(false);
                return;
            }
            error = syncXml2Form();
            if(error.length() > 0)
            {
                clearDisplay();
                appendError(error);
                searchButton.setEnabled(false);
                return;
            }
            clearDisplay();
            setReadOnly(false);
        }
    }
}
