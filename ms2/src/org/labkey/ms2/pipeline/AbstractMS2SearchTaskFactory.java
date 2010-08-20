/*
 * Copyright (c) 2008-2009 LabKey Corporation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.AbstractTaskFactory;
import org.labkey.api.pipeline.AbstractTaskFactorySettings;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * <code>AbstractMS2SearchTaskFactory</code>
 */
abstract public class AbstractMS2SearchTaskFactory<FactoryType extends AbstractMS2SearchTaskFactory<FactoryType>> extends AbstractTaskFactory<AbstractTaskFactorySettings, FactoryType>
{
    private List<FileType> _inputTypes;

    protected AbstractMS2SearchTaskFactory(Class namespaceClass)
    {
        super(namespaceClass);
    }

    public FactoryType cloneAndConfigure(AbstractTaskFactorySettings settings) throws CloneNotSupportedException
    {
        FactoryType result = super.cloneAndConfigure(settings);
        
        if (_inputTypes != null)
        {
            result.setInputTypes(_inputTypes);
        }
        
        return result;
    }

    public void setInputTypes(List<FileType> inputTypes)
    {
        _inputTypes = inputTypes;
    }

    public List<FileType> getInputTypes()
    {
        if (_inputTypes == null)
        {
            return Collections.singletonList(AbstractMS2SearchProtocol.FT_MZXML);
        }
        return _inputTypes;
    }

    public String getStatusName()
    {
        return "SEARCH";
    }

    public File findInputFile(File dataDirectory, String baseName) throws PipelineJobException
    {
        for (FileType fileType : getInputTypes())
        {
            File f = fileType.newFile(dataDirectory, baseName);
            if (NetworkDrive.exists(f))
            {
                return f;
            }
        }
        throw new PipelineJobException("Could not find a file in '" + dataDirectory + "' that matches input types: " + getInputTypes());
    }
}
