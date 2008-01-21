/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms1.query;

import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;

import java.io.Writer;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 16, 2008
 * Time: 4:34:02 PM
 */
public class FindSimilarDisplayColumn extends SimpleDisplayColumn
{
    private ActionURL _url = null;

    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        if(null == _url)
        {
            _url = MS1Controller.SimilarSearchForm.getDefaultUrl(ctx.getViewContext().getContainer());
            _url.addParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name(), "0");
        }

        Integer featureId = (Integer)ctx.get("FeatureId");
        if(null == featureId)
            return;

        _url.replaceParameter(MS1Controller.SimilarSearchForm.ParamNames.featureId.name(),
                featureId.toString());

        out.write("[<a href=\"");
        out.write(_url.getLocalURIString());
        out.write("\">similar</a>]");
    }
}
