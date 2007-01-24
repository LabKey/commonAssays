package org.labkey.flow.data;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.OntologyManager;

public enum InputRole
{
    CompensationMatrix,
    AnalysisScript,
    FCSFile,
    Sample,
    ;
    public PropertyDescriptor getPropertyDescriptor(Container container)
    {
        return OntologyManager.getPropertyDescriptor(ExperimentService.get().getDataInputRolePropertyURI(container, name()), container);
    }
}
