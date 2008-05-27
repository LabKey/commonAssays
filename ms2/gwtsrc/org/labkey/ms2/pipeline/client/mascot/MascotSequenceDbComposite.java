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

package org.labkey.ms2.pipeline.client.mascot;

import org.labkey.ms2.pipeline.client.SequenceDbComposite;
import org.labkey.api.gwt.client.util.StringUtils;

import java.util.List;

import com.google.gwt.user.client.ui.*;

/**
 * User: billnelson@uky.edu
 * Date: Apr 22, 2008
 */

/**
 * <code>MascotSequenceDbComposite</code>
 */
public class MascotSequenceDbComposite extends SequenceDbComposite
{

    private ListBox taxonomyListBox  = new ListBox();
    private Label taxonomyReadOnly = new Label();
    private Label taxonomyLabel = new Label();
    private Label databaseLabel = new Label();


    public MascotSequenceDbComposite()
    {
        super();
        init();
    }

    public void init()
    {
        super.init();
        taxonomyReadOnly.setStylePrimaryName("ms-readonly");
        taxonomyListBox.setVisibleItemCount(1);
        instance.add(taxonomyListBox);
        labelWidget = new VerticalPanel();
        initLabel(false);
    }

        private void initLabel(boolean readonly)
    {
        taxonomyLabel.setText("Taxonomy:");
        ((VerticalPanel)labelWidget).clear();
        if(readonly)
        {
            databaseLabel.setText("Database:");
            ((VerticalPanel)labelWidget).add(databaseLabel);
            ((VerticalPanel)labelWidget).add(taxonomyLabel);
        }
        else
        {
            databaseLabel.setText("Databases:");
            ((VerticalPanel)labelWidget).add(databaseLabel);
            ((VerticalPanel)labelWidget).add(new Label(" "));
            ((VerticalPanel)labelWidget).add(new Label(" "));
            ((VerticalPanel)labelWidget).add(new Label(" "));
            ((VerticalPanel)labelWidget).add(new Label(" "));
            ((VerticalPanel)labelWidget).add(taxonomyLabel);
        }
    }

    public void update(List files, List directories, String defaultDb, List taxonomy)
    {
        super.update(files, directories, defaultDb, taxonomy);
        setTaxonomyListBoxContents(taxonomy);
    }

    public void setTaxonomyListBoxContents(List taxonomy)
    {
        if(taxonomy == null) return;
        for(int i = 0; i < taxonomy.size(); i++)
        {
            taxonomyListBox.addItem((String)taxonomy.get(i));
        }
    }

    public Widget getLabel(String style)
    {
        setLabelStyle(style);
        return labelWidget;
    }

    private void setLabelStyle(String style)
    {
        int widgetCount = ((VerticalPanel)labelWidget).getWidgetCount();
        for(int i = 0; i < widgetCount; i++)
        {
            Label l = (Label)((VerticalPanel)labelWidget).getWidget(i);
            l.setStylePrimaryName(style);
        }
    }

    public void setWidth(String width)
    {
        super.setWidth(width);
        taxonomyListBox.setWidth(width);
    }

    public void setReadOnly(boolean readOnly)
    {
        super.setReadOnly(readOnly);
        String path = "/";
        String sequenceDbName = " ";
        String dbWidgetName = "";
        String taxonomyName = "";

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

            int taxonomyIndex = taxonomyListBox.getSelectedIndex();
            if(taxonomyIndex != -1)
            {
                taxonomyName = taxonomyListBox.getValue(taxonomyIndex);
            }
            instance.remove(taxonomyListBox);
            taxonomyReadOnly.setText(taxonomyName);

            instance.insert(sequenceDbLabel, 0);
            instance.insert(taxonomyReadOnly,1);
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
            labelIndex = instance.getWidgetIndex(taxonomyReadOnly);
            if( labelIndex != -1)
            {
                instance.remove(taxonomyReadOnly);
                instance.insert(taxonomyListBox,1);
            }
        }
        initLabel(readOnly);
    }

    public String getSelectedTaxonomy()
    {
        int index = taxonomyListBox.getSelectedIndex();
        if(index == -1) return "";
        return taxonomyListBox.getValue(index);
    }

    public String setDefaultTaxonomy(String tax)
    {
        int taxCount = taxonomyListBox.getItemCount();
        boolean foundTax = false;
        for(int i = 0; i < taxCount; i++)
        {
            if(tax.equals(taxonomyListBox.getValue(i)))
            {
                taxonomyListBox.setSelectedIndex(i);
                foundTax = true;
            }
        }
        if(!foundTax)
            return "The taxon '" + tax + "' is not found on the Mascot server.";
        return "";
    }

    public void addTaxonomyChangeListener(ChangeListener listener) {
        taxonomyListBox.addChangeListener(listener);
    }

    public void removeTaxonomyChangeListener(ChangeListener listener) {
        taxonomyListBox.removeChangeListener(listener);
    }
}
