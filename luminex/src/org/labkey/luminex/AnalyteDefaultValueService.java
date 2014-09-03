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

    public static class AnalyteDefaultTransformer
    {
        private List<String> analytes;
        private List<String> positivityThresholds;
        private List<String> negativeBeads;

        public AnalyteDefaultTransformer(){
            analytes = new ArrayList<>();
            positivityThresholds = new ArrayList<>();
            negativeBeads = new ArrayList<>();
        }

        public AnalyteDefaultTransformer(Map<String, Map<String, String>> analyteProperities)
        {
            this();
            for(Map.Entry<String, Map<String, String>> entry : analyteProperities.entrySet())
            {
                analytes.add(entry.getKey());

                for(Map.Entry<String, String> row : entry.getValue().entrySet())
                {
                    // NOTE: can this be prettier?
                    if (row.getKey().equals(propertyNames.get(0))) positivityThresholds.add(row.getValue());
                    if (row.getKey().equals(propertyNames.get(1))) negativeBeads.add(row.getValue());
                }
            }
        }

        public int size()
        {
            return analytes.size();
        }

        public List<String> getAnalytes()
        {
            return analytes;
        }

        private void addAnalyte(String analyte)
        {
            this.analytes.add(analyte);
        }

        public List<String> getPositivityThreshold()
        {
            return positivityThresholds;
        }

        private void setPositivityThreshold(String positivityThreshold)
        {
            this.positivityThresholds.add(positivityThreshold);
        }

        public List<String> getNegativeBead()
        {
            return negativeBeads;
        }

        private void setNegativeBead(String negativeBead)
        {
            this.negativeBeads.add(negativeBead);
        }
    }

    public static List<String> getPropertyNames()
    {
        return propertyNames;
    }

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

    public static void setAnalyteDefaultValues(Map<String, Map<String, String>> analyteProperties, Container container, ExpProtocol protocol)
    {
        PropertyManager.PropertyMap defaultAnalyteColumnValues = PropertyManager.getWritableProperties(container, AnalyteDefaultValueService.getAnalyteColumnCategory(protocol), true);
        defaultAnalyteColumnValues.clear(); // NOTE: an empty property map would work too.
        for(Map.Entry<String, Map<String, String>> entry : analyteProperties.entrySet())
        {
            String analyte = StringUtils.trimToNull(entry.getKey());
            if (analyte != null)
            {
                for (String propertyName : propertyNames)
                {
                    if(entry.getValue().containsKey(propertyName))
                    {
                        String value = StringUtils.trimToNull(entry.getValue().get(propertyName));
                        if (value != null)
                        {
                            String propKey = AnalyteDefaultValueService.getAnalytePropertyName(analyte, propertyName);
                            defaultAnalyteColumnValues.put(propKey, value);
                        }
                    }
                }
            }
        }
        PropertyManager.saveProperties(defaultAnalyteColumnValues);
    }

    // TODO: merge with the method above
    public static void setAnalyteDefaultValues(List<String> analytes, List<String> positivityThresholds, List<String> negativeBeads, Container container, ExpProtocol protocol)
    {
        PropertyManager.PropertyMap defaultAnalyteColumnValues = PropertyManager.getWritableProperties(container, AnalyteDefaultValueService.getAnalyteColumnCategory(protocol), true);
        defaultAnalyteColumnValues.clear(); // NOTE: an empty property map would work too.
        for (int i = 0; i < analytes.size(); i++)
        {
            String analyte = StringUtils.trimToNull(analytes.get(i));
            if (analyte != null)
            {
                String positivityThresholdPropKey = AnalyteDefaultValueService.getAnalytePropertyName(analytes.get(i), LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
                // this probably won't trim to null because it defaults to 100...
                String positivityThreshold = StringUtils.trimToNull(positivityThresholds.get(i));

                if (positivityThreshold != null)
                    defaultAnalyteColumnValues.put(positivityThresholdPropKey, positivityThreshold);

                String negativeBeadPropKey = AnalyteDefaultValueService.getAnalytePropertyName(analytes.get(i), LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME);
                String negativeBead = StringUtils.trimToNull(negativeBeads.get(i));
                if (negativeBead != null)
                    defaultAnalyteColumnValues.put(negativeBeadPropKey, negativeBead);
            }
        }
        PropertyManager.saveProperties(defaultAnalyteColumnValues);
    }

    public static Map<String, String> getAnalyteColumnDefaultValues(ExpProtocol protocol, User user, Container container, boolean isReset)
    {
        Map<String, String> mergedDefaults = new HashMap<>();
        // fall back on any user last entered default values, so add them to the map first
        if (!isReset)
            mergedDefaults.putAll(PropertyManager.getProperties(user, container, getAnalyteColumnCategory(protocol)));
        // override map with any container level analyte default values (i.e. used as Editable Defaults)
        mergedDefaults.putAll(PropertyManager.getProperties(container, getAnalyteColumnCategory(protocol)));
        return mergedDefaults;
    }
}