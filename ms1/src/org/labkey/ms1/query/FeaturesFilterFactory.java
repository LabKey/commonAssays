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
import org.labkey.common.util.Pair;

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

    public static ArrayList<FeaturesFilter> createFilters(ActionURL url)
    {
        Pair<String,String>[] params = url.getParameters();
        ArrayList<FeaturesFilter> filters = new ArrayList<FeaturesFilter>(params.length);

        for(Pair<String,String> param : params)
        {
            if(param.getKey().startsWith(NAMESPACE_PREFIX))
            {
                FeaturesFilter filter = loadFilter(param, url);
                if(null != filter)
                    filters.add(filter);
            }
        }

        return filters;
    }

    private static FeaturesFilter loadFilter(Pair<String, String> param, ActionURL url)
    {
        //CONSIDER: generalize this more with a registry of known filters
        //and methods on the FeaturesFilter interface to load/persist from
        //URL parameters.
        //NOTE: some filters use multiple parameters

        String name = param.getKey().substring(NAMESPACE_PREFIX.length());
        if(name.equalsIgnoreCase(RunFilter.NAME))
        {
            return new RunFilter(param.getValue());
        }
        else if(name.equalsIgnoreCase(PeptideFilter.NAME))
        {
            return new PeptideFilter(param.getValue(),
                            url.getParameter(PeptideFilter.NAME_EXACT) != null);
        }
        else if(name.equalsIgnoreCase(ContainerFilter.NAME))
        {
            return new ContainerFilter(param.getValue());
        }

        //unknown filter
        return null;
    }

}
