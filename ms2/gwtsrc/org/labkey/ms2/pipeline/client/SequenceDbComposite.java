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

import com.google.gwt.user.client.ui.*;
import com.google.gwt.core.client.GWT;

import java.util.*;
import org.labkey.api.gwt.client.ui.ImageButton;

/**
 * User: billnelson@uky.edu
 * Date: Mar 13, 2008
 */

public abstract class SequenceDbComposite extends SearchFormComposite implements SourcesChangeEvents, SourcesClickEvents
{
    protected VerticalPanel instance = new VerticalPanel();
    protected ListBox sequenceDbPathListBox = new ListBox();
    protected ListBox sequenceDbListBox = new ListBox(false);
    protected Hidden sequenceDbHidden = new Hidden();
    protected Hidden sequenceDbPathHidden = new Hidden();
    protected Label sequenceDbLabel = new Label();
    protected HorizontalPanel dirPanel = new HorizontalPanel();
    protected RefreshButton refreshButton = new RefreshButton();
    protected HorizontalPanel refreshPanel = new HorizontalPanel();
    protected boolean hasDirectories;
    protected boolean foundDefaultDb;
    public static final String DB_DIR_ROOT = "<root>";


    public void init()
    {
        sequenceDbPathListBox.setVisibleItemCount(1);
        sequenceDbLabel.setStylePrimaryName("ms-readonly");
        dirPanel.add(sequenceDbPathListBox);
        instance.add(sequenceDbListBox);
        initWidget(instance);
    }

    public void update(List files, List directories, String defaultDb, List taxonomy)
    {
        setSequenceDbPathListBoxContents(directories,defaultDb);
        setSequenceDbsListBoxContents(files,defaultDb);
    }

    public void setSequenceDbPathListBoxContents(List paths, String defaultDb)
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
        if(paths == null || paths.size() == 0)
        {
            instance.remove(dirPanel);
            hasDirectories = false;
            return; 
        }
        else
        {
            if(instance.getWidgetIndex(dirPanel) == -1)
                instance.insert(dirPanel,0);
            hasDirectories = true;
        }
        Collections.sort(paths);
        sequenceDbPathListBox.addItem(DB_DIR_ROOT, "/");
        String dirName;
        for(Iterator it = paths.iterator() ; it.hasNext(); )
        {
            dirName = (String)it.next();
            if(dirName == null||dirName.equals(""))
                continue;
            sequenceDbPathListBox.addItem(dirName,dirName);
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

    public void setSequenceDbsListBoxContents(List files, String defaultDb)
    {
        if(defaultDb == null) defaultDb = "";
        if(files == null || files.size() == 0)
        {
            files = new ArrayList();
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
        if(files == null ||files.size() == 0) return;
        Collections.sort(files);
        String fileName;
        String path;
        for(Iterator it = files.iterator() ; it.hasNext(); )
        {
            path = "";
            fileName = (String)it.next();
            if(fileName == null||fileName.equals(""))
                continue;
            int index = defaultDb.lastIndexOf("/");
            if(index != -1)
            {
                path = defaultDb.substring(0,index +1);
                if(path.equals("/")) path = "";
            }
            sequenceDbListBox.addItem(fileName, path  + fileName);
        }
        setDefault(defaultDb);
    }
    
    public boolean setDefault(String defaultDb)
    {
        String path = "/";
        String name ="";
        if(defaultDb == null || defaultDb.length() == 0)
        {
            setFoundDefaultDb(false);
            return true;
        }
        int index = defaultDb.lastIndexOf('/');
        if(index != -1)
        {
            path = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
            name = defaultDb.substring(defaultDb.lastIndexOf('/') + 1);
        }
        else
        {
            name = defaultDb;
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
        int dbItemsCount = sequenceDbListBox.getItemCount();
        if(name.indexOf("/") == 0)
        {
            name = name.substring(1);
        }
        
        for(int i = 0; i < dbItemsCount; i++)
        {
            if(sequenceDbListBox.getValue(i).equals(name))
            {
                sequenceDbListBox.setSelectedIndex(i);
                setFoundDefaultDb(true);
                break;
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

    public String getSelectedDb()
    {
        int index = sequenceDbListBox.getSelectedIndex();
        if(index == -1) return "";
        return sequenceDbListBox.getValue(index);    
    }

    public void addChangeListener(ChangeListener listener)
    {
        sequenceDbPathListBox.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener changeListener) {

        sequenceDbPathListBox.removeChangeListener(changeListener);
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

    public void addRefreshClickListener(ClickListener listener)
    {
            refreshButton.addClickListener(listener);
    }

    public void removeRefreshClickListener(ClickListener listener)
    {
            refreshButton.removeClickListener(listener);
    }

    public void addClickListener(ClickListener listener)
    {
            sequenceDbListBox.addClickListener(listener);
    }

    public void removeClickListener(ClickListener listener)
    {
            sequenceDbListBox.removeClickListener(listener);
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
            int nameIndex = sequenceDbListBox.getSelectedIndex();
            if(nameIndex != -1)
            {
                sequenceDbName = sequenceDbListBox.getValue(nameIndex);
            }
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
                if(sequenceDbPathListBox.getItemCount() > 0)
                    instance.insert(dirPanel, 0);
                instance.add(sequenceDbListBox);
            }
        }
    }

    public Widget getLabel(String style)
    {
        labelWidget = new Label("Databases:");
        labelWidget.setStylePrimaryName(style);
        return labelWidget;
    }

    public String validate()
    {
        if(getSelectedDb().equals("") || getSelectedDb().equals("None found.") )
        {
            return "A sequence database must be selected.";
        }
        return "";
    }

    abstract public void setTaxonomyListBoxContents(List taxonomyList);
    abstract public String getSelectedTaxonomy();
    abstract public String setDefaultTaxonomy(String name);
    abstract public void addTaxonomyChangeListener(ChangeListener listener);
    abstract public void removeTaxonomyChangeListener(ChangeListener listener);

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
}
