/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2.peptideview;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Run;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Apr 30, 2007
 */
public enum MS2RunViewType
{
    QUERY_PEPTIDES("Standard", "query")
    {
        @Override  @NotNull
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryPeptideMS2RunView(viewContext, runs);
        }
    },
    QUERY_PROTEIN_GROUPS("FastaProtein Groups", "queryproteingroups")
    {
        @Override @NotNull
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryProteinGroupMS2RunView(viewContext, runs);
        }

        @Override
        public boolean supportsRun(MS2Run run)
        {
            return run.hasProteinProphet();
        }

        @Override
        public boolean supportsExtraFilters()
        {
            return false;
        }
    };

    private final String _name;
    private final String _urlName;

    private MS2RunViewType(String name, String urlName)
    {
        _name = name;
        _urlName = urlName;
    }

    public String getName()
    {
        return _name;
    }


    public String getURLName()
    {
        return _urlName;
    }

    public boolean supportsRun(MS2Run run)
    {
        return true;
    }

    public boolean supportsExpansion()
    {
        return true;
    }

    public boolean supportsExtraFilters()
    {
        return true;
    }

    @NotNull
    public abstract AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs);

    public static MS2RunViewType getViewType(String urlName)
    {
        if (urlName == null)
        {
            return QUERY_PEPTIDES;
        }
        for (MS2RunViewType type : values())
        {
            if (urlName.equalsIgnoreCase(type._urlName))
            {
                return type;
            }
        }
        return QUERY_PEPTIDES;
    }

    public static List<MS2RunViewType> getTypesForRun(MS2Run run)
    {
        List<MS2RunViewType> result = new ArrayList<>();
        for (MS2RunViewType type : values())
        {
            if (type.supportsRun(run))
            {
                result.add(type);
            }
        }
        return result;
    }
}
