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

package org.labkey.ms2.client;

import org.labkey.api.gwt.client.ui.VennDiagramView;
import org.labkey.api.gwt.client.util.ServiceUtil;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;

/**
 * User: jeckels
 * Date: Feb 2, 2008
 */
public class MS2VennDiagramView extends VennDiagramView implements EntryPoint
{
    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.ms2.MS2VennDiagramView-Root");
        initialize(panel);
        requestComparison();
    }
    
    private CompareServiceAsync _service;
    private CompareServiceAsync getService()
    {
        if (_service == null)
        {
            _service = (CompareServiceAsync) GWT.create(CompareService.class);
            ServiceUtil.configureEndpoint(_service, "compareService");
        }
        return _service;
    }
    
    protected void requestComparison(String originalURL, String comparisonGroup, AsyncCallback callbackHandler)
    {
        if ("Peptides".equalsIgnoreCase(comparisonGroup))
        {
            getService().getPeptideComparison(originalURL, callbackHandler);
        }
        else if ("Proteins".equalsIgnoreCase(comparisonGroup))
        {
            getService().getProteinProphetComparison(originalURL, callbackHandler);
        }
        else if ("ProteinProphetCrosstab".equalsIgnoreCase(comparisonGroup))
        {
            getService().getProteinProphetCrosstabComparison(originalURL, callbackHandler);
        }
        else
        {
            throw new IllegalArgumentException("Unknown comparison group: " + comparisonGroup);
        }
    }

}
