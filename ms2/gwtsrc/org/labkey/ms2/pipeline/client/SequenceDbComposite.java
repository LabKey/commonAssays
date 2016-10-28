/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import java.util.*;

import org.labkey.api.gwt.client.ui.HelpPopup;
import org.labkey.api.gwt.client.ui.ImageButton;

/**
 * User: billnelson@uky.edu
 * Date: Mar 13, 2008
 */

public abstract class SequenceDbComposite extends SearchFormComposite
{
    protected final VerticalPanel instance = new VerticalPanel();
    protected final ListBox sequenceDbPathListBox = new ListBox();
    protected final ListBox sequenceDbListBox;
    protected final Hidden sequenceDbHidden = new Hidden();
    protected final Hidden sequenceDbPathHidden = new Hidden();
    protected final Label sequenceDbLabel = new Label();
    protected final HorizontalPanel dirPanel = new HorizontalPanel();
    protected final Label statusLabel = new Label();
    protected final RefreshButton refreshButton = new RefreshButton();
    protected final HorizontalPanel refreshPanel = new HorizontalPanel();
    protected boolean hasDirectories;
    protected boolean foundDefaultDb;
    public static final String DB_DIR_ROOT = "<root>";
    private final Search _search;

    protected SequenceDbComposite(Search search, boolean allowMultipleFASTAs)
    {
        _search = search;
        sequenceDbListBox = new ListBox(allowMultipleFASTAs);
        sequenceDbPathListBox.setVisibleItemCount(1);
        sequenceDbLabel.setStylePrimaryName("labkey-read-only");
        dirPanel.add(sequenceDbPathListBox);
        dirPanel.add(statusLabel);
        dirPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        statusLabel.setWidth("100px");
        statusLabel.setStylePrimaryName("labkey-message-strong");
        dirPanel.setSpacing(3);
        instance.add(dirPanel);
        instance.add(sequenceDbListBox);
        initWidget(instance);
    }

    public void update(List<String> files, List<String> directories, String defaultDb, List<String> taxonomy)
    {
        setSequenceDbPathListBoxContents(directories,defaultDb);
        setSequenceDbsListBoxContents(files,defaultDb);
    }

    public void setSequenceDbPathListBoxContents(List<String> paths, String defaultDb)
    {
        String defaultPath;
        if(defaultDb == null)
        {
            defaultPath = "";
        }
        else
        {
            defaultPath = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
        }
        sequenceDbPathListBox.clear();
        if (paths == null)
        {
            paths = new ArrayList<String>();
        }
        hasDirectories = !paths.isEmpty();

        sequenceDbPathListBox.addItem(DB_DIR_ROOT, "/");
        Collections.sort(paths, String.CASE_INSENSITIVE_ORDER);
        for (String dirName : paths)
        {
            if (dirName == null || dirName.equals(""))
                continue;
            sequenceDbPathListBox.addItem(dirName, dirName);
        }
        int pathCount = sequenceDbPathListBox.getItemCount();
        for(int i = 0; i < pathCount; i++)
        {
            String dir = sequenceDbPathListBox.getValue(i);
            if(dir.equals(defaultPath))
            {
                sequenceDbPathListBox.setSelectedIndex(i);
                break;
            }
        }
    }

    public void setSequenceDbsListBoxContents(List<String> files, String defaultDb)
    {
        if(defaultDb == null) defaultDb = "";
        if(files == null || files.size() == 0)
        {
            files = new ArrayList<String>();
            int index = defaultDb.lastIndexOf("/");
            if(index != -1)
            {
                files.add(defaultDb.substring(index + 1));
            }
            else
            {
                files.add(defaultDb);
            }
        }
        sequenceDbListBox.clear();
        if(files.isEmpty()) return;
        Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
        String path;
        for (String fileName : files)
        {
            path = "";
            if (fileName == null || fileName.equals(""))
                continue;
            int index = defaultDb.lastIndexOf("/");
            if (index != -1)
            {
                path = defaultDb.substring(0, index + 1);
                if (path.equals("/")) path = "";
            }
            sequenceDbListBox.addItem(fileName, path + fileName);
        }
        setDefault(defaultDb);
    }

    public boolean setDefault(String defaultDb)
    {
        String path = "/";
        if(defaultDb == null || defaultDb.length() == 0)
        {
            setFoundDefaultDb(false);
            return true;
        }
        int index = defaultDb.lastIndexOf('/');
        if(index != -1)
        {
            path = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
        }

        int pathItemsCount = sequenceDbPathListBox.getItemCount();
        if(hasDirectories)
        {
            boolean wrongDir = true;
            for(int i = 0; i < pathItemsCount; i++)
            {
                String listPath = sequenceDbPathListBox.getValue(i);
                boolean isSelected = sequenceDbPathListBox.isItemSelected(i);
                if(listPath.equals(path) && isSelected)
                {
                    wrongDir = false;
                    selectDefaultDb(defaultDb);
                    break;
                }
            }
            if( wrongDir)
            {
                setFoundDefaultDb(false);
            }
        }
        selectDefaultDb(defaultDb);
        return true;
    }

    public void selectDefaultDb(String name)
    {
        setFoundDefaultDb(false);
        if(name == null|| name.length() == 0) return;

        Set<String> defaultDbs = new HashSet<String>();

        for (String defaultDb : Arrays.asList(name.split(";")))
        {
            if(defaultDb.indexOf("/") == 0)
            {
                defaultDb = defaultDb.substring(1);
            }
            defaultDbs.add(defaultDb);
        }

        int dbItemsCount = sequenceDbListBox.getItemCount();
        for(int i = 0; i < dbItemsCount; i++)
        {
            if(defaultDbs.contains(sequenceDbListBox.getValue(i)))
            {
                sequenceDbListBox.setItemSelected(i, true);
                setFoundDefaultDb(true);
            }
            else
            {
                sequenceDbListBox.setItemSelected(i, false);
            }
        }
    }

    public boolean foundDefaultDb()
    {
        return foundDefaultDb;
    }

    public void setFoundDefaultDb(boolean found)
    {
        this.foundDefaultDb = found;
    }

    public String getSelectedDbPath()
    {
        int index = sequenceDbPathListBox.getSelectedIndex();
        if(index == -1) return "";
        return sequenceDbPathListBox.getValue(index);
    }

    public String getSelectedDb()
    {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (int i = 0; i < sequenceDbListBox.getItemCount(); i++)
        {
            if (sequenceDbListBox.isItemSelected(i))
            {
                sb.append(separator);
                sb.append(sequenceDbListBox.getValue(i));
                separator = ";";
            }
        }
        return sb.toString();
    }

    public void addChangeListener(ChangeHandler changeHandler)
    {
        sequenceDbPathListBox.addChangeHandler(changeHandler);
    }

    public void setName(String name) {
        sequenceDbListBox.setName(name);
        sequenceDbPathListBox.setName(name + "Path");
    }

    public String getName() {
        return sequenceDbListBox.getName();
    }

    public void setWidth(String width)
    {
        instance.setWidth(width);
        sequenceDbListBox.setWidth(width);
        sequenceDbPathListBox.setWidth(width);
    }

    public void setVisibleItemCount(int itemCount)
    {
        sequenceDbListBox.setVisibleItemCount(itemCount);
    }

    public void addRefreshClickHandler(ClickHandler handler)
    {
        refreshButton.addClickHandler(handler);
    }

    public void addClickHandler(ClickHandler handler)
    {
        sequenceDbListBox.addClickHandler(handler);
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        String path = "/";
        String sequenceDbName = "";
        String dbWidgetName = "";

        if(readOnly)
        {
            int pathIndex = sequenceDbPathListBox.getSelectedIndex();
            if( pathIndex != -1)
            {
                path = sequenceDbPathListBox.getValue(pathIndex);
            }
            sequenceDbName = getSelectedDb();
            instance.remove(dirPanel);
            dbWidgetName = sequenceDbPathListBox.getName();
            sequenceDbPathHidden.setName(dbWidgetName);
            sequenceDbPathHidden.setValue(path);

            instance.remove(sequenceDbListBox);
            sequenceDbLabel.setText(sequenceDbName);
            dbWidgetName = sequenceDbListBox.getName();
            sequenceDbHidden.setName(dbWidgetName);
            sequenceDbHidden.setValue(sequenceDbName);
            instance.insert(sequenceDbLabel, 0);
            instance.add(sequenceDbHidden);
            instance.add(sequenceDbPathHidden);
        }
        else
        {
            int labelIndex = instance.getWidgetIndex(sequenceDbLabel);
            if(labelIndex != -1)
            {
                instance.remove(sequenceDbLabel);
                instance.remove(sequenceDbHidden);
                instance.remove(sequenceDbPathHidden);
                instance.insert(dirPanel, 0);
                instance.add(sequenceDbListBox);
            }
        }
    }

    public Widget getLabel()
    {
        Label label = new Label("Protein database");
        label.setStylePrimaryName(LABEL_STYLE_NAME);
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(label);
        panel.add(new HelpPopup("Protein Database", "A protein database defines the set of sequences that are searched for matches against the input spectra. This is typically in the <a href=\"http://en.wikipedia.org/wiki/FASTA_format\" target=\"_blank\">FASTA format</a>."));
        return panel;
    }

    public String validate()
    {
        if(getSelectedDb().equals("") || getSelectedDb().equals("None found.") )
        {
            return "A sequence database must be selected.";
        }
        return "";
    }

    abstract public void setTaxonomyListBoxContents(List<String> taxonomyList);
    abstract public String getSelectedTaxonomy();
    abstract public String setDefaultTaxonomy(String name);
    abstract public void addTaxonomyChangeHandler(ChangeHandler listener);

    public void setLoading(boolean loading)
    {
        statusLabel.setText(loading ? "LOADING..." : "");
    }

    public void setEnabled(boolean paths, boolean files)
    {
        if (!paths)
        {
            sequenceDbPathListBox.clear();
        }
        sequenceDbPathListBox.setEnabled(paths);
        if (!files)
        {
            sequenceDbListBox.clear();
        }
        sequenceDbListBox.setEnabled(files);
        refreshButton.setEnabled(paths);
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        params.setInputParameter(ParameterNames.SEQUENCE_DB, getSelectedDb());
    }

    protected class RefreshButton extends ImageButton
    {
        RefreshButton()
        {
            super("Refresh");
        }

        public void onClick(Widget sender)
        {
        }
    }

    public String syncXmlToForm(ParamParser params)
    {
        String sequenceDb = params.getInputParameter(ParameterNames.SEQUENCE_DB);
        if(sequenceDb == null || sequenceDb.equals(""))
        {
            sequenceDb = getSelectedDb();
            if(sequenceDb == null || sequenceDb.equals(""))
            {
                return "";
            }
            else
            {
                try
                {
                    params.setInputParameter(ParameterNames.SEQUENCE_DB, sequenceDb);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set \"" + ParameterNames.SEQUENCE_DB + "\" in XML: " + e.getMessage();
                }
            }
        }
        else if(!sequenceDb.equals(getSelectedDb()))
        {
            _search.getSequenceDbs(sequenceDb);
        }

        String taxonomy = params.getInputParameter(ParameterNames.TAXONOMY);
        if(taxonomy == null || taxonomy.equals(""))
        {
            taxonomy = getSelectedTaxonomy();
            if(taxonomy != null && !taxonomy.equals(""))
            {
                try
                {
                    params.setInputParameter(ParameterNames.TAXONOMY, taxonomy);
                }
                catch(SearchFormException e)
                {
                    return "Cannot set protein, taxon in XML: " + e.getMessage();
                }
            }
        }
        else if(!taxonomy.equals(getSelectedTaxonomy()))
        {
            return setDefaultTaxonomy(taxonomy);
        }
        return "";
    }

    @Override
    public Set<String> getHandledParameterNames()
    {
        return new HashSet<String>(Arrays.asList(ParameterNames.SEQUENCE_DB, ParameterNames.TAXONOMY));
    }
}
