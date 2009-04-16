/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.security.ACL;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.common.util.Pair;

import java.io.File;
import java.util.*;

/**
 * User: phussey
 * Date: Sep 23, 2007
 * Time: 1:09:11 PM
 */
public class XarAssayDataCollector extends PipelineDataCollector<XarAssayForm>
{
    public static String NAME = "mzXMLFiles";

    public String getHTML(XarAssayForm form) throws ExperimentException
    {
        StringBuilder sb = new StringBuilder(super.getHTML(form));

        sb.append("<br/>");

        Container c = form.getContainer();

        if (c.hasPermission(form.getUser(), ACL.PERM_DELETE))
        {
            Pair<Integer, Integer> status = getExistingAnnotationStatus(form);
            int totalFiles = status.getKey().intValue();
            int annotatedFiles = status.getValue().intValue();

            // If we found some, prompt the user to delete them
            if (annotatedFiles > 0)
            {
                ActionURL deleteURL = new ActionURL(XarAssayUploadAction.class, c);
                deleteURL.addParameter("rowId", form.getProtocol().getRowId());
                deleteURL.addParameter("uploadStep", XarAssayUploadAction.DeleteAssaysStepHandler.NAME);

                sb.append("<div id=\"deleteRunsSpan\">");
                if (annotatedFiles == totalFiles)
                {
                    if (totalFiles == 1)
                    {
                        sb.append("The selected file has already been annotated. You must delete the existing run to re-annotate it.");
                    }
                    else
                    {
                        sb.append("The selected files have already been annotated. You must delete the existing runs to re-annotate them.");
                    }
                }
                else
                {
                    sb.append("Some of the selected files have already been annotated. You must delete the existing runs to re-annotate them.");
                }
                sb.append(" [<a onclick=\"");
                //noinspection StringConcatenationInsideStringBufferAppend
                sb.append("if (window.confirm('Are you sure you want to delete the existing assay runs associated with these files?'))" +
                            "{" +
                                "Ext.Ajax.request(" +
                                "{" +
                                    "url: '" + PageFlowUtil.filter(deleteURL )+ "', " +
                                    "success: function() { document.getElementById('deleteRunsSpan').innerHTML = 'Runs deleted successfully.' }," +
                                    "failure: function() { alert('failure'); }" +
                                "});" +
                            "}" +
                            "return false;");
                sb.append("\">");
                sb.append("delete assay runs</a>]</div>");
            }
        }

        ActionURL showSamplesURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowSampleSetURL(ExperimentService.get().ensureActiveSampleSet(c));
        sb.append(PageFlowUtil.textLink("edit samples", showSamplesURL));

        return sb.toString();
    }

    /** @return the total number of files for this set, and the number that have already been annotated */
    public Pair<Integer, Integer> getExistingAnnotationStatus(XarAssayForm form)
    {
        int total = 0;
        int annotated = 0;
        // Look for files that have already been annotated
        List<Map<String,File>> allFiles = getFileCollection(form);
        for (Map<String, File> fileSet : allFiles)
        {
            for (File file : fileSet.values())
            {
                total++;
                ExpRun run = ExperimentService.get().getCreatingRun(file, form.getContainer());
                if (run != null)
                {
                    annotated++;
                }
            }
        }
        return new Pair<Integer, Integer>(total, annotated);
    }

    @Override
    public void uploadComplete(XarAssayForm context)
    {
        if (context.isFractions())
        {
            super.getFileCollection(context).clear();
        }
        else
        {
            super.uploadComplete(context);
        }
    }

    @Override
    public List<Map<String, File>> getFileCollection(XarAssayForm context)
    {
        List<Map<String, File>> files = super.getFileCollection(context);
        if (context.isFractions())
        {
            Map<String, File> result = new LinkedHashMap<String, File>();
            for (Map<String, File> batch : files)
            {
                result.putAll(batch);
            }
            return Collections.singletonList(result);
        }

        return files;
    }

    public String getShortName()
    {
        return NAME;
    }
}
