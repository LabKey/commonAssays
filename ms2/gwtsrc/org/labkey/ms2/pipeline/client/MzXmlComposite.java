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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;
import java.util.List;

/**
 * User: billnelson@uky.edu
 * Date: Mar 31, 2008
 */
public class MzXmlComposite extends SearchFormComposite
{
    VerticalPanel instance = new VerticalPanel();
    private boolean hasWork;
    private boolean hasRun;
    private List<String> _fileInputNames;

    public MzXmlComposite()
    {
        initWidget(instance);
    }

    public void update(List<String> fileInputNames, List<String> fileInputStatus, boolean isActiveJobs)
    {
        hasRun = false;
        hasWork = !isActiveJobs;
        // Stash for refreshing without getting a new list of files
        _fileInputNames = fileInputNames;

        instance.clear();
        int num = (fileInputNames == null ? 0 : fileInputNames.size());
        if(num != 0)
        {
            StringBuilder names = new StringBuilder();
            for(int i = 0; i < num; i++)
            {
                if (i > 0)
                    names.append("<br>");
                names.append(fileInputNames.get(i));
                if(fileInputStatus != null && i < fileInputStatus.size())
                {
                    String status = fileInputStatus.get(i);
                    if (status != null && !"".equals(status))
                    {
                        names.append(" (<b>").append(status).append("</b>)");
                        hasRun = true;
                    }
                }
            }
            HTML html = new HTML(names.toString());
            html.setStylePrimaryName("labkey-read-only");
            instance.add(html);
        }
        else
        {
            instance.add(new Label("No mzXML files found"));
        }
    }

    public String getName()
    {
        return null;  //Not needed. Just Labels.
    }

    public void setName(String s)
    {
        //Not needed. Just Labels.
    }

    public void setWidth(String width)
    {
        //defaults are okay for now
    }

    public Widget getLabel()
    {
        Label label = new Label("Input file(s)");
        label.setStylePrimaryName(LABEL_STYLE_NAME);
        return label;
    }

    public String validate()
    {
        return null;  //No need for now.
    }

    public boolean hasWork()
    {
        return hasWork;
    }

    public void setHasWork(boolean hasWork)
    {
        this.hasWork = hasWork;
    }

    public boolean hasRun()
    {
        return hasRun;
    }

    public void setHasRun(boolean hasRun)
    {
        this.hasRun = hasRun;
    }

    public void clearStatus()
    {
        update(_fileInputNames, null, false);
    }

    @Override
    public void syncFormToXml(ParamParser params) throws SearchFormException
    {

    }

    @Override
    public String syncXmlToForm(ParamParser params)
    {
        return "";
    }
}
