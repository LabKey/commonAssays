package org.labkey.flow.analysis.model;

import org.jetbrains.annotations.Nullable;
import org.labkey.flow.persist.AttributeSet;

import java.util.Map;

/**
 * User: kevink
 * Date: 11/16/12
 */
public interface ISampleInfo
{
    /**
     * Internal sample id used by the workspace or analysis archive.
     * Not a stable identifier.
     * @return
     */
    public String getSampleId();

    /**
     * The sample name (usually the same as the $FIL keyword, but may be renamed in the workspace.)
     * @return
     */
    public String getSampleName();

    /**
     * The $FIL keyword value.
     * @return
     */
    public String getFilename();

    /**
     * Get human readable display name (may be sample name, $FIL, or sample id).
     * @return
     */
    public String getLabel();

    /**
     * A case-insensitive map of keyword names to values.
     * @return
     */
    public Map<String, String> getKeywords();

    /**
     * The analysis definition which may be null for an analysis archive.
     * @return
     */
    @Nullable
    public Analysis getAnalysis();

    /**
     * The calculated statistics and graphs.
     * @return
     */
    public AttributeSet getAnalysisResults();

    /**
     * The compensation matrix used for analysis.
     * @return
     */
    public CompensationMatrix getCompensationMatrix();
}
