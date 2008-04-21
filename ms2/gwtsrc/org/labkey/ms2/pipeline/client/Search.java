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
        actualSearchEngineLabel.setText(searchEngine);

        sequenceDbComposite.setName("sequenceDB");
        sequenceDbComposite.setWidth(INPUT_WIDTH);
        sequenceDbComposite.setVisibleItemCount(4);

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

        sequenceDbComposite.addChangeListener(new SequenceDbChangeListener(dirSequenceRoot));
        sequenceDbComposite.addClickListener(new RefreshSequenceDbPathsClickListener(dirSequenceRoot));


        RootPanel panel = RootPanel.get("org.labkey.ms2.pipeline.Search-Root");

        panel.add(searchFormPanel);
        setReadOnly(true);
    }

    private void setReadOnly(boolean readOnly)
    {
        setReadOnly(readOnly, false);
    }

    private void setReadOnly(boolean readOnly, boolean force)
    {
        protocolComposite.setReadOnly(readOnly);
        sequenceDbComposite.setReadOnly(readOnly);
        inputXmlComposite.setReadOnly(readOnly);
        saveProtocolCheckBox.setEnabled(!readOnly);
        if(protocolComposite.getSelectedProtocolValue().equals("new"))
            buttonPanel.remove(copyButton);
        else
            buttonPanel.insert(copyButton, 1);
        if(getErrors().length() > 0 || !mzXmlComposite.hasWork())
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
        mzXmlComposite.clearStatus();
        clearDisplay();
        getSearchService().getSequenceDbs(sequenceDbComposite.getSelectedDb(), dirSequenceRoot,searchEngine,
                new SequenceDbServiceCallback());
        setReadOnly(false);
        buttonPanel.remove(copyButton);
        protocolComposite.setFocus(true);
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
        int rows = 6;
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
        formGrid.setWidget(4, 0, inputXmlComposite.getLabel(labelStyle));
        formGrid.setWidget(4, 1, inputXmlComposite);
        formGrid.setWidget(5, 0, saveProtocolCheckBoxLabel);
        formGrid.setWidget(5, 1, saveProtocolCheckBox);
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

    private void appendError(String error)
    {
        appendDisplay(error);
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

    private void clearDisplay()
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

    private String getErrors()
    {
        return displayLabel.getText();
    }

    private void loading()
    {
        setMessage("LOADING...");
    }

    private class SearchButton extends ImageButton
    {
        SearchButton()
        {
            super("Search");
        }

        public void onClick(Widget sender)
        {
            searchFormPanel.submit();
        }
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
            event.setCancelled(getErrors().length() > 0);
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
                    appendError("Error: The submit did not return a destination.");
                    setReadOnly(false, true);
                }
                navigate(destination);
                appendMessage("Navigating to " + destination);

            }
            else if(results.indexOf("ERROR=") != -1)
            {
                String errorString = results.substring(results.indexOf("ERROR=") + 6);
                errorString  = errorString.trim();
                appendError("Error: " + errorString);
                setReadOnly(false, true);
            }
            else
            {
                appendError("Error: Unexpected response from server: " + results);
                setReadOnly(false);
            }
        }
    }

    private class SequenceDbServiceCallback implements AsyncCallback
    {
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
        }

        public void onSuccess(Object result)
        {
            clearDisplay();
            GWTSearchServiceResult gwtResult = (GWTSearchServiceResult)result;
            List sequenceDbs = gwtResult.getSequenceDBs();
            List sequenceDbPaths = gwtResult.getSequenceDbPaths();
            sequenceDbComposite.setSequenceDbPathListBoxContents(sequenceDbPaths,((GWTSearchServiceResult)result).getDefaultSequenceDb());
            sequenceDbComposite.setSequenceDbsListBoxContents(sequenceDbs,gwtResult.getDefaultSequenceDb());
            appendError(gwtResult.getErrors());
            sequenceDbComposite.selectDefaultDb(gwtResult.getDefaultSequenceDb());

        }
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
            inputXmlComposite.update(gwtResult.getProtocolXml());
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
            sequenceDbComposite.update(sequenceDbs,sequenceDbPaths, defaultDb );
            List protocols = gwtResult.getProtocols();
            String defaultProtocol = gwtResult.getSelectedProtocol();
            String protocolDescription = gwtResult.getProtocolDescription();
            protocolComposite.update(protocols, defaultProtocol, protocolDescription);
            mzXmlComposite.update(gwtResult.getMzXmlMap());
            inputXmlComposite.update(gwtResult.getProtocolXml());
            appendError(gwtResult.getErrors());
            if(defaultProtocol == null || defaultProtocol.equals(""))
                setReadOnly(false);
            else
                setReadOnly(true);
        }
    }

    private class SequenceDbChangeListener implements ChangeListener
    {

        private String  dirSequenceRoot;

        public SequenceDbChangeListener(String dirSequenceRoot)
        {
            this.dirSequenceRoot = dirSequenceRoot;
        }

        public void onChange(Widget widget)
        {
            ListBox listBox = (ListBox)widget;
            String dbDirectory = listBox.getValue(listBox.getSelectedIndex());
            loading();
            service.getSequenceDbs(dbDirectory, dirSequenceRoot, searchEngine,new SequenceDbServiceCallback());
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
            service.getProtocol(searchEngine, protocolName, dirRoot, dirSequenceRoot, PropertyUtil.getServerProperty("path"),new ProtocolServiceAsyncCallback());
        }
    }

    private class RefreshSequenceDbPathsClickListener implements ClickListener
    {
        private String dirSequenceRoot;

        public RefreshSequenceDbPathsClickListener(String dirSequenceRoot)
        {
            this.dirSequenceRoot = dirSequenceRoot;
        }
        public void onClick(Widget widget)
        {
            loading();
            service.refreshSequenceDbPaths(dirSequenceRoot,new SequenceDbServiceCallback());
        }
    }

}
