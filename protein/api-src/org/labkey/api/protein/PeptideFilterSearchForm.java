package org.labkey.api.protein;

public class PeptideFilterSearchForm extends PeptideSearchForm
{
    @Override
    public PeptideSequenceFilter createFilter(String sequenceColumnName)
    {
        return new PeptideSequenceFilter(getPepSeq(), isExact(), sequenceColumnName, null);
    }
}
