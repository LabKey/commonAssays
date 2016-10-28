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

package org.labkey.ms1.client;

import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.gwt.client.ui.VennDiagramView;
import org.labkey.api.gwt.client.util.ServiceUtil;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;

/**
 * User: jeckels
 * Date: March 24, 2007
 */
public class MS1VennDiagramView extends VennDiagramView implements EntryPoint
{
    public static final String FEATURES_BY_PEPTIDE = "FeaturesByPeptide";

    public void onModuleLoad()
    {
        RootPanel panel = RootPanel.get("org.labkey.ms1.MS1VennDiagramView-Root");
        initialize(panel);
        requestComparison();
    }

    private CompareServiceAsync _service;
    private CompareServiceAsync getService()
    {
        if (_service == null)
        {
            _service = GWT.create(CompareService.class);
            ServiceUtil.configureEndpoint(_service, "compareService");
        }
        return _service;
    }

    protected void requestComparison(String originalURL, String comparisonGroup, AsyncCallback<GWTComparisonResult> callbackHandler)
    {
        if (FEATURES_BY_PEPTIDE.equalsIgnoreCase(comparisonGroup))
        {
            getService().getFeaturesByPeptideComparison(originalURL, callbackHandler);
        }
        else
        {
            throw new IllegalArgumentException("Unknown comparison group: " + comparisonGroup);
        }
    }

}