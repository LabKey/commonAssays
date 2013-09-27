/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
package org.labkey.ms2.pipeline.sequest;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobService;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineJob;
import org.labkey.ms2.pipeline.AbstractMS2SearchTaskFactory;
import org.labkey.ms2.pipeline.TPPTask;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Dec 23, 2011
 */
public abstract class AbstractSequestSearchTaskFactory<Type extends AbstractMS2SearchTaskFactory<Type>> extends AbstractMS2SearchTaskFactory<Type> implements InitializingBean
{
    private File _sequestInstallDir;
    private File _indexRootDir;
    private List<String> _sequestOptions = new ArrayList<>();

    protected AbstractSequestSearchTaskFactory(Class namespaceClass)
    {
        super(namespaceClass);
    }

    public boolean isJobComplete(PipelineJob job)
    {
        AbstractMS2SearchPipelineJob support = (AbstractMS2SearchPipelineJob) job;
        String baseName = support.getBaseName();
        String baseNameJoined = support.getJoinedBaseName();
        File dirAnalysis = support.getAnalysisDirectory();

        // Fraction roll-up, completely analyzed sample pepXML, or the raw pepXML exist
        return NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseNameJoined)) ||
               NetworkDrive.exists(TPPTask.getPepXMLFile(dirAnalysis, baseName)) ||
               NetworkDrive.exists(AbstractMS2SearchPipelineJob.getPepXMLConvertFile(dirAnalysis, baseName));
    }

    public String getGroupParameterName()
    {
        return "sequest";
    }

    public String getSequestInstallDir()
    {
        return _sequestInstallDir == null ? null : _sequestInstallDir.getAbsolutePath();
    }

    public void setSequestInstallDir(String sequestInstallDir)
    {
        if (sequestInstallDir != null)
        {
            _sequestInstallDir = new File(sequestInstallDir);
        }
        else
        {
            _sequestInstallDir = null;
        }
    }

    public List<String> getSequestOptions()
    {
        return _sequestOptions;
    }

    public void setSequestOptions(List<String> sequestOptions)
    {
        _sequestOptions = sequestOptions;
    }

    public String getIndexRootDir()
    {
        return _indexRootDir == null ? null : _indexRootDir.getAbsolutePath();
    }

    public void setIndexRootDir(String indexRootDir)
    {
        if (indexRootDir != null)
        {
            _indexRootDir = new File(indexRootDir);
        }
        else
        {
            _indexRootDir = null;
        }
    }

    public void afterPropertiesSet() throws Exception
    {
        PipelineJobService.RemoteServerProperties props = PipelineJobService.get().getRemoteServerProperties();

        // Check if we're on the web server and Sequest is going to run there, or that we're a remote server and Sequest is set to run there
        if ((PipelineJobService.get().getLocationType() == PipelineJobService.LocationType.WebServer && (getLocation() == null || WEBSERVER.equals(getLocation()))) ||
            (PipelineJobService.get().getLocationType() == PipelineJobService.LocationType.RemoteServer && props != null && props.getLocation() != null && props.getLocation().equals(getLocation())))
        {
            if (_indexRootDir != null)
            {
                NetworkDrive.exists(_indexRootDir);
                if (!_indexRootDir.isDirectory())
                {
                    throw new IllegalArgumentException("No such index root dir: " + _indexRootDir);
                }
            }

            if (_sequestInstallDir != null)
            {
                NetworkDrive.exists(_sequestInstallDir);
                if (!_sequestInstallDir.isDirectory())
                {
//                    throw new IllegalArgumentException("No such Sequest install dir: " + _sequestInstallDir);
                }
            }
        }
    }
}
