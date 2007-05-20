package org.labkey.ms2.peptideview;

import org.labkey.ms2.MS2Run;
import org.labkey.api.view.ViewContext;

import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Apr 30, 2007
 */
public enum MS2RunViewType
{
    NONE("None", "none")
    {
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
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
    PROTEIN("Protein", "protein")
    {
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
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
    PROTEIN_PROPHET("Protein Prophet", "proteinprophet")
    {
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
        {
            return new ProteinProphetPeptideView(viewContext, runs);
        }

        public boolean supportsRun(MS2Run run) throws SQLException
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
    },
    QUERY_PEPTIDES("Query - Peptides", "query")
    {
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryPeptideMS2RunView(viewContext, runs);
        }
    },
    QUERY_PROTEIN_GROUPS("Query - Protein Groups", "queryproteingroups")
    {
        public AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs)
        {
            return new QueryProteinGroupMS2RunView(viewContext, runs);
        }

        public boolean supportsRun(MS2Run run) throws SQLException
        {
            return run.hasProteinProphet();
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

    public boolean supportsRun(MS2Run run) throws SQLException
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

    public abstract AbstractMS2RunView createView(ViewContext viewContext, MS2Run... runs);

    public static MS2RunViewType getViewType(String urlName)
    {
        if (urlName == null)
        {
            return NONE;
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

    public static List<MS2RunViewType> getTypesForRun(MS2Run run) throws SQLException
    {
        List<MS2RunViewType> result = new ArrayList<MS2RunViewType>();
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
