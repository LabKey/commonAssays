package org.labkey.elisa.actions;

import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.actions.PlateUploadFormImpl;
import org.labkey.api.study.assay.PlateSamplePropertyHelper;
import org.labkey.elisa.ElisaAssayProvider;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/9/12
 */
public class ElisaRunUploadForm extends PlateUploadFormImpl<ElisaAssayProvider>
{
    private Map<String, Map<DomainProperty, String>> _sampleProperties;
    private PlateSamplePropertyHelper _samplePropertyHelper;

    public PlateSamplePropertyHelper getSamplePropertyHelper()
    {
        return _samplePropertyHelper;
    }

    public void setSamplePropertyHelper(PlateSamplePropertyHelper helper)
    {
        _samplePropertyHelper = helper;
    }

    public Map<String, Map<DomainProperty, String>> getSampleProperties()
    {
        return _sampleProperties;
    }

    public void setSampleProperties(Map<String, Map<DomainProperty, String>> sampleProperties)
    {
        _sampleProperties = sampleProperties;
    }
}
