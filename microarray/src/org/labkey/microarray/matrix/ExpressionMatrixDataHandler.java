/*
 * Copyright (c) 2013-2014 LabKey Corporation
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
package org.labkey.microarray.matrix;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.AbstractExperimentDataHandler;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.microarray.MicroarrayManager;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExpressionMatrixDataHandler extends AbstractExperimentDataHandler
{
    private static final String PROBE_ID_COLUMN_NAME = "ID_REF";

    private static final Logger LOG = Logger.getLogger(ExpressionMatrixDataHandler.class);

    @Override
    public DataType getDataType()
    {
        return ExpressionMatrixAssayProvider.DATA_TYPE;
    }
    
    @Override
    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        if (!dataFile.exists())
        {
            log.warn("Could not find file " + dataFile.getAbsolutePath() + " on disk for data with LSID " + data.getLSID());
            return;
        }
        ExpRun expRun = data.getRun();
        if (expRun == null)
        {
            throw new ExperimentException("Could not load ExpressionMatrix file " + dataFile.getAbsolutePath() + " because it is not owned by an experiment run");
        }

        try
        {
            ExpProtocol protocol = expRun.getProtocol();
            AssayProvider provider = AssayService.get().getProvider(expRun);
            if (provider == null)
            {
                throw new ExperimentException("Could not find assay provider for protocol with LSID " + protocol.getLSID());
            }

            Domain runDomain = provider.getRunDomain(protocol);
            if (runDomain == null)
            {
                throw new ExperimentException("Could not find run domain for protocol with LSID " + protocol.getLSID());
            }

            Map<String, String> runProps = getRunPropertyValues(expRun, runDomain);

            try (TabLoader loader = createTabLoader(dataFile))
            {
                ColumnDescriptor[] cols = loader.getColumns();
                List<String> columnNames = new ArrayList<>(cols.length);
                for (ColumnDescriptor col : cols)
                    columnNames.add(col.getColumnName());

                Map<String, Integer> samplesMap = ensureSamples(info.getContainer(), columnNames);

                boolean importValues = true;
                if (runProps.containsKey(ExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName()))
                {
                    String importValuesStr = runProps.get(ExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName());
                    if (importValuesStr != null)
                        importValues = Boolean.valueOf(importValuesStr);
                }

                if (importValues)
                {
                    insertExpressionMatrixData(info.getContainer(), samplesMap, loader, runProps, data.getRowId());
                }
            }
        }
        catch (IOException e)
        {
            throw new ExperimentException("Failed to read from data file " + dataFile.getName(), e);
        }
        catch (ExperimentException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ExperimentException(e);
        }
    }

    protected static TabLoader createTabLoader(File file) throws IOException
    {
        TabLoader loader = new TabLoader(file, true);

        // If the 0th column is missing a name, consider it the ID_REF column
        ColumnDescriptor[] cols = loader.getColumns();
        if (cols[0].name.equals("column0"))
            cols[0].name = PROBE_ID_COLUMN_NAME;

        return loader;
    }

    protected static Map<String, Integer> ensureSamples(Container container, Collection<String> columnNames) throws ExperimentException
    {
        Set<String> sampleNames = new HashSet<>(columnNames.size());
        for (String name : columnNames)
        {
            if (!name.equals(PROBE_ID_COLUMN_NAME))
            {
                sampleNames.add(name);
            }
        }

        SimpleFilter sampleSetFilter = new SimpleFilter();
        sampleSetFilter.addInClause(FieldKey.fromParts("Name"), sampleNames);
        sampleSetFilter.addCondition(FieldKey.fromParts("Container"), container);

        Set<String> selectNames = new LinkedHashSet<>();
        selectNames.add("Name");
        selectNames.add("RowId");
        TableSelector sampleTableSelector = new TableSelector(ExperimentService.get().getTinfoMaterial(), selectNames, sampleSetFilter, null);
        Map<String, Object>[] sampleSetResults = sampleTableSelector.getMapArray();
        if (sampleSetResults.length < 1)
            throw new ExperimentException("No matching samples found");

        // CONSIDER: Create missing samples automatically ?
        Map<String, Integer> sampleMap = sampleTableSelector.getValueMap();
        if (sampleMap.size() < sampleNames.size())
        {
            Set<String> missingSamples = new HashSet<>(sampleNames);
            missingSamples.removeAll(sampleMap.keySet());
            throw new ExperimentException("No samples found for: " + StringUtils.join(missingSamples, ", "));
        }

        return sampleMap;
    }

    private void insertExpressionMatrixData(Container c,
                                            Map<String, Integer> samplesMap, DataLoader loader,
                                            Map<String, String> runProps, Integer dataRowId) throws ExperimentException
    {
        assert MicroarrayUserSchema.getSchema().getScope().isTransactionActive() : "Should be invoked in the context of an existing transaction";
        PreparedStatement statement = null;
        try
        {
            Connection connection = MicroarrayUserSchema.getSchema().getScope().getConnection();
            statement = connection.prepareStatement("INSERT INTO microarray." +
                    ExpressionMatrixProtocolSchema.FEATURE_DATA_TABLE_NAME + " (DataId, SampleId, FeatureId, \"Value\") " +
                    "VALUES (?, ?, ?, ?)");
            int rowCount = 0;

            // Grab the probe name to rowId mapping for this run's annotation set
            int featureSet = Integer.parseInt(runProps.get("featureSet"));
            Map<String, Integer> probeIds = MicroarrayManager.get().getFeatureAnnotationSetProbeIds(c, featureSet);

            for (Map<String, Object> row : loader)
            {
                Object probeObject = row.get(PROBE_ID_COLUMN_NAME);
                String probeName = probeObject == null ? null : probeObject.toString();

                if (probeName == null || StringUtils.trimToNull(probeName) == null)
                {
                    throw new ExperimentException("Probe ID (ID_REF) must be present and cannot be blank");
                }

                Integer featureId = probeIds.get(probeName);
                if (featureId == null)
                {
                    throw new ExperimentException("Unable to find a feature/probe with name '" + probeName + "'");
                }

                // All of the column headers are sample names except for the probe id column.
                for (String sampleName : row.keySet())
                {
                    if (sampleName.equals(PROBE_ID_COLUMN_NAME) || row.get(sampleName) == null)
                        continue;

                    statement.setInt(1, dataRowId);
                    statement.setInt(2, samplesMap.get(sampleName));
                    statement.setInt(3, featureId);
                    statement.setDouble(4, ((Number) row.get(sampleName)).doubleValue());
                    statement.executeUpdate();
                }

                if (++rowCount % 5000 == 0)
                {
                    LOG.info("Imported " + rowCount + " rows...");
                }
            }
            LOG.info("Imported " + rowCount + " rows.");
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (statement != null) { try { statement.close(); } catch (SQLException ignored) {} }
        }
    }

    private Map<String, String> getRunPropertyValues(ExpRun run, Domain domain)
    {
        Map<String, String> runPropValues = new HashMap<>();
        for (DomainProperty runProp : domain.getProperties())
        {
            Object value = run.getProperty(runProp);
            if (value != null)
                runPropValues.put(runProp.getName(), value.toString());
        }
        return runPropValues;
    }

    @Override
    public ActionURL getContentURL(Container container, ExpData data)
    {
        return null;
    }

    @Override
    public void deleteData(ExpData data, Container container, User user)
    {
        SqlExecutor executor = new SqlExecutor(MicroarrayUserSchema.getSchema());
        SQLFragment deleteDataSql = new SQLFragment("DELETE FROM " + MicroarrayUserSchema.SCHEMA_NAME);
        deleteDataSql.append("." + ExpressionMatrixProtocolSchema.FEATURE_DATA_TABLE_NAME);
        deleteDataSql.append(" WHERE DataId = ?").add(data.getRowId());
        executor.execute(deleteDataSql);
    }

    @Override
    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException();
    }
}
