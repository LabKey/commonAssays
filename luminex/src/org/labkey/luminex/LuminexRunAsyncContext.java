/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.pipeline.AssayRunAsyncContext;
import org.apache.log4j.Logger;
import org.labkey.luminex.model.SinglePointControl;
import org.labkey.luminex.model.Titration;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles run import in the context of a pipeline job (assay design set to "import in background")
 * User: jeckels
 * Date: Feb 13, 2012
 */
public class LuminexRunAsyncContext extends AssayRunAsyncContext<LuminexAssayProvider> implements LuminexRunContext
{
    private String[] _analyteNames;
    private Map<String, Map<Integer, String>> _analytePropertiesById = new HashMap<>();
    private Map<String, Map<String, String>> _analyteColumnPropertiesByName = new HashMap<>();
    private Map<String, Map<String, String>> _analytePropertiesByName = new HashMap<>();
    private Map<String, Set<String>> _titrationsByAnalyte = new HashMap<>();
    private List<Titration> _titrations;
    private List<SinglePointControl> _singlePointControls;
    private Boolean _retainExclusions;

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
            _analytePropertiesByName.put(analyteName, convertDomainsToNames(originalContext.getAnalyteProperties(analyteName)));
            _titrationsByAnalyte.put(analyteName, originalContext.getTitrationsForAnalyte(analyteName));
        }
        _titrations = originalContext.getTitrations();
        _singlePointControls = originalContext.getSinglePointControls();
        _retainExclusions = originalContext.getRetainExclusions();
    }

    @Override
    public void logProperties(Logger logger)
    {
        super.logProperties(logger);
        String valueText;
        logger.info("----- Start Analyte Properties -----");
        for(Map.Entry<String, Map<String, String>> entry : _analyteColumnPropertiesByName.entrySet())
        {
            logger.info("\tProperties for " + entry.getKey() + ":");
            for(Map.Entry<String, String> props : entry.getValue().entrySet())
            {
                if(props.getValue() == null)
                    valueText = "[Blank]";
                else
                    valueText = props.getValue();

                logger.info("\t\t*"+props.getKey() + ":  " + valueText);
            }

            //Currenlty this should only have the positivity thresholds in it, but it might have more later.
            for(Map.Entry<String, String> props: _analytePropertiesByName.get(entry.getKey()).entrySet())
            {
                if(props.getValue() == null)
                    valueText = "[Blank]";
                else
                    valueText = props.getValue();

                logger.info("\t\t*"+props.getKey() + ":  " + valueText);
            }

            Object[] titrationByAnalyteArray = _titrationsByAnalyte.get(entry.getKey()).toArray();
            if(titrationByAnalyteArray.length > 0)
            {
                logger.info("\t\tUses Titrations:");
                for (Object aTitrationByAnalyteArray : titrationByAnalyteArray)
                {
                    logger.info("\t\t\t*" + aTitrationByAnalyteArray);
                }
            }
        }
        logger.info("----- Stop Analyte Properties -----");
        logger.info("----- Start Well Role Properties -----");
        for(Titration titration : _titrations)
        {
            logger.info("\tProperties for " + titration.getName());
            logger.info("\t\t*Standard:  " + titration.isStandard());
            logger.info("\t\t*QC Control:  " + titration.isQcControl());
            logger.info("\t\t*Unknown:  " + titration.isUnknown());
            logger.info("\t\t*Other Control:  " + titration.isOtherControl());
        }
        for (SinglePointControl singlePointControl : _singlePointControls)
        {
            logger.info("\tProperties for " + singlePointControl.getName());
            logger.info("\t\t*Single Point Control: true");
        }
        logger.info("----- Stop Well Role Properties -----");

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

    @Override
    public boolean getRetainExclusions()
    {
        return _retainExclusions;
    }

    /** Convert to a map that can be serialized - ColumnInfo can't be */
    private Map<String, String> convertDomainsToNames(Map<DomainProperty, String> properties)
    {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<DomainProperty, String> entry : properties.entrySet())
        {
            result.put(entry.getKey().getName(), entry.getValue());
        }
        return result;
    }

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
            if (column.getName().equals(columnName))
                return column;
        }
        throw new IllegalStateException("Could not find property: " + columnName);
    }

    @Override
    public List<SinglePointControl> getSinglePointControls() throws ExperimentException
    {
        return _singlePointControls;
    }


    public static class TestCase extends Assert
    {

        LuminexUnitTestContext testContext = new LuminexUnitTestContext();
        LuminexRunAsyncContext asyncContext;

        private static class StringLogger extends Logger
        {
            StringBuilder sb = new StringBuilder();
            private StringLogger()
            {
                super("");
            }

            @Override
            public void info(Object append)
            {
                sb.append(append);
                sb.append("\n");
            }

            public String toString()
            {
                return sb.toString();
            }
        }

        @Test
        public void checkLogging() throws Exception
        {
            asyncContext = new LuminexRunAsyncContext(testContext);
            StringLogger sl = new StringLogger();
            asyncContext.logProperties(sl);
            String output = sl.toString();

            assert(output.contains("Analyte 1"));
            assert(output.contains("Analyte 2"));
            assert(output.contains("Batch Name: Name of the Batch"));
            assert(output.contains("* New"));
            assert(output.contains("Assay ID: Log Test"));
            assert(output.contains("Run Comments: Test Comments"));
            assert(output.contains("Run Name: Name of the Run"));
            assert(output.contains("Name:  Name of the Project"));
            assert(output.contains("*PositivityThreshold:  50.0"));
            assert(output.contains("*NegativeBead:  Blank (3)"));
            assert(output.contains("Standard:  true"));
            assert(output.contains("QC Control:  false"));
            assert(output.contains("Other Control:  true"));
            assert(output.contains("Unknown:  false"));
            assert(output.contains("Single Point Control: true"));
        }

    }
}
