package org.labkey.api.protein.search;

import org.apache.commons.lang3.ArrayUtils;
import org.labkey.api.action.HasViewContext;
import org.labkey.api.annotations.Migrate;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.MatchCriteria;
import org.labkey.api.protein.query.ProteinUserSchema;
import org.labkey.api.protein.query.SequencesTableInfo;
import org.labkey.api.query.QueryParam;
import org.labkey.api.view.ViewContext;

public class ProbabilityProteinSearchForm extends ProteinSearchForm implements HasViewContext
{
    private Float _minimumProbability;
    private Float _maximumErrorRate;
    private ViewContext _context;
    private int[] _seqId;
    private SQLFragment _restrictCondition = new SQLFragment();

    @Override
    public void setViewContext(ViewContext context)
    {
        _context = context;
    }

    @Override
    public ViewContext getViewContext()
    {
        return _context;
    }

    public boolean isPeptideProphetFilter()
    {
        return ProphetFilterType.probability.toString().equals(getPeptideFilterType());
    }

    public boolean isCustomViewPeptideFilter()
    {
        return ProphetFilterType.customView.toString().equals(getPeptideFilterType());
    }

    public Float getMaximumErrorRate()
    {
        return _maximumErrorRate;
    }

    public void setMaximumErrorRate(Float maximumErrorRate)
    {
        _maximumErrorRate = maximumErrorRate;
    }

    public Float getMinimumProbability()
    {
        return _minimumProbability;
    }

    public void setMinimumProbability(Float minimumProbability)
    {
        _minimumProbability = minimumProbability;
    }

    public String getCustomViewName(ViewContext context)
    {
        String result = context.getRequest().getParameter("PeptidesFilter." + QueryParam.viewName); // Needs to match MS2Controller.PEPTIDES_FILTER_VIEW_NAME
        if (result == null)
        {
            result = _defaultCustomView;
        }
        if ("".equals(result))
        {
            return null;
        }
        return result;
    }

    public boolean isNoPeptideFilter()
    {
        return !isCustomViewPeptideFilter() && !isPeptideProphetFilter();
    }

    public static ProbabilityProteinSearchForm createDefault()
    {
        ProbabilityProteinSearchForm result = new ProbabilityProteinSearchForm();
        result.setIncludeSubfolders(true);
        result.setRestrictProteins(true);
        result.setExactMatch(true);
        return result;
    }

    public void setSeqId(int[] seqIds)
    {
        _seqId = seqIds;
    }

    @Override
    public int[] getSeqId()
    {
        if (_seqId == null)
        {
            ProteinUserSchema schema = new ProteinUserSchema(_context.getUser(), _context.getContainer());
            SequencesTableInfo<ProteinUserSchema> tableInfo = schema.createSequences();
            tableInfo.addProteinNameFilter(getIdentifier(), isExactMatch() ? MatchCriteria.EXACT : MatchCriteria.PREFIX);
            if (isRestrictProteins())
            {
                tableInfo.addCondition(_restrictCondition);
            }
            _seqId = ArrayUtils.toPrimitive(new TableSelector(tableInfo.getColumn("SeqId")).getArray(Integer.class));
        }
        return _seqId;
    }

    public void setRestrictCondition(SQLFragment restrictCondition)
    {
        _restrictCondition = restrictCondition;
    }
}
