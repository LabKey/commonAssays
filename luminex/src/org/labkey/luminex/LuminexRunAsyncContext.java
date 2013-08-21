/*
 * Copyright (c) 2012-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.pipeline.AssayRunAsyncContext;
import org.apache.log4j.Logger;

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
    private Map<String, Map<Integer, String>> _analytePropertiesById = new HashMap<>();
    private Map<String, Map<String, String>> _analyteColumnPropertiesByName = new HashMap<>();
    private Map<String, Set<String>> _titrationsByAnalyte = new HashMap<>();
    private List<Titration> _titrations;
    private List<SinglePointControl> _singlePointControls;

    private transient Map<String, Map<DomainProperty, String>> _analyteProperties;
    private transient Map<String, Map<ColumnInfo, String>> _analyteColumnProperties;
    private transient LuminexExcelParser _parser;

    public LuminexRunAsyncContext(LuminexRunContext originalContext) throws IOException, ExperimentException
    {
        super(originalContext);

        _analyteNames = originalContext.getAnalyteNames();

        for (String analyteName : _analyteNames)
        {
            _analytePropertiesById.put(analyteName, convertPropertiesToIds(originalContext.getAnalyteProperties(analyteName)));
            _analyteColumnPropertiesByName.put(analyteName, convertColumnPropertiesToNames(originalContext.getAnalyteColumnProperties(analyteName)));
            _titrationsByAnalyte.put(analyteName, originalContext.getTitrationsForAnalyte(analyteName));
        }
        _titrations = originalContext.getTitrations();
        _singlePointControls = originalContext.getSinglePointControls();
    }

    @Override
    public void logProperties(Logger logger)
    {
        super.logProperties(logger);
        logger.info("----- Start Luminex Analyte Properties -----");
        for(Map.Entry<String, Map<String, String>> entry : this._analyteColumnPropertiesByName.entrySet())
        {
            logger.info("\tProperties for " + entry.getKey() + ":");
            for(Map.Entry<String, String> props : entry.getValue().entrySet())
            {
                logger.info("\t\t*"+props.getKey() + ":  " + props.getValue());
            }
        }
        logger.info("----- Stop Luminex Analyte Properties -----");
        logger.info("----- Start Luminex Titration Properties -----");
        for(Titration titration : this._titrations)
        {
            logger.info("\tProperties for " + titration.getName());
            logger.info("\t\t*Row ID:  " + titration.getRowId());
            logger.info("\t\t*Run ID:  " + titration.getRunId());
            logger.info("\t\t*Standard:  " + titration.isStandard());
            logger.info("\t\t*QC Control:  " + titration.isQcControl());
            logger.info("\t\t*Unknown:  " + titration.isUnknown());
            logger.info("\t\t*Max FI:  " + titration.getMaxFI());
        }
        logger.info("----- Stop Luminex Titration Properties -----");

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
            _analyteProperties = new HashMap<>();
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
        if (_analyteColumnProperties == null)
        {
            _analyteColumnProperties = new HashMap<>();
        }
        Map<ColumnInfo, String> result = _analyteColumnProperties.get(analyteName);
        if (result == null)
        {
            Map<String, String> propsByName = _analyteColumnPropertiesByName.get(analyteName);
            if (propsByName == null)
            {
                throw new IllegalStateException("Could not find analyte: " + analyteName);
            }
            result = convertColumnPropertiesFromNames(propsByName);
            _analyteColumnProperties.put(analyteName, result);
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

    /** Convert to a map that can be serialized - ColumnInfo can't be */
    private Map<String, String> convertColumnPropertiesToNames(Map<ColumnInfo, String> properties)
    {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<ColumnInfo, String> entry : properties.entrySet())
        {
            result.put(entry.getKey().getName(), entry.getValue());
        }
        return result;
    }

    /** Convert from a serialized map by looking up the ColumnInfo from the Analyte table */
    private Map<ColumnInfo, String> convertColumnPropertiesFromNames(Map<String, String> properties)
    {
        Map<ColumnInfo, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet())
        {
            result.put(findColumn(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private ColumnInfo findColumn(String columnName)
    {
        List<ColumnInfo> columns = LuminexProtocolSchema.getTableInfoAnalytes().getColumns();
        for (ColumnInfo column : columns)
        {
            if (column.getName().equals(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME))
            {
                return column;
            }
        }
        throw new IllegalStateException("Could not find property: " + columnName);
    }

    @Override
    public List<SinglePointControl> getSinglePointControls() throws ExperimentException
    {
        return _singlePointControls;
    }
}
