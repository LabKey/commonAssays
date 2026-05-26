/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.api.protein;

public class PeptideUtils
{
    // Get rid of previous and next amino acid
    public static String trimPeptide(String peptide)
    {
        String[] p = peptide.split("\\.");

        if (2 < p.length)
            return p[1];
        else
            return peptide;
    }

    // Remove variable modification characters, leaving only A-Z
    public static String stripPeptide(String peptide)
    {
        return stripPeptideAZ(peptide);
    }

    // Remove variable modifications and '.', leaving only A-Z
    public static String stripPeptideAZ(String peptide)
    {
        StringBuilder stripped = new StringBuilder();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z')
                stripped.append(c);
        }

        return stripped.toString();
    }

    // String variable modifications and '.', leaving '-' and A-Z
    public static String stripPeptideAZDash(String peptide)
    {
        StringBuilder stripped = new StringBuilder();

        for (int i = 0; i < peptide.length(); i++)
        {
            char c = peptide.charAt(i);
            if (c >= 'A' && c <= 'Z' || c == '-')
                stripped.append(c);
        }

        return stripped.toString();
    }
}
