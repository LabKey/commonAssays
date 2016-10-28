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

package org.labkey.ms2.client;

import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.gwt.client.ui.VennDiagramView;
import org.labkey.api.gwt.client.util.PropertyUtil;
import org.labkey.api.gwt.client.util.ServiceUtil;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 2, 2008
 */
public class MS2VennDiagramView extends VennDiagramView implements EntryPoint
{
    public static final String PEPTIDES_VIEW_PARAMETER_NAME = "PeptidesFilter.viewName";
    public static final String PROTEIN_GROUPS_VIEW_PARAMETER_NAME = "ProteinGroupsFilter.viewName";

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
            _service = GWT.create(CompareService.class);
            Map<String, String> params = new HashMap<String, String>();
            if (PropertyUtil.getServerProperty(PEPTIDES_VIEW_PARAMETER_NAME) != null)
            {
                params.put(PEPTIDES_VIEW_PARAMETER_NAME, PropertyUtil.getServerProperty(PEPTIDES_VIEW_PARAMETER_NAME));
            }
            if (PropertyUtil.getServerProperty(PROTEIN_GROUPS_VIEW_PARAMETER_NAME) != null)
            {
                params.put(PROTEIN_GROUPS_VIEW_PARAMETER_NAME, PropertyUtil.getServerProperty(PROTEIN_GROUPS_VIEW_PARAMETER_NAME));
            }
            ServiceUtil.configureEndpoint(_service, "compareService", null, params);
        }
        return _service;
    }
    
    protected void requestComparison(String originalURL, String comparisonGroup, AsyncCallback<GWTComparisonResult> callbackHandler)
    {
        if ("Proteins".equalsIgnoreCase(comparisonGroup))
        {
            getService().getProteinProphetComparison(originalURL, callbackHandler);
        }
        else if ("ProteinProphetCrosstab".equalsIgnoreCase(comparisonGroup))
        {
            getService().getQueryCrosstabComparison(originalURL, comparisonGroup, callbackHandler);
        }
        else if ("PeptideCrosstab".equalsIgnoreCase(comparisonGroup))
        {
            getService().getQueryCrosstabComparison(originalURL, comparisonGroup, callbackHandler);
        }
        else
        {
            throw new IllegalArgumentException("Unknown comparison group: " + comparisonGroup);
        }
    }

}
