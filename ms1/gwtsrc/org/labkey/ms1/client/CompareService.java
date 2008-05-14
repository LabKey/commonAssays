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

package org.labkey.ms1.client;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.core.client.GWT;
import org.labkey.api.gwt.client.model.GWTComparisonResult;

/**
 * User: jeckels
 * Date: March 24, 2007
 */
public interface CompareService extends RemoteService
{
    /**
     * Utility/Convinience class.
     * Use CompareService.App.getInstance() to access static instance of CompareServiceAsync
     */
    public static class App
    {
        private static CompareServiceAsync ourInstance = null;

        public static synchronized CompareServiceAsync getInstance()
        {
            if (ourInstance == null)
            {
                ourInstance = (CompareServiceAsync) GWT.create(CompareService.class);
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "org.labkey.ms1.MS1VennDiagramView/CompareService");
            }
            return ourInstance;
        }

    }

    public GWTComparisonResult getFeaturesByPeptideComparison(String originalURL);

}