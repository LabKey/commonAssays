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

package org.labkey.ms2.pipeline.client.tandem;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.core.client.GWT;
import org.labkey.ms2.pipeline.client.Search;
import org.labkey.ms2.pipeline.client.SequenceDbComposite;

import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Apr 17, 2008
 */

/**
 * <code>XtandemSequenceDbComposite</code>
 */

public class XtandemSequenceDbComposite extends SequenceDbComposite
{
    public XtandemSequenceDbComposite(Search search)
    {
        super(search, true);

        dirPanel.insert(refreshPanel, 1);
        refreshPanel.add(refreshButton);
    }

    public void setWidth(String width)
    {
        super.setWidth(width);
        int intWidth = 0;
        StringBuffer num = new StringBuffer();
        StringBuffer type = new StringBuffer();
        StringBuffer newWidth = new StringBuffer();
        for(int i = 0; i < width.length(); i++)
        {
            char widthChar = width.charAt(i);
            if(Character.isDigit(widthChar))
            {
                num.append(widthChar);
            }
            else
            {
                type.append(widthChar);
            }
        }
        try
        {
            intWidth = Integer.parseInt(num.toString());
            if(intWidth < 60) throw new NumberFormatException("The database path ListBox is too small");
            newWidth.append(Integer.toString(intWidth - (60 + 3)));
            newWidth.append(type);
            sequenceDbPathListBox.setWidth(newWidth.toString());
            refreshPanel.setWidth("60px");

        }
        catch(NumberFormatException e)
        {}
    }

    public void addClickHandler(ClickHandler handler)
    {
        if(GWT.getTypeName(handler).equals("org.labkey.ms2.pipeline.client.Search$RefreshSequenceDbPathsClickListener"))
            refreshButton.addClickHandler(handler);
        else
            super.addClickHandler(handler);
    }

    public void setTaxonomyListBoxContents(List taxonomyList)
    {
        //No Mascot style taxonomy in X! Tandem
    }

    public String getSelectedTaxonomy()
    {
        //No Mascot style taxonomy in X! Tandem
        return null;
    }

    public String setDefaultTaxonomy(String name)
    {
        //No Mascot style taxonomy in X! Tandem
        return null;
    }

    public void addTaxonomyChangeHandler(ChangeHandler handler) {
        ///No Mascot style taxonomy in X! Tandem
    }
}
