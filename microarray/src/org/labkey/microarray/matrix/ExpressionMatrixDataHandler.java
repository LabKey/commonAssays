/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.reader.ColumnDescriptor;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.matrix.AbstractMatrixDataHandler;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.microarray.MicroarrayManager;
import org.labkey.microarray.query.MicroarrayUserSchema;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpressionMatrixDataHandler extends AbstractMatrixDataHandler
{
    public static final String FEATURE_ID_COLUMN_NAME = "ID_REF";

    private static final Logger LOG = Logger.getLogger(ExpressionMatrixDataHandler.class);

    // CONSIDER: move this flag to the assay design
    private static boolean autoCreateSamples = true;

    public ExpressionMatrixDataHandler()
    {
        super(FEATURE_ID_COLUMN_NAME, MicroarrayUserSchema.SCHEMA_NAME, ExpressionMatrixProtocolSchema.FEATURE_DATA_TABLE_NAME);
    }

    @Override
    public DbSchema getDbSchema()
    {
        return MicroarrayUserSchema.getSchema();
    }

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

            try (TabLoader loader = createTabLoader(dataFile, FEATURE_ID_COLUMN_NAME))
            {
                ColumnDescriptor[] cols = loader.getColumns();
                List<String> columnNames = new ArrayList<>(cols.length);
                for (ColumnDescriptor col : cols)
                    columnNames.add(col.getColumnName());

                Map<String, Integer> samplesMap = ensureSamples(info.getContainer(), info.getUser(), columnNames, FEATURE_ID_COLUMN_NAME);

                boolean importValues = true;
                if (runProps.containsKey(ExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName()))
                {
                    String importValuesStr = runProps.get(ExpressionMatrixAssayProvider.IMPORT_VALUES_COLUMN.getName());
                    if (importValuesStr != null)
                        importValues = Boolean.valueOf(importValuesStr);
                }

                if (importValues)
                    insertMatrixData(info.getContainer(), info.getUser(), samplesMap, loader, runProps, data.getRowId());
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

    @Override
    public void insertMatrixData(Container c, User user,
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
            String featureSetString = runProps.get(ExpressionMatrixAssayProvider.FEATURE_SET_PROPERTY_NAME);
            if (featureSetString == null)
            {
                throw new ExperimentException("Could not find " + ExpressionMatrixAssayProvider.FEATURE_SET_PROPERTY_NAME + " property value");
            }

            int featureSet;
            try
            {
                featureSet = Integer.parseInt(featureSetString);
            }
            catch (NumberFormatException e)
            {
                throw new ExperimentException("Illegal " + ExpressionMatrixAssayProvider.FEATURE_SET_PROPERTY_NAME + " value:" + featureSetString);
            }
            Map<String, Integer> featureIds = MicroarrayManager.get().getFeatureAnnotationSetFeatureIds(featureSet);

            for (Map<String, Object> row : loader)
            {
                Object featureObject = row.get(FEATURE_ID_COLUMN_NAME);
                String featureName = featureObject == null ? null : featureObject.toString();

                if (featureName == null || StringUtils.trimToNull(featureName) == null)
                {
                    throw new ExperimentException("Feature ID (ID_REF) must be present and cannot be blank");
                }

                Integer featureId = featureIds.get(featureName);
                if (featureId == null)
                {
                    throw new ExperimentException("Unable to find a feature/probe with name '" + featureName + "'");
                }

                // All of the column headers are sample names except for the probe id column.
                for (String sampleName : row.keySet())
                {
                    if (sampleName.equals(FEATURE_ID_COLUMN_NAME) || row.get(sampleName) == null)
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


}
