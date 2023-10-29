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

package org.labkey.microarray.controllers;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.SimpleRedirectAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.pipeline.browse.PipelinePathForm;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.URLHelper;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.UnauthorizedException;
import org.labkey.microarray.pipeline.GeneDataPipelineProvider;
import org.springframework.validation.Errors;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

public class MicroarrayController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(
        MicroarrayController.class
    );

    public MicroarrayController()
    {
        setActionResolver(_actionResolver);
    }


    /**
     * Basic approach:
     * 1. The user logs in to LabKey Server and selects the files they want to analyze, and clicks on a new "Launch in Analyst" button.
     * 2. LabKey Server will then package up the files as a ZIP or just copy them into a new directory. In either case, the file(s) will end up on a network share that LabKey Server can write to, and Analyst can read from.
     * 3. LabKey Server will then cause the user's browser to navigate to a URL of the general form that you described. It will include a GET parameter that gives the path to the ZIP file or directory.
     * 4. The Analyst plugin takes over from here.
     */
    @RequiresPermission(ReadPermission.class)
    public class GeneDataAnalysisAction extends SimpleRedirectAction<PipelinePathForm>
    {
        @Override
        public @Nullable URLHelper getRedirectURL(PipelinePathForm form) throws URISyntaxException, IOException
        {
            String baseURL = GeneDataPipelineProvider.getGeneDataBaseURL();
            File root = GeneDataPipelineProvider.getGeneDataFileRoot();

            if (baseURL == null || root == null)
            {
                throw new NotFoundException("GeneData URL or file root not configured");
            }

            if (getUser().isGuest())
            {
                throw new UnauthorizedException();
            }

            String simpleDirName = getUser().getDisplayName(getUser()) + DateUtil.formatDateTime(new Date(), "yyyy-MM-dd-HH-mm");
            File analysisDir = new File(root, simpleDirName);
            int suffix = 1;
            while (analysisDir.exists())
            {
                analysisDir = new File(root, simpleDirName + "-" + (suffix++));
            }
            FileUtil.mkdir(analysisDir);

            // Copy over all of the files
            List<File> files = form.getValidatedFiles(getContainer());
            for (File selectedFile : files)
            {
                FileUtil.copyFile(selectedFile, new File(analysisDir, selectedFile.getName()));
            }

            return new URLHelper(baseURL + analysisDir.getAbsolutePath());
        }
    }

}
