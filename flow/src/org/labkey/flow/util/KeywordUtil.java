/*
 * Copyright (c) 2011 LabKey Corporation
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

import java.util.ArrayList;
import java.util.Collection;
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
            "\\$ABRT|\\$BTIM|\\$ETIM|" +
            "\\$CSMODE|\\$CSVBITS|" +
            "\\$CSV\\d+FLAG|" +
            "\\$GATING|\\$LOST|" +
            "\\$PK\\d+.*|" +
            "\\$G\\d+.*|\\$R\\d.*|" +
            "\\$LASER\\d+.*|" +
            "\\$TIMESTEP|" +
            "FJ_\\$.*|" +
            "SPILL|" +
            "\\$DFC\\d+TO\\d+|" +
            "APPLY COMPENSATION|" +
            "CREATOR|" +
            "\\$CYT|\\$SYS|" +
            "FSC ASF|" +
            "THREASHOLD|" +
            "WINDOW EXTENSION)$", Pattern.CASE_INSENSITIVE);

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
        ArrayList<String> ret = new ArrayList<String>(keywords.size());
        for (String s : keywords)
            if (!isHidden(s))
                ret.add(s);
        return ret;
    }
}
