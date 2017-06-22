/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.JdbcType;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StringExpression;
import org.labkey.data.xml.StringExpressionType;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * User: arauch
 * Date: Apr 4, 2006
 * Time: 4:15:26 PM
 */
public class ProteinStringExpression implements StringExpression, Cloneable
{
    private String _localURI;

    public ProteinStringExpression(String localURI)
    {
        _localURI = localURI;
    }

    public String eval(Map ctx)
    {
        Integer seqId = (Integer)ctx.get("SeqId");

        // Always include protein (use as a title in the details page); include SeqId if it's not null
        return _localURI + (null != seqId ? "&seqId=" + seqId : "") + "&protein=" + PageFlowUtil.encode((String)ctx.get("Protein"));
    }

    public String getSource()
    {
        return _localURI + "&seqId={$SeqId}";
    }

    public void render(Writer out, Map ctx) throws IOException
    {
        out.write(eval(ctx));
    }

    public boolean canRender(Set<FieldKey> fieldKeys)
    {
        return fieldKeys.contains(FieldKey.fromParts("SeqId"));
    }

    public ProteinStringExpression copy()
    {
        return clone();
    }

    @Override
    public ProteinStringExpression clone()
    {
        try
        {
            return (ProteinStringExpression)super.clone();
        }
        catch (CloneNotSupportedException x)
        {
            throw new RuntimeException(x);
        }
    }

    @Nullable
    @Override
    public Object getJdbcParameterValue()
    {
        return getSource();
    }

    @NotNull
    @Override
    public JdbcType getJdbcParameterType()
    {
        return JdbcType.VARCHAR;
    }

    @Override
    public StringExpressionType toXML()
    {
        StringExpressionType xurl = StringExpressionType.Factory.newInstance();
        xurl.setStringValue(_localURI);
        return xurl;
    }
}
