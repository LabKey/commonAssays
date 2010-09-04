/*
 * Copyright (c) 2007-2008 LabKey Corporation
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
 * Date: Apr 11, 2007
 */
public class ProteinGroupQueryNestingOption extends QueryNestingOption
{
    public ProteinGroupQueryNestingOption(boolean allowNesting)
    {
        super("RowId", allowNesting);
    }
    
    public boolean isOuter(String columnName)
    {
        return columnName.indexOf("/") < 0;
    }
}
