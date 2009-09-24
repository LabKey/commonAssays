/*
 * Copyright (c) 2006-2009 LabKey Corporation
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

import org.labkey.api.util.StringExpression;
import org.labkey.api.util.PageFlowUtil;

import java.util.Map;
import java.util.Set;
import java.io.Writer;
import java.io.IOException;

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
        return _localURI + (null != seqId ? "&seqId=" + seqId : "") + "&protein=" + ctx.get("Protein");
    }

    public String getSource()
    {
        return _localURI + "&seqId={$SeqId}";
    }

    public void render(Writer out, Map ctx) throws IOException
    {
        out.write(eval(ctx));
    }

    public ProteinStringExpression copy()
    {
        return clone();
    }

    @Override
    protected ProteinStringExpression clone()
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
}
