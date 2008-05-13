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

package org.labkey.ms2;

import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;

/**
 * User: jeckels
 * Date: Oct 25, 2007
 */
public class UniqueFilteredPeptidesColumn extends SimpleDisplayColumn
{
    public static final String NAME = "UniqueFilteredPeptides";

    public UniqueFilteredPeptidesColumn()
    {
        super();
        setCaption("Unique Filtered Peptides");
        setWidth("30");
        setTextAlign("right");
    }

    public Object getValue(RenderContext ctx)
    {
        return ctx.get(NAME);
    }

    public Class getValueClass()
    {
        return Integer.class;
    }

    public String getName()
    {
        return NAME;
    }
}