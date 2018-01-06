/*
 * Copyright (c) 2005-2013 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein.tools;


import org.jfree.data.general.DefaultPieDataset;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * User: tholzman
 * Date: Oct 31, 2005
 * Time: 4:30:14 PM
 */
public class ProteinPieDataset extends DefaultPieDataset
{
    public Map<String, Set<Integer>> getExtraInfo()
    {
        return extraInfo;
    }

    public void setExtraInfo(Map<String, Set<Integer>> extraInfo)
    {
        this.extraInfo = extraInfo;
    }

    protected Map<String, Set<Integer>> extraInfo = new HashMap<>();

    public ProteinPieDataset()
    {
        super();
    }
}
