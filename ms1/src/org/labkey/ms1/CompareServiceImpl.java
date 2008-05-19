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

package org.labkey.ms1;

import org.apache.log4j.Logger;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms1.client.CompareService;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.view.CompareRunsView;

/**
 * User: jeckels
 * Date: Mar 24, 2008
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);

    public CompareServiceImpl(ViewContext context)
    {
        super(context);
    }
    
    public GWTComparisonResult getFeaturesByPeptideComparison(String originalURL)
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);

            int[] runIds = PageFlowUtil.toInts(url.getParameter("runIds").split(","));
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            return new CompareRunsView(new MS1Schema(getUser(), _context.getContainer()), runIds, url).createComparisonResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw UnexpectedException.wrap(e);
        }
    }
    
}
