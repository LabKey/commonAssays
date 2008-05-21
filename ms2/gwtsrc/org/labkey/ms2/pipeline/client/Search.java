/*
 * Copyright (c) 2008 LabKey Corporation
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
    private                 Hidden                  pathHidden = new Hidden();
    private                 Hidden                  searchEngineHidden = new Hidden();
    private                 Hidden                  runSearch = new Hidden();
    private                 Label                   displayLabel = new Label();
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
    private                 String                  dirSequenceRoot;

    private Map databaseCache = new HashMap();

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
        returnURL = PropertyUtil.getServerProperty("returnURL");
        searchEngine = PropertyUtil.getServerProperty("searchEngine");
        SearchFormCompositeFactory compositeFactory = new SearchFormCompositeFactory(searchEngine);
        sequenceDbComposite = compositeFactory.getSequenceDbComposite();
        inputXmlComposite = compositeFactory.getInputXmlComposite();
        enzymeComposite = compositeFactory.getEnzymeComposite();
        residueModComposite = compositeFactory.getResidueModComposite(this);
        protocolComposite = new ProtocolComposite();
        dirSequenceRoot = PropertyUtil.getServerProperty("dirSequenceRoot");
        String dirRoot = PropertyUtil.getServerProperty("dirRoot");
        loading();
        getSearchService().getSearchServiceResult(searchEngine, dirSequenceRoot,dirRoot,
                PropertyUtil.getServerProperty("path"), new SearchServiceAsyncCallback());
        //form
        searchFormPanel.setAction(PropertyUtil.getServerProperty("action"));
        searchFormPanel.setMethod(FormPanel.METHOD_POST);
        searchFormPanel.addFormHandler(new SearchFormHandler());
        searchFormPanel.setWidth("100%");

        //hidden fields
        pathHidden.setName("path");
        pathHidden.setValue(PropertyUtil.getServerProperty("path"));
        searchEngineHidden.setName("searchEngine");
        searchEngineHidden.setValue(searchEngine);
        runSearch.setName("runSearch");
        runSearch.setValue("true");

        displayLabel.setWordWrap(false);
        displayLabel.setHeight("15px");

        protocolComposite.setName("protocol");
        protocolComposite.setWidth(INPUT_WIDTH);
        protocolComposite.setVisibleLines(4);

        searchEngineLabel.setText("Search engine:");
        searchEngineLabel.setStylePrimaryName("ms-searchform-nowrap");
        actualSearchEngineLabel.setStylePrimaryName("ms-readonly");
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
        saveProtocolCheckBoxLabel.setStylePrimaryName("ms-searchform-nowrap");
        saveProtocolCheckBox.setName("saveProtocol");
        saveProtocolCheckBox.setChecked(new Boolean(PropertyUtil.getServerProperty("saveProtocol")).booleanValue());
        buttonPanel.setSpacing(5);
        buttonPanel.add(searchButton);
        buttonPanel.add(cancelButton);

        loadSubPanel();
        searchFormPanel.add(subPanel);

        protocolComposite.addChangeListener(new ProtocolChangeListener(dirRoot));

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
        DatabaseRequestKey key = new DatabaseRequestKey("/", dirSequenceRoot, searchEngine);
        getSearchService().getSequenceDbs(sequenceDbComposite.getSelectedDb(), dirSequenceRoot,searchEngine,
                new SequenceDbServiceCallback(key));
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
        String labelStyle = "ms-searchform-nowrap";

        subPanel.add(pathHidden);
        subPanel.add(searchEngineHidden);
        subPanel.add(runSearch);
        subPanel.add(displayLabel);
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

        for(int i = 0; i < rows; i++)
        {
            formGrid.getCellFormatter().setStylePrimaryName(i,0, "ms-top-color");
            formGrid.getCellFormatter().setStylePrimaryName(i,1, "ms-top");
        }
        subPanel.add(formGrid);
        subPanel.add(buttonPanel);
        subPanel.add(helpHTML);
    }

    public void appendError(String error)
    {
        if(error.trim().length() ==  0)   return;
        appendDisplay("ERROR:" + error);
        displayLabel.setStylePrimaryName("cpas-error");
    }

    private void appendMessage(String message)
    {
        appendDisplay(message);
        displayLabel.setStylePrimaryName("cpas-message-strong");
    }

    private void appendDisplay(String display)
    {
        if(display == null || display.length() == 0) return;
        StringBuffer startingError = new StringBuffer(displayLabel.getText());
        if(startingError.length() > 0) startingError.append("\n");
        startingError.append(display);
        displayLabel.setText(startingError.toString());
    }

    public void clearDisplay()
    {
        displayLabel.setText("");
    }

    private void setMessage(String message)
    {
        setDisplay(message);
        displayLabel.setStylePrimaryName("cpas-message-strong");
    }

    private void setError(String error)
    {
        setDisplay(error);
        displayLabel.setStylePrimaryName("cpas-error");
    }

    private void setDisplay(String text)
    {
        if(text == null) displayLabel.setText("");
        else displayLabel.setText(text);
    }

    private boolean hasErrors()
    {
        return(displayLabel.getText().length() > 0);
    }

    private void loading()
    {
        setMessage("LOADING...");
    }

    public String syncForm2Xml()
    {
        String sequenceDb = sequenceDbComposite.getSelectedDb();
        String taxonomy   = sequenceDbComposite.getSelectedTaxonomy();
        String enzyme     = enzymeComposite.getSelectedEnzyme();
        Map staticMods    = residueModComposite.getStaticMods();
        Map dynamicMods   = residueModComposite.getDynamicMods();

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
        Map modMap = residueModComposite.getModMap(residueModComposite.STATIC);
        Map staticMods = inputXmlComposite.getStaticMods(modMap);
        residueModComposite.setSelectedStaticMods(staticMods);

        modMap = residueModComposite.getModMap(residueModComposite.DYNAMIC);
        Map dynamicMods = inputXmlComposite.getDynamicMods(modMap);
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
            getSearchService().getSequenceDbs(sequenceDb, dirSequenceRoot, searchEngine, new SequenceDbServiceCallback(new DatabaseRequestKey(sequenceDb, dirSequenceRoot, searchEngine)));
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

    private class SearchFormHandler implements FormHandler
    {
        public void onSubmit(FormSubmitEvent event)
        {
            clearDisplay();
            appendError(protocolComposite.validate());
            appendError(sequenceDbComposite.validate());
            event.setCancelled(hasErrors());
        }

        public void onSubmitComplete(FormSubmitCompleteEvent event)
        {
            String results = event.getResults();
            if(results.indexOf("SUCCESS=") != -1)
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
            else
            {
                appendError("Unexpected response from server: " + results);
                setReadOnly(false);
            }
        }
    }

    private class SequenceDbServiceCallback implements AsyncCallback
    {
        private final DatabaseRequestKey _key;

        public SequenceDbServiceCallback(DatabaseRequestKey key)
        {
            _key = key;
        }

        public void onFailure(Throwable caught)
        {
            if(!(GWT.getTypeName(caught).equals("com.google.gwt.user.client.rpc.InvocationException")
                    && caught.getMessage().length() == 0))
                Window.alert(caught.getMessage() + GWT.getTypeName(caught));
            databaseCache.remove(_key);
        }

        public void onSuccess(Object result)
        {
            GWTSearchServiceResult gwtResult = (GWTSearchServiceResult)result;
            databaseCache.put(_key, gwtResult);
            updateDatabases(gwtResult);
        }
    }
    
    private void updateDatabases(GWTSearchServiceResult gwtResult)
    {
        clearDisplay();
        List sequenceDbs = gwtResult.getSequenceDBs();
        List sequenceDbPaths = gwtResult.getSequenceDbPaths();
        sequenceDbComposite.setSequenceDbPathListBoxContents(sequenceDbPaths,
                gwtResult.getDefaultSequenceDb());
        if(sequenceDbs != null)
        {
            sequenceDbComposite.setSequenceDbsListBoxContents(sequenceDbs,gwtResult.getDefaultSequenceDb());
        }
        appendError(gwtResult.getErrors());
        sequenceDbComposite.selectDefaultDb(gwtResult.getDefaultSequenceDb());

        if(inputXmlComposite.getSequenceDb().length() > 0 &&
            !inputXmlComposite.getSequenceDb().equals(sequenceDbComposite.getSelectedDb()))
        {
            appendError("The database entered for the input XML label \"pipeline, database\" cannot be found"
            + " at this fasta root.");
            return;
        }
        appendError(syncForm2Xml());
    }

    private class ProtocolServiceAsyncCallback implements AsyncCallback
    {
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
        }

         public void onSuccess(Object result)
        {
            clearDisplay();
            GWTSearchServiceResult gwtResult = (GWTSearchServiceResult)result;
            appendError(gwtResult.getErrors());
            List protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getMzXmlMap());
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

    private class SearchServiceAsyncCallback implements AsyncCallback
    {
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
        }

        public void onSuccess(Object result)
        {
            setError(PropertyUtil.getServerProperty("errors"));
            GWTSearchServiceResult gwtResult = (GWTSearchServiceResult)result;
            List sequenceDbs = gwtResult.getSequenceDBs();
            List sequenceDbPaths = gwtResult.getSequenceDbPaths();
            String defaultDb = gwtResult.getDefaultSequenceDb();
            List taxonomy = gwtResult.getMascotTaxonomyList();
            sequenceDbComposite.update(sequenceDbs,sequenceDbPaths, defaultDb, taxonomy );
            List protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getMzXmlMap());
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
            loading();
            inputXmlComposite.removeSequenceDb();

            DatabaseRequestKey key = new DatabaseRequestKey(dbDirectory, dirSequenceRoot, searchEngine);
            if (databaseCache.containsKey(key))
            {
                final GWTSearchServiceResult gwtResult = (GWTSearchServiceResult) databaseCache.get(key);
                if (gwtResult != null)
                {
                    updateDatabases(gwtResult);
                }
            }
            else
            {
                service.getSequenceDbs(dbDirectory, dirSequenceRoot, searchEngine, new SequenceDbServiceCallback(key));
            }
        }
    }

    private class ProtocolChangeListener implements ChangeListener
    {
        private String  dirRoot;

        public ProtocolChangeListener(String dirRoot)
        {
            this.dirRoot = dirRoot;
        }

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
            service.getProtocol(searchEngine, protocolName, dirRoot, dirSequenceRoot,
                    PropertyUtil.getServerProperty("path"),new ProtocolServiceAsyncCallback());
        }
    }

    private class SequenceDbClickListener implements ClickListener
    {

        public void onClick(Widget widget)
        {
            String db = sequenceDbComposite.getSelectedDb();
            if(db.length() > 0)
            {
                inputXmlComposite.removeSequenceDb();
                appendError(syncForm2Xml());
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
            loading();
            databaseCache.clear();
            service.refreshSequenceDbPaths(dirSequenceRoot,new SequenceDbServiceCallback(new DatabaseRequestKey(dirSequenceRoot, dirSequenceRoot, searchEngine)));
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

    private static class DatabaseRequestKey
    {
        private final String _dbDirectory;
        private final String _dirSequenceRoot;
        private final String _searchEngine;

        public DatabaseRequestKey(String dbDirectory, String dirSequenceRoot, String searchEngine)
        {
            _dbDirectory = dbDirectory;
            _dirSequenceRoot = dirSequenceRoot;
            _searchEngine = searchEngine;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof DatabaseRequestKey)) return false;

            DatabaseRequestKey that = (DatabaseRequestKey) o;

            if (_dbDirectory != null ? !_dbDirectory.equals(that._dbDirectory) : that._dbDirectory != null)
                return false;
            if (_dirSequenceRoot != null ? !_dirSequenceRoot.equals(that._dirSequenceRoot) : that._dirSequenceRoot != null)
                return false;
            if (_searchEngine != null ? !_searchEngine.equals(that._searchEngine) : that._searchEngine != null)
                return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = (_dbDirectory != null ? _dbDirectory.hashCode() : 0);
            result = 31 * result + (_dirSequenceRoot != null ? _dirSequenceRoot.hashCode() : 0);
            result = 31 * result + (_searchEngine != null ? _searchEngine.hashCode() : 0);
            return result;
        }
    }
}
