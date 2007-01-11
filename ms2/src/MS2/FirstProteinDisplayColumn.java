package MS2;

import org.fhcrc.cpas.data.*;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: May 5, 2006
 */
public class FirstProteinDisplayColumn extends SimpleDisplayColumn
{
    private final FirstProteinType _type;
    private final ProteinGroupProteins _proteins;

    public FirstProteinDisplayColumn(String caption, FirstProteinType type, ProteinGroupProteins proteins)
    {
        setCaption(caption);
        _type = type;
        _proteins = proteins;
    }

    public Object getValue(RenderContext ctx)
    {
        Map row = ctx.getRow();
        Integer id = (Integer)row.get("RowId");
        if (id == null)
        {
            id = (Integer)row.get("ProteinGroupId");
        }
        try
        {
            List<ProteinSummary> summaries = _proteins.getSummaries(id.intValue());
            ProteinSummary firstSummary = summaries.get(0);
            return _type.getValue(firstSummary);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    public enum FirstProteinType
    {
        NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getName();
            }
        },
        BEST_NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getBestName();
            }
        },
        DESCRIPTION
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getDescription();
            }
        },
        BEST_GENE_NAME
        {
            public String getValue(ProteinSummary proteinSummary)
            {
                return proteinSummary.getBestGeneName();
            }
        };

        public abstract String getValue(ProteinSummary proteinSummary);
    }

}
