/*
 * Copyright (c) 2010-2016 LabKey Corporation
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
package org.labkey.microarray.pipeline;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedActionSet;
import org.labkey.api.pipeline.WorkDirectoryTask;
import org.labkey.api.pipeline.file.AbstractFileAnalysisJob;
import org.labkey.api.study.assay.DefaultAssayRunCreator;
import org.labkey.api.util.FileType;
import org.labkey.microarray.MicroarrayModule;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * User: jeckels
 * Date: Dec 30, 2010
 */
public class MageMLDataCreatorTask extends WorkDirectoryTask<MageMLDataCreatorTask.Factory>
{
    public MageMLDataCreatorTask(Factory factory, PipelineJob job)
    {
        super(factory, job);
    }

    @Override
    public AbstractFileAnalysisJob getJob()
    {
        return (AbstractFileAnalysisJob)super.getJob();
    }

    @NotNull
    @Override
    public RecordedActionSet run() throws PipelineJobException
    {
        File[] mageFiles = getJob().getAnalysisDirectory().listFiles(ArrayPipelineManager.getMageFileFilter());

        for (File mage : mageFiles)
        {
            ExpData data = DefaultAssayRunCreator.createData(getJob().getContainer(), mage, mage.getName(), MicroarrayModule.MAGE_ML_INPUT_TYPE, true);
            data.save(getJob().getUser());
        }

        return new RecordedActionSet();
    }

    public static class Factory extends AbstractTaskFactory<AbstractTaskFactorySettings, Factory>
    {
        public Factory()
        {
            super(MageMLDataCreatorTask.class);
            setJoin(true);
        }

        @Override
        public List<String> getProtocolActionNames()
        {
            return Collections.singletonList("MageML Data Creator");
        }

        @Override
        public String getStatusName()
        {
            return "MAGEML DATA CREATOR";
        }

        @Override
        public boolean isJobComplete(PipelineJob job)
        {
            return false;
        }

        public Factory(String name)
        {
            super(FeatureExtractorTask.class, name);
            setJoin(true);
        }

        public PipelineJob.Task createTask(PipelineJob job)
        {
            return new MageMLDataCreatorTask(this, job);
        }

        public List<FileType> getInputTypes()
        {
            return Collections.singletonList(MicroarrayModule.MAGE_ML_INPUT_TYPE.getFileType());
        }
    }

}
