/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
import org.labkey.api.security.User;
import org.labkey.api.study.DilutionCurve;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayUrls;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewBackgroundInfo;

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
    public static final String NAB_DATA_ROW_LSID_PREFIX = "AssayRunNabDataRow";

    public static final String NAB_INPUT_MATERIAL_DATA_PROPERTY = "SpecimenLsid";
    public static final String WELLGROUP_NAME_PROPERTY = "WellgroupName";
    public static final String FIT_ERROR_PROPERTY = "Fit Error";
    public static final String CURVE_IC_PREFIX = "Curve IC";
    public static final String POINT_IC_PREFIX = "Point IC";
    public static final String AUC_PREFIX = "AUC";
    public static final String pAUC_PREFIX = "PositiveAUC";
    public static final String OORINDICATOR_SUFFIX = "OORIndicator";
    public static final String DATA_ROW_LSID_PROPERTY = "Data Row LSID";
    public static final String AUC_PROPERTY_FORMAT = "0.000";

    public interface NabDataFileParser
    {
        List<Map<String, Object>> getResults() throws ExperimentException;
    }

    public void importFile(ExpData data, File dataFile, ViewBackgroundInfo info, Logger log, XarContext context) throws ExperimentException
    {
        ExpRun run = data.getRun();
        ExpProtocol protocol = run.getProtocol();
        NabDataFileParser parser = getDataFileParser(data, dataFile, info, log, context);

        importRows(data, run, protocol, parser.getResults());
    }

    protected void importRows(ExpData data, ExpRun run, ExpProtocol protocol, List<Map<String, Object>> rawData) throws ExperimentException
    {
        try
        {
            Container container = run.getContainer();
            OntologyManager.ensureObject(container, data.getLSID());
            Map<Integer, String> cutoffFormats = getCutoffFormats(protocol, run);

            for (Map<String, Object> group : rawData)
            {
                if (!group.containsKey(WELLGROUP_NAME_PROPERTY))
                    throw new ExperimentException("The row must contain a value for the well group name : " + WELLGROUP_NAME_PROPERTY);

                String groupName = group.get(WELLGROUP_NAME_PROPERTY).toString();
                String dataRowLsid = getDataRowLSID(data, groupName).toString();

                OntologyManager.ensureObject(container, dataRowLsid,  data.getLSID());
                List<ObjectProperty> results = new ArrayList<ObjectProperty>();

                for (Map.Entry<String, Object> prop : group.entrySet())
                {
                    if (prop.getKey().equals(DATA_ROW_LSID_PROPERTY))
                        continue;

                    ObjectProperty objProp = getObjectProperty(container, protocol, dataRowLsid, prop.getKey(), prop.getValue(), cutoffFormats);
                    if (objProp != null)
                        results.add(objProp);
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

    protected static ObjectProperty getObjectProperty(Container container, ExpProtocol protocol, String objectURI, String propertyName, Object value, Map<Integer, String> cutoffFormats)
    {
        if (isValidDataProperty(propertyName))
        {
            PropertyType type = PropertyType.STRING;
            String format = null;

            if (propertyName.equals(FIT_ERROR_PROPERTY))
            {
                type = PropertyType.DOUBLE;
                format = "0.0";
            }
            else if (propertyName.startsWith(AUC_PREFIX) || propertyName.startsWith(pAUC_PREFIX))
            {
                type = PropertyType.DOUBLE;
                format = AUC_PROPERTY_FORMAT;
            }
            else if (propertyName.startsWith(CURVE_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            else if (propertyName.startsWith(POINT_IC_PREFIX))
            {
                Integer cutoff = getCutoffFromPropertyName(propertyName);
                if (cutoff != null)
                {
                    format = cutoffFormats.get(cutoff);
                    type = PropertyType.DOUBLE;
                }
            }
            return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, format);
        }
        return null;
    }

    protected static boolean isValidDataProperty(String propertyName)
    {
        if (DATA_ROW_LSID_PROPERTY.equals(propertyName)) return true;
        if (NAB_INPUT_MATERIAL_DATA_PROPERTY.equals(propertyName)) return true;
        if (WELLGROUP_NAME_PROPERTY.equals(propertyName)) return true;
        if (FIT_ERROR_PROPERTY.equals(propertyName)) return true;

        if (propertyName.startsWith(AUC_PREFIX)) return true;
        if (propertyName.startsWith(pAUC_PREFIX)) return true;
        if (propertyName.startsWith(CURVE_IC_PREFIX)) return true;
        if (propertyName.startsWith(POINT_IC_PREFIX)) return true;

        return false;
    }

    public static Lsid getDataRowLSID(ExpData data, String wellGroupName)
    {
        Lsid dataRowLsid = new Lsid(data.getLSID());
        dataRowLsid.setNamespacePrefix(NAB_DATA_ROW_LSID_PREFIX);
        dataRowLsid.setObjectId(dataRowLsid.getObjectId() + "-" + wellGroupName);
        return dataRowLsid;
    }

    protected static ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
                                                   String propertyName, Object value, PropertyType type)
    {
        return getResultObjectProperty(container, protocol, objectURI, propertyName, value, type, null);
    }

    protected static ObjectProperty getResultObjectProperty(Container container, ExpProtocol protocol, String objectURI,
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

    static public String getPropertyName(String prefix, int cutoff, DilutionCurve.FitType type)
    {
        return getPropertyName(prefix + cutoff, type);
    }

    static public String getPropertyName(String prefix, DilutionCurve.FitType type)
    {
        return prefix + "_" + type.getColSuffix();
    }

    static public Integer getCutoffFromPropertyName(String propertyName)
    {
        if (propertyName.startsWith(CURVE_IC_PREFIX) && !propertyName.endsWith(OORINDICATOR_SUFFIX))
        {
            // parse out the cutoff number
            int idx = propertyName.indexOf('_');
            String num;
            if (idx != -1)
                num = propertyName.substring(CURVE_IC_PREFIX.length(), propertyName.indexOf('_'));
            else
                num = propertyName.substring(CURVE_IC_PREFIX.length());

            return Integer.valueOf(num);
        }
        else if (propertyName.startsWith(POINT_IC_PREFIX) && !propertyName.endsWith(OORINDICATOR_SUFFIX))
        {
            return Integer.valueOf(propertyName.substring(POINT_IC_PREFIX.length()));
        }
        return null;
    }
}
