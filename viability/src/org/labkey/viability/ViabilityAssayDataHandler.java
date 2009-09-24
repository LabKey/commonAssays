/*
 * Copyright (c) 2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.study.assay.AbstractAssayTsvDataHandler;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.api.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.query.ValidationException;
import org.labkey.api.util.Pair;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * User: kevink
 * Date: Sep 16, 2009
 */
public abstract class ViabilityAssayDataHandler extends AbstractAssayTsvDataHandler
{
    public static abstract class Parser
    {
        protected Domain _runDomain;
        protected Domain _resultsDomain;
        protected File _dataFile;

        protected Map<String, Object> _runData;
        protected List<Map<String, Object>> _resultData;

        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            _runDomain = runDomain;
            _resultsDomain = resultsDomain;
            _dataFile = dataFile;
        }

        public abstract DataType getDataType();
        
        protected void parse() throws ExperimentException
        {
            try
            {
                _parse();
            }
            catch (IOException e)
            {
                throw new ExperimentException(e);
            }
            finally
            {
                if (_runData == null)
                    _runData = Collections.emptyMap();
                if (_resultData == null)
                    _resultData = Collections.emptyList();
            }
        }

        protected abstract void _parse() throws IOException, ExperimentException;

        public Map<String, Object> getRunData() throws ExperimentException
        {
            if (_runData == null)
                parse();
            return _runData;
        }

        public List<Map<String, Object>> getResultData() throws ExperimentException
        {
            if (_resultData == null)
                parse();
            return _resultData;
        }

    }

    protected abstract Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile);

    protected boolean allowEmptyData()
    {
        return true;
    }

    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        return null;
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        // results should have been deleted in beforeDeleteData()
        super.deleteData(data, container, user);
    }

    @Override
    public void beforeDeleteData(List<ExpData> datas) throws ExperimentException
    {
        Container c = datas.get(0).getContainer();
        ViabilityManager.deleteAll(datas, c);
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpProtocol protocol = data.getRun().getProtocol();
        AssayProvider provider = AssayService.get().getProvider(protocol);

        Domain runDomain = provider.getRunDomain(protocol);
        Domain resultsDomain = provider.getResultsDomain(protocol);
        Parser parser = getParser(runDomain, resultsDomain, dataFile);
        List<Map<String, Object>> rows = parser.getResultData();
        validateData(rows, false);

        Map<DataType, List<Map<String, Object>>> result = new HashMap<DataType, List<Map<String, Object>>>();
        result.put(parser.getDataType(), rows);
        return result;
    }

    // check file data: all rows must have PoolID
    public static void validateData(List<Map<String, Object>> rows, boolean requireSpecimens) throws ExperimentException
    {
        if (rows == null || rows.size() == 0)
            throw new ExperimentException("No rows found.");

        for (ListIterator<Map<String, Object>> it = rows.listIterator(); it.hasNext();)
        {
            Map<String, Object> row = it.next();
            String poolID = (String) row.get(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME);
            if (poolID == null || poolID.length() == 0)
                throw new ExperimentException(ViabilityAssayProvider.POOL_ID_PROPERTY_NAME + " required");

            String participantID = (String) row.get(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
            String visitID = (String) row.get(AbstractAssayProvider.VISITID_PROPERTY_NAME);
            if (participantID == null && visitID == null)
            {
                String[] parts = poolID.split("-|_", 2);
                if (parts.length == 2)
                {
                    row = new HashMap<String, Object>(row);
                    row.put(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME, parts[0]);
                    row.put(AbstractAssayProvider.VISITID_PROPERTY_NAME, parts[1]);
                    it.set(row);
                }
            }

            if (requireSpecimens)
            {
                Object obj = row.get(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME);
                if (!(obj instanceof String[]))
                    throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + " required");
                String[] specimenIDs = (String[]) obj;
                if (specimenIDs.length == 0)
                    throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + " required");
                for (int i = 0; i < specimenIDs.length; i++)
                {
                    String specimenID = specimenIDs[i];
                    if (specimenID == null || specimenID.length() == 0)
                        throw new ExperimentException(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME + "[" + i + "] is empty or null.");
                }
            }
        }
    }

    @Override
    protected List<Map<String, Object>> convertPropertyNamesToURIs(List<Map<String, Object>> dataMaps, Map<String, DomainProperty> propertyNamesToUris)
    {
        // XXX: pass data thru untouched for now.
        return dataMaps;
    }

    @Override
    protected void insertRowData(ExpData data, User user, Container container, PropertyDescriptor[] dataProperties, List<Map<String, Object>> fileData) throws SQLException, ValidationException
    {
        // Insert is a no-op.  We insert later during the upload wizard SpecimensStepHandler.handleSuccessfulPost()
        //_insertRowData(data, user, container, dataProperties, fileData);
    }

    /*package*/ static void _insertRowData(ExpData data, User user, Container container, PropertyDescriptor[] dataProperties, List<Map<String, Object>> fileData) throws SQLException, ValidationException
    {
        for (Map<String, Object> row : fileData)
        {
            Pair<Map<String, Object>, Map<String, Object>> pair = splitBaseFromExtra(row);

            ViabilityResult result = ViabilityResult.fromMap(pair.first, pair.second);
            assert result.getDataID() == 0;
            assert result.getObjectID() == 0;
            result.setDataID(data.getRowId());

            ViabilityManager.saveResult(user, container, result);
        }
    }

    /*package*/ static Pair<Map<String, Object>, Map<String, Object>> splitBaseFromExtra(Map<String, Object> row)
    {
        Map<String, Object> base = new CaseInsensitiveHashMap<Object>();
        Map<String, Object> extra = new CaseInsensitiveHashMap<Object>();
        for (Map.Entry<String, Object> entry : row.entrySet())
        {
            if (ViabilityAssayProvider.RESULT_DOMAIN_PROPERTIES.containsKey(entry.getKey()))
                base.put(entry.getKey(), entry.getValue());
            else
                extra.put(entry.getKey(), entry.getValue());
        }

        return new Pair<Map<String, Object>, Map<String, Object>>(base, extra);
    }
}
