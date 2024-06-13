package org.labkey.api.protein.search;

import org.labkey.api.util.SafeToRenderEnum;

public enum PeptideFilteringFormElements implements SafeToRenderEnum
{
    peptideFilterType,
    peptideProphetProbability,
    proteinGroupFilterType,
    proteinProphetProbability,
    orCriteriaForEachRun,
    runList,
    spectraConfig,
    pivotType,
    targetProtein,
    targetSeqIds,
    targetProteinMsg,
    targetURL
}
