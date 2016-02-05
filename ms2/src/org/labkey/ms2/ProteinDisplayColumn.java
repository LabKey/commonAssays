/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

import org.json.JSONObject;
import org.labkey.api.data.AJAXDetailsDisplayColumn;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.ActionURL;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * User: adam
 * Date: Aug 3, 2006
 * Time: 10:42 AM
 */
public class ProteinDisplayColumn extends AJAXDetailsDisplayColumn
{
    private final FieldKey _seqIdFK;
    private final FieldKey _proteinNameFK;
    private boolean _renderedCSS = false;

    public ProteinDisplayColumn(ColumnInfo col, ActionURL url, Map<String, FieldKey> params)
    {
        super(col, url, params, new JSONObject().put("width", 450).put("title", "Protein Details"));
        setLinkTarget("prot");

        FieldKey parentFK = getColumnInfo().getFieldKey().getParent();
        _seqIdFK = new FieldKey(parentFK, "SeqId");
        _proteinNameFK = new FieldKey(parentFK, "Protein");
        addRequiredValue(_seqIdFK);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        if (!_renderedCSS)
        {
            out.write("<script type=\"text/javascript\">\n" +
            "LABKEY.requiresCss(\"ProteinCoverageMap.css\");\n" +
            "LABKEY.requiresScript(\"util.js\");\n" +
            "</script>");
            _renderedCSS = true;
        }
        super.renderGridCellContents(ctx, out);
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_seqIdFK);
        keys.add(_proteinNameFK);
    }
}
