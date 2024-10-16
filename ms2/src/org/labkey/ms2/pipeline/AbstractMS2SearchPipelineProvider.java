/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.TaskPipeline;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Common base class for pipeline providers that map to MS2 searches (XTandem, Mascot, etc)
 */
abstract public class AbstractMS2SearchPipelineProvider<FactoryType extends AbstractMS2SearchTaskFactory>
        extends AbstractMS2PipelineProvider<AbstractMS2SearchProtocolFactory>
{
    private final Class<FactoryType> _factoryClass;

    public AbstractMS2SearchPipelineProvider(String name, Module owningModule, Class<FactoryType> factoryClass)
    {
        super(name, owningModule);
        _factoryClass = factoryClass;
    }

    @Nullable
    public FactoryType findFactory()
    {
        return AbstractMS2SearchTaskFactory.findFactory(_factoryClass);
    }

    @Override
    public final void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (isEnabled() && context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            updateFilePropertiesEnabled(context, pr, directory, includeAll);
        }
    }

    public abstract void updateFilePropertiesEnabled(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll);

    @Override
    public final @Nullable HttpView getSetupWebPart(Container container)
    {
        if (isEnabled())
            return createSetupWebPart(container);
        // Hide setup UI for providers that are disabled
        return null;
    }

    /** Called if the search provider is enabled */
    @NotNull
    protected abstract HttpView createSetupWebPart(Container container);

    /** @return false if this type of search has been disabled via Spring XML config */
    protected boolean isEnabled()
    {
        FactoryType factoryType = findFactory();
        return factoryType == null || factoryType.isEnabled();
    }

    @Override
    public boolean isSearch()
    {
        return true;
    }

    @Override
    public void initSystemDirectory(Path rootDir, Path systemDir)
    {
        AbstractMS2SearchProtocolFactory factory = getProtocolFactory();
        if (factory != null && !FileUtil.hasCloudScheme(rootDir) && !FileUtil.hasCloudScheme(systemDir))
            factory.initSystemDirectory(rootDir.toFile(), systemDir.toFile());
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory(TaskPipeline pipeline)
    {
        // MS2 search providers all support only one protocol factory each.
        return getProtocolFactory();
    }

    @Override
    public AbstractMS2SearchProtocolFactory getProtocolFactory(File file)
    {
        AbstractMS2SearchProtocolFactory factory = getProtocolFactory();
        if (factory != null && factory.isProtocolTypeFile(file))
            return factory;
        return null; 
    }

    @Override
    public boolean dbExists(Container container, File sequenceRoot, String db) throws IOException
    {
        File dbFile = FileUtil.appendName(sequenceRoot, db);
        return NetworkDrive.exists(dbFile);
    }

    abstract public boolean supportsDirectories();

    abstract public boolean remembersDirectories();

    abstract public boolean hasRemoteDirectories();

    /** @return the list of subdirectories that might contain sequence DBs */
    @Nullable
    abstract public List<String> getSequenceDbPaths(File sequenceRoot);

    /** @return the list of sequence DBs in a given directory */
    @Nullable
    abstract public List<String> getSequenceDbDirList(Container container, File sequenceRoot) throws IOException;

    abstract public List<String> getTaxonomyList(Container container) throws IOException;

    /** @return enzyme name -> cut patterns
     * @param container*/
    abstract public Map<String, List<String>> getEnzymes(Container container) throws IOException;

    abstract public Map<String, String> getResidue0Mods(Container container) throws IOException;

    abstract public Map<String, String> getResidue1Mods(Container container);
}
