/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.ms2.peptideview;

/**
 * User: jeckels
 * Date: Apr 9, 2007
 */
public class StandardProteinQueryNestingOption extends QueryNestingOption
{
    private static final String PREFIX = "SeqId/";
    private static final String PROTEIN_ROWID = PREFIX + "SeqId";

    public StandardProteinQueryNestingOption(boolean allowNesting)
    {
        super(PROTEIN_ROWID, allowNesting);
    }

    public int getOuterGroupLimit()
    {
        return _allowNesting ? 250 : 0;
    }

    public int getResultSetRowLimit()
    {
        return _allowNesting ? 15000 : 0;
    }

    public boolean isOuter(String columnName)
    {
        return columnName.toLowerCase().startsWith(PREFIX.toLowerCase());
    }
}
