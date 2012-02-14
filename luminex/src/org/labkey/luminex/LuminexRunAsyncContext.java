package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.pipeline.AssayRunAsyncContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: jeckels
 * Date: Feb 13, 2012
 */
public class LuminexRunAsyncContext extends AssayRunAsyncContext<LuminexAssayProvider> implements LuminexRunContext
{
    private String[] _analyteNames;
    private Map<String, Map<Integer, String>> _analytePropertiesById = new HashMap<String, Map<Integer, String>>();
    private Map<String, Set<String>> _titrationsByAnalyte = new HashMap<String, Set<String>>();
    private Map<String, Map<ColumnInfo, String>> _analyteColumnProperties = new HashMap<String, Map<ColumnInfo, String>>();
    private List<Titration> _titrations;

    private transient Map<String, Map<DomainProperty, String>> _analyteProperties;
    private transient LuminexExcelParser _parser;

    public LuminexRunAsyncContext(LuminexRunContext originalContext) throws IOException, ExperimentException
    {
        super(originalContext);

        _analyteNames = originalContext.getAnalyteNames();

        for (String analyteName : _analyteNames)
        {
            _analytePropertiesById.put(analyteName, convertPropertiesToIds(originalContext.getAnalyteProperties(analyteName)));
            _titrationsByAnalyte.put(analyteName, originalContext.getTitrationsForAnalyte(analyteName));
            _analyteColumnProperties.put(analyteName, originalContext.getAnalyteColumnProperties(analyteName));
        }
        _titrations = originalContext.getTitrations();
    }

    @Override
    public String[] getAnalyteNames()
    {
        return _analyteNames;
    }

    @Override
    public Map<DomainProperty, String> getAnalyteProperties(String analyteName)
    {
        if (_analyteProperties == null)
        {
            _analyteProperties = new HashMap<String, Map<DomainProperty, String>>();
        }
        Map<DomainProperty, String> result = _analyteProperties.get(analyteName);
        if (result == null)
        {
            Map<Integer, String> propsById = _analytePropertiesById.get(analyteName);
            if (propsById == null)
            {
                throw new IllegalStateException("Could not find analyte: " + analyteName);
            }
            result = convertPropertiesFromIds(propsById);
            _analyteProperties.put(analyteName, result);
        }
        return result;
    }

    @Override
    public Map<ColumnInfo, String> getAnalyteColumnProperties(String analyteName)
    {
        Map<ColumnInfo, String> result = _analyteColumnProperties.get(analyteName);
        if (result == null)
        {
            throw new IllegalStateException("Could not find analyte: " + analyteName);
        }
        return result;
    }

    @Override
    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException
    {
        Set<String> result = _titrationsByAnalyte.get(analyteName);
        if (result == null)
        {
            throw new IllegalStateException("Could not find analyte: " + analyteName);
        }
        return result;
    }

    @Override
    public List<Titration> getTitrations() throws ExperimentException
    {
        return _titrations;
    }

    @Override
    public LuminexExcelParser getParser() throws ExperimentException
    {
        if (_parser == null)
        {
            _parser = new LuminexExcelParser(getProtocol(), getUploadedData().values());
        }
        return _parser;
    }
}
