/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.api.exp.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.api.*;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.query.ValidationException;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.File;
import java.sql.SQLException;

/**
 * User: kevink
 * Date: Sep 16, 2009
 */
public class ViabilityAssayDataHandler extends AbstractAssayTsvDataHandler
{
    public static final DataType DATA_TYPE = new DataType("ViabilityAssayData");

    protected boolean allowEmptyData()
    {
        return true;
    }

    protected boolean shouldAddInputMaterials()
    {
        return false;
    }

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid))
            return Priority.HIGH;
        return null;
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        return null;
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Map<DataType, List<Map<String, Object>>> getValidationDataMap(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        Map<DataType, List<Map<String, Object>>> result = new HashMap<DataType, List<Map<String, Object>>>();
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        Map<String, Object> row = new HashMap<String, Object>();
        row.put("PoolID", "12345-67890");
        row.put("TotalCells", 10000);
        row.put("ViableCells", 9000);
        row.put("Viability", 0.9);
        row.put("SpecimenIDs", Arrays.asList("111", "222", "333"));
        rows.add(row);

        result.put(DATA_TYPE, rows);
        return result;
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
        for (Map<String, Object> row : fileData)
        {
            ViabilityResult result = ViabilityResult.fromMap(row);
            assert result.getDataID() == 0;
            result.setDataID(data.getRowId());

            ViabilityManager.saveResult(user, result);
        }
    }

}
