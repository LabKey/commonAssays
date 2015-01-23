/*
 * Copyright (c) 2014-2015 LabKey Corporation
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


    // NOTE: enforce ADT is created with lists...
    public static class AnalyteDefaultTransformer
    {
        private List<String> analytes;
        private List<String> positivityThresholds;
        private List<String> negativeBeads;
        private Map<String, Map<String, String>> analyteMap;

        public AnalyteDefaultTransformer()
        {
            analytes = new ArrayList<>();
            positivityThresholds = new ArrayList<>();
            negativeBeads = new ArrayList<>();
            analyteMap = new HashMap<>();
        }

        public AnalyteDefaultTransformer(List<String> analytes, List<String> positivityThresholds, List<String> negativeBeads)
        {
            this();

            // Consider making this into a stand alone method that also raises errors and is called in LuminexController before ADT
            if (analytes != null)
            {
                // prune incoming lists
                for (int i = analytes.size()-1; i >= 0; i--)
                {
                    // NOTE: this is kind of dangerous because where we are... no way to raise errors to user...
                    if (StringUtils.trimToNull(analytes.get(i)) == null)
                    {
                        analytes.remove(i);
                        positivityThresholds.remove(i);
                        negativeBeads.remove(i);
                    }
                }
            }

            if (analytes != null)
            {
                this.setAnalytes(analytes);
                // these could get nulled out if they are not truely adjacency lists...
                this.setPositivityThresholds(positivityThresholds);
                this.setNegativeBeads(negativeBeads);

                for (int i=0; i < analytes.size(); i++)
                {
                    Map<String, String> map = new HashMap<>();
                    String analyte = this.analytes.get(i);

                    String positivityThreshold = this.positivityThresholds.get(i);
                    if ( StringUtils.trimToNull(positivityThreshold) != null)
                    {
                        if (positivityThreshold.isEmpty()) positivityThreshold = "100"; // defaulting
                        map.put(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME, positivityThreshold);
                    }

                    String negativeBead = StringUtils.trimToNull(this.negativeBeads.get(i));
                    if (negativeBead != null) map.put(LuminexDataHandler.NEGATIVE_BEAD_COLUMN_NAME, negativeBead);

                    analyteMap.put(analyte, map);
                }
            }
        }

        public int size()
        {
            return analyteMap.size();
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

        private void addPositivityThreshold(String positivityThreshold)
        {
            this.positivityThresholds.add(positivityThreshold);
        }

        public List<String> getNegativeBead()
        {
            return negativeBeads;
        }

        private void addNegativeBead(String negativeBead)
        {
            this.negativeBeads.add(negativeBead);
        }

        public Map<String, Map<String, String>> getAnalyteMap()
        {
            return analyteMap;
        }

        public void setAnalyteMap(Map<String, Map<String, String>> analyteMap)
        {
            this.analyteMap = analyteMap;
        }

        public void setAnalytes(List<String> analytes)
        {
            this.analytes = analytes;
        }

        public void setNegativeBeads(List<String> negativeBeads)
        {
            this.negativeBeads = negativeBeads;
        }

        public void setPositivityThresholds(List<String> positivityThresholds)
        {
            this.positivityThresholds = positivityThresholds;
        }
    }

    public static List<String> getPropertyNames()
    {
        return propertyNames;
    }

    public static Map<String, String> getContainerDefaultValues(Container container, ExpProtocol protocol)
    {
        return PropertyManager.getProperties(container, getAnalyteColumnCategory(protocol));
    }

    public static PropertyManager.PropertyMap getWritableContainerDefaultValues(Container container, ExpProtocol protocol)
    {
        return PropertyManager.getWritableProperties(container, AnalyteDefaultValueService.getAnalyteColumnCategory(protocol), true);
    }

    public static Map<String, String> getUserDefaultValues(User user, Container container, ExpProtocol protocol)
    {
        return PropertyManager.getProperties(user, container, getAnalyteColumnCategory(protocol));
    }

    public static PropertyManager.PropertyMap getWritableUserDefaultValues(User user, Container container, ExpProtocol protocol)
    {
        return PropertyManager.getWritableProperties(user, container, AnalyteDefaultValueService.getAnalyteColumnCategory(protocol), true);
    }

    private static String getAnalyteColumnCategory(ExpProtocol protocol)
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
        List<String> result = new ArrayList<>(getAnalyteDefaultValues(protocol, container).keySet());
        Collections.sort(result);
        return result;
    }

    public static Map<String, Map<String, String>> getAnalyteDefaultValues(ExpProtocol protocol, Container container)
    {
        Map<String, Map<String, String>> analyteMap = new HashMap<>();
        for (Map.Entry<String, String> defaultValueEntry : getContainerDefaultValues(container, protocol).entrySet())
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
        Map<String, String> currentDefaults =  getContainerDefaultValues(container, protocol);
        List<String> result = new ArrayList<>();
        String propKey;
        for (String analyte : analytes)
        {
            propKey = getAnalytePropertyName(analyte, propertyName);
            if(currentDefaults.containsKey(propKey))
                result.add(currentDefaults.get(propKey));
            else
                // NOTE: I do not think this is the right places to enforce the default...
                result.add(propertyDefaults.get(propertyNames.indexOf(propertyName)));
        }
        return result;
    }

    public static void setAnalyteDefaultValues(Map<String, Map<String, String>> analyteProperties, Container container, ExpProtocol protocol)
    {
        PropertyManager.PropertyMap defaultAnalyteColumnValues = getWritableContainerDefaultValues(container, protocol);
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
        defaultAnalyteColumnValues.save();
    }

    // TODO: merge with the method above
    public static void setAnalyteDefaultValues(List<String> analytes, List<String> positivityThresholds, List<String> negativeBeads, Container container, ExpProtocol protocol)
    {
        PropertyManager.PropertyMap defaultAnalyteColumnValues = getWritableContainerDefaultValues(container, protocol);
        defaultAnalyteColumnValues.clear(); // NOTE: an empty property map would work too.
        if (analytes != null)
        {
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
        }
        defaultAnalyteColumnValues.save();
    }

    public static Map<String, String> getAnalyteColumnDefaultValues(ExpProtocol protocol, User user, Container container, boolean isReset)
    {
        Map<String, String> mergedDefaults = new HashMap<>();
        // fall back on any user last entered default values, so add them to the map first
        if (!isReset)
            mergedDefaults.putAll(getUserDefaultValues(user, container, protocol));
        // override map with any container level analyte default values (i.e. used as Editable Defaults)
        mergedDefaults.putAll(getContainerDefaultValues(container, protocol));
        return mergedDefaults;
    }
}