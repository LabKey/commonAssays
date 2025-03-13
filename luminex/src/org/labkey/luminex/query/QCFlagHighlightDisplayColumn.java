/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.luminex.query;

import org.apache.commons.beanutils.ConvertUtils;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.DOM;
import org.labkey.api.writer.HtmlWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.labkey.api.util.DOM.Attribute.style;
import static org.labkey.api.util.DOM.SPAN;
import static org.labkey.api.util.DOM.at;

public class QCFlagHighlightDisplayColumn extends DataColumn
{
    private final FieldKey _qcFlagsEnabledKey;

    public QCFlagHighlightDisplayColumn(ColumnInfo colInfo, String flagEnabledColName)
    {
        super(colInfo);
        FieldKey parentFK = colInfo.getFieldKey().getParent();
        _qcFlagsEnabledKey = new FieldKey(parentFK, flagEnabledColName);
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_qcFlagsEnabledKey);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, HtmlWriter out)
    {
        // comma separated list of enabled states for any associated QC Flags (i.e. true,false)
        String flagsEnabled = ctx.get(_qcFlagsEnabledKey, String.class);
        List<Boolean> enabled = parseBooleans(flagsEnabled);

        if (enabled.contains(true))
        {
            SPAN(
                at(style, "color:red;"),
                    (DOM.Renderable) ret -> {
                        super.renderGridCellContents(ctx, out);
                        return ret;
                    }
            ).appendTo(out);
        }
        else
        {
            super.renderGridCellContents(ctx, out);
        }
    }

    private List<Boolean> parseBooleans(String s)
    {
        List<Boolean> enabled = new ArrayList<>();
        if (s != null)
        {
            String[] enabledSplit = s.split(",");
            for (String val : enabledSplit)
            {
                // The standard Boolean converter doesn't understand "f" or "t", which is what Postgres returns
                if ("f".equalsIgnoreCase(val))
                {
                    enabled.add(false);
                }
                else if ("t".equalsIgnoreCase(val))
                {
                    enabled.add(true);
                }
                else
                {
                    enabled.add((Boolean) ConvertUtils.convert(val, Boolean.class));
                }
            }
        }
        return enabled;
    }
}
