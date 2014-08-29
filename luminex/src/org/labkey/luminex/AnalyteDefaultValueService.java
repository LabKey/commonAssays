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
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aaronr on 8/24/14.
 */
public class AnalyteDefaultValueService
{
    private static final List<String> propertyNames = Arrays.asList(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME, LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
    // NOTE: defaults do get flushed to backend if user saves.
    private static final List<String> propertyDefaults = Arrays.asList("100", "");
    private static final String PROP_NAME_PREFIX = "_analyte_";

    public static Map<String, String> getLuminexDefaultValues(Container container, ExpProtocol protocol)
    {
        return PropertyManager.getProperties(container, getAnalyteColumnCategory(protocol));
    }

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
        return PROP_NAME_PREFIX + analyte + "_" + property;
    }

    public static List<String> getAnalyteNames(ExpProtocol protocol, Container container)
    {
        return getAnalyteNames(protocol, container, false);
    }

    public static List<String> getAnalyteNames(ExpProtocol protocol, Container container, boolean sort)
    {
        List<String> result = new ArrayList<>(getAnalyteDefaultValues(protocol, container).keySet());
        if (sort) Collections.sort(result);
        return result;
    }

    public static Map<String, Map<String, String>> getAnalyteDefaultValues(ExpProtocol protocol, Container container)
    {
        Map<String, Map<String, String>> analyteMap = new HashMap<>();
        for (Map.Entry<String, String> defaultValueEntry : getLuminexDefaultValues(container, protocol).entrySet())
        {
            String key = defaultValueEntry.getKey().replace(PROP_NAME_PREFIX, "");
            for (String propertyName : propertyNames)
            {
                if (key.endsWith("_" + propertyName))
                {
                    String analyte = key.substring(0, key.indexOf("_" + propertyName));
                    if (!analyteMap.containsKey(analyte))
                        analyteMap.put(analyte, new HashMap<String, String>());

                    analyteMap.get(analyte).put(propertyName, defaultValueEntry.getValue());
                }
            }
        }

        return analyteMap;
    }

    public static List<String> getAnalyteProperty(List<String> analytes, Container container, ExpProtocol protocol, String propertyName)
    {
        // TODO: catch bad propertyNames...
        Map<String, String> currentDefaults =  getLuminexDefaultValues(container, protocol);
        List<String> result = new ArrayList<>();
        String propKey;
        for (String analyte : analytes)
        {
            propKey = getAnalytePropertyName(analyte, propertyName);
            if(currentDefaults.containsKey(propKey))
                result.add(currentDefaults.get(propKey));
            else
                result.add(propertyDefaults.get(propertyNames.indexOf(propertyName)));
        }
        return result;
    }

    public static void setAnalyteDefaultValues(List<String> analytes, List<String> positivityThresholds, List<String> negativeBeads, Container container, ExpProtocol protocol)
    {
        PropertyManager.PropertyMap defaultAnalyteColumnValues = PropertyManager.getWritableProperties(container, AnalyteDefaultValueService.getAnalyteColumnCategory(protocol), true);
        defaultAnalyteColumnValues.clear(); // NOTE: an empty property map would work too.
        for (int i = 0; i < analytes.size(); i++)
        {
            String analyte = StringUtils.trimToNull(analytes.get(i));
            if (analyte != null)
            {
                String positivityThresholdPropKey = AnalyteDefaultValueService.getAnalytePropertyName(analytes.get(i), propertyNames.get(0));
                // this probably won't trim to null because it defaults to 100...
                String positivityThreshold = StringUtils.trimToNull(positivityThresholds.get(i));

                if (positivityThreshold != null)
                    defaultAnalyteColumnValues.put(positivityThresholdPropKey, positivityThreshold);

                String negativeBeadPropKey = AnalyteDefaultValueService.getAnalytePropertyName(analytes.get(i), propertyNames.get(0));
                String negativeBead = StringUtils.trimToNull(negativeBeads.get(i));
                if (negativeBead != null)
                    defaultAnalyteColumnValues.put(negativeBeadPropKey, negativeBead);
            }
        }
        PropertyManager.saveProperties(defaultAnalyteColumnValues);
    }

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