package org.labkey.flow.analysis.web;

import java.io.Serializable;

public interface SpecBase<Z extends SpecBase> extends Serializable, Comparable<Z>
{
    SubsetSpec getSubset();

    String[] getParameters();
}
