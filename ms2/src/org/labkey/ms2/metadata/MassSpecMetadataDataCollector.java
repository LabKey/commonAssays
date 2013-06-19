/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms2.metadata;

import org.labkey.api.data.Container;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExperimentUrls;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.study.assay.PipelineDataCollector;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.HttpView;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: phussey
 * Date: Sep 23, 2007
 * Time: 1:09:11 PM
 */
public class MassSpecMetadataDataCollector extends PipelineDataCollector<MassSpecMetadataAssayForm>
{

    @Override
    protected Map<String, File> getCurrentFilesForDisplay(MassSpecMetadataAssayForm context)
    {
        if (!context.isFractions())
        {
            return super.getCurrentFilesForDisplay(context);
        }
        else
        {
            Map<String, File> result = new HashMap<>();
            for (Map<String, File> files : getFileQueue(context))
            {
                result.putAll(files);
            }
            return result;
        }
    }

    @Override
    protected int getAdditionalFileSetCount(MassSpecMetadataAssayForm context)
    {
        if (context.isFractions())
        {
            return 0;
        }
        return super.getAdditionalFileSetCount(context);
    }

    public HttpView getView(MassSpecMetadataAssayForm form) throws ExperimentException
    {
        StringBuilder sb = new StringBuilder(super.getHTML(form));

        sb.append("<br/>");

        Container c = form.getContainer();

        if (c.hasPermission(form.getUser(), DeletePermission.class))
        {
            Pair<Integer, Integer> status = getExistingAnnotationStatus(form);
            int totalFiles = status.getKey().intValue();
            int annotatedFiles = status.getValue().intValue();

            // If we found some, prompt the user to delete them
            if (annotatedFiles > 0)
            {
                ActionURL deleteURL = new ActionURL(MassSpecMetadataUploadAction.class, c);
                deleteURL.addParameter("rowId", form.getProtocol().getRowId());
                deleteURL.addParameter("uploadStep", MassSpecMetadataUploadAction.DeleteAssaysStepHandler.NAME);

                sb.append("<div id=\"deleteRunsSpan\"><span class=\"labkey-error\">");
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
                sb.append("</span>");
                //noinspection StringConcatenationInsideStringBufferAppend
                String click = "if (window.confirm('Are you sure you want to delete the existing assay runs associated with these files?'))" +
                        "{" +
                        "Ext.Ajax.request(" +
                        "{" +
                        "url: '" + PageFlowUtil.filter(deleteURL) + "', " +
                        "success: function() { document.getElementById('deleteRunsSpan').innerHTML = 'Runs deleted successfully.' }," +
                        "failure: function() { alert('failure'); }" +
                        "});" +
                        "}" +
                        "return false;";
                sb.append(PageFlowUtil.textLink("delete assay runs", "", click, ""));
                sb.append("</div>");
            }
        }

        ActionURL showSamplesURL = PageFlowUtil.urlProvider(ExperimentUrls.class).getShowSampleSetURL(ExperimentService.get().ensureActiveSampleSet(c));
        sb.append(PageFlowUtil.textLink("edit samples", showSamplesURL));

        return new HtmlView(sb.toString());
    }

    /** @return the total number of files for this set, and the number that have already been annotated */
    public Pair<Integer, Integer> getExistingAnnotationStatus(MassSpecMetadataAssayForm form)
    {
        int annotated = 0;
        // Look for files that have already been annotated
        Collection<File> files;
        if (form.isFractions())
        {
            files = form.getAllFiles();
        }
        else
        {
            List<Map<String,File>> fileCollection = getFileQueue(form);
            if (fileCollection.isEmpty())
            {
                return new Pair<>(0, 0);
            }
            files = fileCollection.get(0).values();
        }
        for (File file : files)
        {
            ExpRun run = ExperimentService.get().getCreatingRun(file, form.getContainer());
            if (run != null)
            {
                annotated++;
            }
        }
        return new Pair<>(files.size(), annotated);
    }

    public AdditionalUploadType getAdditionalUploadType(MassSpecMetadataAssayForm context)
    {
        if (!context.isFractions() && getFileQueue(context).size() > 1)
        {
            return AdditionalUploadType.AlreadyUploaded;
        }
        return AdditionalUploadType.Disallowed;
    }
}
