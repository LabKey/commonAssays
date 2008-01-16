/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms1.query;

import org.labkey.api.view.ActionURL;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.ms1.MS1Controller;

import java.util.ArrayList;

/**
 * Can be used to recreate a set of features filters based on a set of query string parameters
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 14, 2008
 * Time: 12:47:33 PM
 */
public class FeaturesFilterFactory
{
    public static final String NAMESPACE = "fvf";
    public static final String NAMESPACE_PREFIX = NAMESPACE + ".";
    public static final String PARAM_SOURCE_CONTAINER = "sc";

    public static ArrayList<FeaturesFilter> createFilters(ActionURL url, User user)
    {
        ArrayList<FeaturesFilter> filters = new ArrayList<FeaturesFilter>();

        //start with the container filter
        String scParam = url.getParameter(NAMESPACE_PREFIX + PARAM_SOURCE_CONTAINER);
        String subfoldersParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.PepSearchForm.ParamNames.subfolders.name());
        Container root = null != scParam && scParam.length() > 0 
                ? ContainerManager.getForPath(scParam)
                : ContainerManager.getForPath(url.getExtraPath());
        filters.add(new ContainerFilter(root, null != subfoldersParam && subfoldersParam.length() > 0, user));

        //RunFilter
        String runParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.ShowFeaturesForm.ParamNames.runId.name());
        if(null != runParam && runParam.length() > 0)
            filters.add(new RunFilter(runParam));

        //PeptideFilter
        String pepParam = url.getParameter(NAMESPACE_PREFIX + MS1Controller.PepSearchForm.ParamNames.pepSeq.name());
        String exactParam = url.getParameter(MS1Controller.PepSearchForm.ParamNames.exact.name());
        if(null != pepParam && pepParam.length() > 0)
            filters.add(new PeptideFilter(pepParam, null != exactParam && exactParam.length() > 0));

        return filters;
    }

}
