/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import org.labkey.ms2.MS2Run;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 30, 2007
 */
public enum MS2RunViewType
{
    QUERY_PEPTIDES("Standard", "query")
    {
        public AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryPeptideMS2RunView(viewContext, runs);
        }
    },
    QUERY_PROTEIN_GROUPS("Protein Groups", "queryproteingroups")
    {
        public AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryProteinGroupMS2RunView(viewContext, runs);
        }

        public boolean supportsRun(MS2Run run)
        {
            return run.hasProteinProphet();
        }

        public boolean supportsExtraFilters()
        {
            return false;
        }
    },
    NONE("Peptides (Legacy)", "none")
    {
        public AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs)
        {
            return new FlatPeptideView(viewContext, runs);
        }

        public boolean supportsExpansion()
        {
            return false;
        }

        public boolean supportsPeptideColumnPicker()
        {
            return true;
        }
    },
    PROTEIN("Protein (Legacy)", "protein")
    {
        public AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs)
        {
            return new StandardProteinPeptideView(viewContext, runs);
        }

        public boolean supportsPeptideColumnPicker()
        {
            return true;
        }

        public boolean supportsProteinColumnPicker()
        {
            return true;
        }
    },
    PROTEIN_PROPHET("ProteinProphet (Legacy)", "proteinprophet")
    {
        public AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs)
        {
            return new ProteinProphetPeptideView(viewContext, runs);
        }

        public boolean supportsRun(MS2Run run)
        {
            return run.hasProteinProphet();
        }

        public boolean supportsPeptideColumnPicker()
        {
            return true;
        }

        public boolean supportsProteinColumnPicker()
        {
            return true;
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

    public boolean supportsPeptideColumnPicker()
    {
        return false;
    }

    public boolean supportsProteinColumnPicker()
    {
        return false;
    }

    public boolean supportsExtraFilters()
    {
        return true;
    }

    public abstract AbstractMS2RunView<? extends WebPartView> createView(ViewContext viewContext, MS2Run... runs);

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
        return NONE;
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
