/*
 * Copyright (c) 2006-2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nab;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.*;
import org.labkey.api.exp.api.*;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.ValidationException;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ActionURL;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 15, 2009
 */
public abstract class AbstractNabDataHandler extends AbstractExperimentDataHandler
{
    public static final String NAB_PROPERTY_LSID_PREFIX = "NabProperty";

    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    public static final String FIT_ERROR_PROPERTY = "Fit Error";
    public static final String CURVE_IC_PREFIX = "Curve IC";
    public static final String POINT_IC_PREFIX = "Point IC";
    public static final String OORINDICATOR_SUFFIX = "OORIndicator";
    public static final String DATA_ROW_LSID_PROPERTY = "Data Row LSID";

    public interface NabDataFileParser
    {
        List<Map<String, Object>> getResults() throws ExperimentException;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        try
        {
            ExpRun run = data.getRun();
            ExpProtocol protocol = run.getProtocol();

            Container container = info.getContainer();
            OntologyManager.ensureObject(container, data.getLSID());
            NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);

            for (Map<String, Object> group : parser.getResults())
            {
                if (!group.containsKey(DATA_ROW_LSID_PROPERTY))
                    throw new ExperimentException("The row must contain a value for the data row LSID : " + DATA_ROW_LSID_PROPERTY);

                String dataRowLsid = group.get(DATA_ROW_LSID_PROPERTY).toString();

                OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();

                for (Map.Entry<String, Object> prop : group.entrySet())
                {
                    if (prop.getKey().equals(DATA_ROW_LSID_PROPERTY))
                        continue;
                    results.add(getObjectProperty(container, protocol, dataRowLsid, prop.getKey(), prop.getValue(), cutoffFormats));
                }
                OntologyManager.insertProperties(container, dataRowLsid, results.toArray(new ObjectProperty[results.size()]));
            }
        }
        catch (ValidationException ve)
        {
            throw new ExperimentException(ve.getMessage(), ve);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    protected ObjectProperty getObjectProperty(Container container, ExpProtocol protocol, String objectURI, String propertyName, Object value, Map<Integer, String> cutoffFormats)
    {
        PropertyType type = PropertyType.STRING;
        String format = null;

        if (propertyName.equals(FIT_ERROR_PROPERTY))
        {
            type = PropertyType.DOUBLE;
            format = "0.0";
        }
        else if (propertyName.startsWith(CURVE_IC_PREFIX))
        {
            // parse out the cutoff number
            if (!propertyName.endsWith(OORINDICATOR_SUFFIX))
            {
                String num = propertyName.substring(CURVE_IC_PREFIX.length());
                format = cutoffFormats.get(Integer.valueOf(num));
                type = PropertyType.DOUBLE;
            }
        }
        else if (propertyName.startsWith(POINT_IC_PREFIX))
        {
            // parse out the cutoff number
            if (!propertyName.endsWith(OORINDICATOR_SUFFIX))
            {
                String num = propertyName.substring(CURVE_IC_PREFIX.length());
                format = cutoffFormats.get(Integer.valueOf(num));
                type = PropertyType.DOUBLE;
            }
        }
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, format);
    }

    protected ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    protected ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type, String format)
    {
        Lsid propertyURI = new Lsid(NAB_PROPERTY_LSID_PREFIX, protocol.getName(), propertyName);
        ObjectProperty prop = new ObjectProperty(objectURI, container, propertyURI.toString(), value, type, propertyName);
        prop.setFormat(format);
        return prop;
    }

    protected static Map<Integer, String> getCutoffFormats(ExpProtocol protocol, ExpRun run)
    {
        NabAssayProvider provider = (NabAssayProvider) AssayService.get().getProvider(protocol);

        Map<String, DomainProperty> runProperties = new HashMap<String, DomainProperty>();
        for (DomainProperty column : provider.getRunDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);
        for (DomainProperty column : provider.getBatchDomain(protocol).getProperties())
            runProperties.put(column.getName(), column);

        Map<Integer, String> cutoffs = new HashMap<Integer, String>();
        for (String cutoffPropName : NabAssayProvider.CUTOFF_PROPERTIES)
        {
            DomainProperty cutoffProp = runProperties.get(cutoffPropName);
            if (cutoffProp != null)
            {
                Integer cutoff = (Integer) run.getProperty(cutoffProp);
                if (cutoff != null)
                    cutoffs.put(cutoff, cutoffProp.getPropertyDescriptor().getFormat());
            }
        }

        if (cutoffs.isEmpty())
        {
            cutoffs.put(50, "0.000");
            cutoffs.put(80, "0.000");
        }
        return cutoffs;
    }

    public abstract NabDataFileParser getDataFileParser(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException;

    public void runMoved(ExpData newData, Container container, Container targetContainer, String oldRunLSID, String newRunLSID, User user, int oldDataRowID) throws ExperimentException
    {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    public ActionURL getContentURL(Container container, ExpData data)
    {
        ExpRun run = data.getRun();
        if (run != null)
        {
            ExpProtocol protocol = run.getProtocol();
            ExpProtocol p = ExperimentService.get().getExpProtocol(protocol.getRowId());
            return PageFlowUtil.urlProvider(AssayUrls.class).getAssayResultsURL(container, p, run.getRowId());
        }
        return null;
    }

    public void deleteData(ExpData data, Container container, User user)
    {
        try
        {
            OntologyManager.deleteOntologyObject(data.getLSID(), container, true);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }
}
