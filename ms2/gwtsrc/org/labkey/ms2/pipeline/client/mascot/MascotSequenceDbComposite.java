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

package org.labkey.ms2.pipeline.client.mascot;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.*;
import org.labkey.ms2.pipeline.client.ParamParser;
import org.labkey.ms2.pipeline.client.ParameterNames;
import org.labkey.ms2.pipeline.client.Search;
import org.labkey.ms2.pipeline.client.SearchFormException;
import org.labkey.ms2.pipeline.client.SequenceDbComposite;

import java.util.List;

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
    protected VerticalPanel labelWidget;


    public MascotSequenceDbComposite(Search search)
    {
        super(search, true);
        sequenceDbLabel.setStylePrimaryName("ms-readonly");
        instance.add(sequenceDbListBox);
        taxonomyReadOnly.setStylePrimaryName("labkey-read-only");
        taxonomyListBox.setVisibleItemCount(1);
        instance.add(taxonomyListBox);
        labelWidget = new VerticalPanel();
        initLabel(false);
    }

    private void initLabel(boolean readonly)
    {
        taxonomyLabel.setText("Taxonomy");
        labelWidget.clear();
        if(readonly)
        {
            databaseLabel.setText("Database");
            labelWidget.add(databaseLabel);
            labelWidget.add(taxonomyLabel);
        }
        else
        {
            databaseLabel.setText("Databases");
            labelWidget.add(databaseLabel);
            labelWidget.add(new Label(" "));
            labelWidget.add(new Label(" "));
            labelWidget.add(new Label(" "));
            labelWidget.add(new Label(" "));
            labelWidget.add(taxonomyLabel);
        }
    }

    public void update(List<String> files, List<String> directories, String defaultDb, List<String> taxonomy)
    {
        super.update(files, directories, defaultDb, taxonomy);
        setTaxonomyListBoxContents(taxonomy);
    }

    public void setTaxonomyListBoxContents(List<String> taxonomy)
    {
        if(taxonomy == null) return;
        for (String s : taxonomy)
        {
            taxonomyListBox.addItem(s);
        }
    }

    public Widget getLabel()
    {
        setLabelStyle();
        return labelWidget;
    }

    private void setLabelStyle()
    {
        String style = "labkey-form-label";
        int widgetCount = labelWidget.getWidgetCount();
        for(int i = 0; i < widgetCount; i++)
        {
            Label l = (Label)labelWidget.getWidget(i);
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
        String sequenceDbName;
        String dbWidgetName;
        String taxonomyName = "";

        if(readOnly)
        {
            sequenceDbName = getSelectedDb();

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
                instance.add(taxonomyListBox);
            }
        }
        initLabel(readOnly);
    }

    public String getSelectedDbPath()
    {
        return "";
    }

    public String getSelectedTaxonomy()
    {
        int index = taxonomyListBox.getSelectedIndex();
        if(index == -1) return "";
        return taxonomyListBox.getValue(index);
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {
        super.syncFormToXml(params);
        params.setInputParameter(ParameterNames.TAXONOMY, getSelectedTaxonomy());
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

    public void addTaxonomyChangeHandler(ChangeHandler handler) {
        taxonomyListBox.addChangeHandler(handler);
    }
}
