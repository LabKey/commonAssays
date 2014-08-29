/*
 * Copyright (c) 2014 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Container;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aaronr on 8/24/14.
 */
public class LuminexDefaultValueService
{
    private static final String[] propertyNames = {"PositivityThreshold", "NegativeBead"};
    // NOTE: defaults do get flushed to backend if user saves.
    private static final String[] propertyDefaults = {"100", ""};

    public static Map<String, String> getLuminexDefaultValues(Container container, ExpProtocol protocol)
    {
        return PropertyManager.getProperties(container, getAnalyteColumnCategory(protocol));
    }

    /*public static PropertyManager.PropertyMap getWritableLuminexDefaultValues(User user, Container container, ExpProtocol protocol)
    {
        return PropertyManager.getWritableProperties(user, container, LuminexDefaultValueService.getAnalyteColumnCategory(protocol), true);
    }*/

    //this should be private
    public static String getAnalyteColumnCategory(ExpProtocol protocol)
    {
        return protocol.getName() + ": Analyte Column";
    }

    public static String getAnalytePropertyName(String analyte, DomainProperty dp)
    {
        return getAnalytePropertyName(analyte, dp.getName());
    }

    public static String getAnalytePropertyName(String analyte, String property)
    {
        return "_analyte_" + analyte + "_" + property;
    }

    public static List<String> getAnalyteNames(ExpProtocol protocol)
    {
        // get list of all analytes currently provided for the given protocol
        SQLFragment sql = new SQLFragment("SELECT DISTINCT a.name FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        sql.append(" JOIN ");
        sql.append(LuminexProtocolSchema.getTableInfoDataRow(), "d");
        sql.append(" ON a.rowid = d.analyteid ");
        sql.append(" WHERE d.protocolid = ? ");
        sql.add(protocol.getRowId());
        return new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getArrayList(String.class);
    }

    /*public static TreeMap<String, Map> getAnalyteDefaultValues(List<String> analytes, User user, Container container, ExpProtocol protocol)
    {
        Map<String, String> currentDefaults =  getLuminexDefaultValues(user, container, protocol);
        TreeMap<String, Map> data = new TreeMap<>();
        Map<String, String> analyteDefaults;

        String propKey;
        for (String analyte : analytes)
        {
            analyteDefaults = new HashMap<>();
            for (int i=0; i<propertyNames.length; i++)
            {
                propKey = getAnalytePropertyName(analyte, propertyNames[i]);
                if(currentDefaults.containsKey(propKey))
                    analyteDefaults.put(propertyNames[i], currentDefaults.get(propKey));
                else
                    analyteDefaults.put(propertyNames[i], propertyDefaults[i]);
            }
            data.put(analyte, analyteDefaults);
        }

        return data;
    }*/

    public static List<String> getAnalytePositivityThresholds(List<String> analytes, Container container, ExpProtocol protocol)
    {
        Map<String, String> currentDefaults =  getLuminexDefaultValues(container, protocol);
        List<String> data = new ArrayList<>();
        String propKey;
        for (String analyte : analytes)
        {
            propKey = getAnalytePropertyName(analyte, propertyNames[0]);
            if(currentDefaults.containsKey(propKey))
                data.add(currentDefaults.get(propKey));
            else
                data.add(propertyDefaults[0]);
        }
        return data;
    }

    public static List<String> getAnalyteNegativeBeads(List<String> analytes, Container container, ExpProtocol protocol)
    {
        Map<String, String> currentDefaults =  getLuminexDefaultValues(container, protocol);
        List<String> data = new ArrayList<>();
        String propKey;
        for (String analyte : analytes)
        {
            propKey = getAnalytePropertyName(analyte, propertyNames[1]);
            if(currentDefaults.containsKey(propKey))
                data.add(currentDefaults.get(propKey));
            else
                data.add(propertyDefaults[1]);
        }
        return data;
    }

    public static void setAnalyteDefaultValues(List<String> analytes, List<String> positivityThresholds, List<String> negativeBeads, Container container, ExpProtocol protocol)
    {
        //PropertyManager.PropertyMap defaultAnalyteColumnValues = getWritableLuminexDefaultValues(user, container, protocol);
        PropertyManager.PropertyMap defaultAnalyteColumnValues = PropertyManager.getWritableProperties(container, LuminexDefaultValueService.getAnalyteColumnCategory(protocol), true);
        for (int i = 0; i < analytes.size(); i++)
        {
            String positivityThresholdPropKey = LuminexDefaultValueService.getAnalytePropertyName(analytes.get(i), "PositivityThreshold");

            if (positivityThresholds.get(i) != null)
                defaultAnalyteColumnValues.put(positivityThresholdPropKey, positivityThresholds.get(i));

            String negativeBeadPropKey = LuminexDefaultValueService.getAnalytePropertyName(analytes.get(i), "NegativeBead");
            String negativeBead = StringUtils.trimToNull(negativeBeads.get(i));
            if (negativeBead != null)
                defaultAnalyteColumnValues.put(negativeBeadPropKey, negativeBead);
        }
        PropertyManager.saveProperties(defaultAnalyteColumnValues);
    }

    /*
    public static List<String> getAnalytePositivityThresholds(TreeMap<String, Map> defaultValues)
    {
        List<String> data = new ArrayList<>();
        for (Map m : defaultValues.values()) data.add(m.get("PositivityThreshold").toString());
        return data;
    }

    public static List<String> getAnalyteNegativeBeads(TreeMap<String, Map> defaultValues)
    {
        List<String> data = new ArrayList<>();
        for (Map m : defaultValues.values())
        {
            if (m.get("NegativeBead") != null)
                data.add(m.get("NegativeBead").toString());
            else
                data.add(null);
        }
        return data;
    }*/

    public static Map<String, String> getAnalyteColumnDefaultValues(ExpProtocol protocol, User user, Container container, boolean isReset)
    {
        // get the container level default values and then override them with any user last entered default values
        Map<String, String> mergedDefaults = new HashMap<>();
        mergedDefaults.putAll(PropertyManager.getProperties(container, getAnalyteColumnCategory(protocol)));
        if (!isReset)
            mergedDefaults.putAll(PropertyManager.getProperties(user, container, getAnalyteColumnCategory(protocol)));

        return mergedDefaults;
    }
}
