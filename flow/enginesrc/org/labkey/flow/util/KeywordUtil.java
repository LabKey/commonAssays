/*
 * Copyright (c) 2011-2013 LabKey Corporation
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

package org.labkey.flow.util;

import org.labkey.api.collections.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User: kevink
 * Date: 5/27/11
 */
public class KeywordUtil
{
    private static Pattern hiddenKeyword = Pattern.compile("^(" +
            "\\$BEGINANALYSIS|\\$BEGINDATA|\\$BEGINSTEXT|" +
            "\\$ENDANALYSIS|\\$ENDDATA|\\$ENDSTEXT|" +
            "\\$BYTEORD|\\$DATATYPE|\\$MODE|\\$NEXTDATA|" +
            "\\$P\\d+.*|\\$PAR|\\$TOT|" +
            "P\\d+DISPLAY|" +
            "P\\d+BS|" +
            "P\\d+MS|" +
            "\\$ABRT|\\$BTIM|\\$ETIM|" +
            "\\$CSMODE|\\$CSVBITS|" +
            "\\$CSV\\d+FLAG|" +
            "\\$GATING|\\$LOST|" +
            "\\$PK\\d+.*|" +
            "\\$G\\d+.*|\\$R\\d.*|" +
            "\\$LASER\\d+.*|" +
            "LASER\\d+.*|" +
            "\\$TIMESTEP|" +
            "FJ_\\$.*|" +
            "BD\\$.*|" +
            "CST .*|" +
            "SPILL|" +
            "\\$DFC\\d+TO\\d+|" +
            "APPLY COMPENSATION|" +
            "CREATOR|" +
            "\\$CYT|\\$SYS|" +
            "FSC ASF|" +
            "THRESHOLD|" +
            "AUTOBS|" +
            "GUID|" +
            "CYTOMETER CONFIG .*|" +
            "WINDOW EXTENSION)$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> sideScatterNames = Sets.newCaseInsensitiveHashSet(
            "SSC", "SS", "SS Lin", "SS Log",
            "SS-A", "SSC-A", "SSC-Area",
            "SS-H", "SSC-H", "SSC-Height",
            "SS-W", "SSC-W", "SSC-Width", "Comp SSC-A",
            "Side Scatter", "OrthSc", "ObtSc", "90D"
    );

    private static final Set<String> forwardScatterNames = Sets.newCaseInsensitiveHashSet(
            "FSC", "FS", "FS Lin", "FS Log",
            "FS-A", "FSC-A", "FSC-Area",
            "FS-H", "FSC-H", "FSC-Height",
            "FS-W", "FSC-W", "FSC-Width", "Comp FSC-H",
            "Forward Scatter", "ForSc", "FS Peak"
    );

    private KeywordUtil() { }

    /**
     * Skip commonly ignored keywords.
     * @param keyword The keyword.
     * @return true if hidden.
     */
    public static boolean isHidden(String keyword)
    {
        return hiddenKeyword.matcher(keyword).matches();
    }

    public static Collection<String> filterHidden(Collection<String> keywords)
    {
        ArrayList<String> ret = new ArrayList<>(keywords.size());
        for (String s : keywords)
            if (!isHidden(s))
                ret.add(s);
        return ret;
    }

    // UNDONE: $SPILL, SPILLOVER, COMP, $COMP
    public static boolean hasSpillKeyword(Map<String, String> keywords)
    {
        return keywords.containsKey("SPILL") || keywords.containsKey("$DFC1TO2");
    }

    public static boolean isSideScatter(String parameterName)
    {
        return sideScatterNames.contains(parameterName);
    }

    public static boolean isForwardScatter(String parameterName)
    {
        return forwardScatterNames.contains(parameterName);
    }

    public static boolean isTimeChannel(String parameterName)
    {
        return parameterName.toLowerCase().contains("time");
    }

    public static boolean isElectronicVoltage(String parameterName)
    {
        return parameterName.equalsIgnoreCase("EV");
    }

    public static boolean isColorChannel(String parameterName)
    {
        return !isForwardScatter(parameterName) &&
                !isSideScatter(parameterName) &&
                !isTimeChannel(parameterName) &&
                !isElectronicVoltage(parameterName);
    }

}
