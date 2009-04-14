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

package org.labkey.ms2.xarassay;

import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.security.ACL;
import org.labkey.api.study.assay.AssayRunUploadContext;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: phussey
 * Date: Sep 23, 2007
 * Time: 1:09:11 PM
 */
public class XarAssayDataCollector extends PipelineDataCollector
{
    public static String NAME="mzXMLFiles";

    public XarAssayDataCollector()
    {
    }

    public boolean isVisible()
    {
        return true;
    }

    public String getHTML(AssayRunUploadContext context) throws ExperimentException
    {
        StringBuilder sb = new StringBuilder(super.getHTML(context));
        XarAssayForm ctx = (XarAssayForm)context;

        // Validate that we have a pipeline root configured
        Map<String, String> links = new LinkedHashMap<String, String>();

        ActionURL showSamplesURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowSampleSetURL(ExperimentService.get().ensureActiveSampleSet(context.getContainer()));
        links.put("Edit Samples", showSamplesURL.toString());

        if (ctx.getContainer().hasPermission(ctx.getUser(), ACL.PERM_DELETE))
        {
            ActionURL deleteURL = new ActionURL(XarAssayUploadAction.class, context.getContainer());
            deleteURL.addParameter("path", ctx.getPath());
            deleteURL.addParameter("rowId", ctx.getProtocol().getRowId());
            deleteURL.addParameter("uploadStep", XarAssayUploadAction.DeleteAssaysStepHandler.NAME);
            deleteURL.addParameter("providerName", ctx.getProviderName());
            links.put("Delete Assay Runs", "javascript: if (window.confirm('Are you sure you want to delete the existing assay runs associated with these files?')) { window.location = '" + deleteURL + "' }");
        }

        sb.append("<br/><br/>");
        for (Map.Entry<String, String> entry : links.entrySet())
        {
            sb.append(PageFlowUtil.textLink(entry.getKey(), entry.getValue()));
            sb.append("&nbsp;");
        }

        return sb.toString();
    }

    public String getShortName()
    {
        return NAME;
    }

    public String getDescription()
    {
        return "mzXML files in selected folder";
    }
}
