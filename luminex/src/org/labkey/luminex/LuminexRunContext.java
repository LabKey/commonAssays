package org.labkey.luminex;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AssayRunUploadContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * General Luminex-specific run context info
 * User: jeckels
 * Date: Oct 7, 2011
 */
public interface LuminexRunContext extends AssayRunUploadContext
{
    public String[] getAnalyteNames();

    public Map<DomainProperty, String> getAnalyteProperties(String analyteName);

    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException;

    public List<Titration> getTitrations() throws ExperimentException;
}
