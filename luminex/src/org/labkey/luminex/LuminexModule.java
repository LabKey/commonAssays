/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.util.ContextListener;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.StartupListener;
import org.labkey.api.view.WebPartFactory;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LuminexModule extends DefaultModule
{
    public String getName()
    {
        return "Luminex";
    }

    public double getVersion()
    {
        return 11.254;
    }

    protected void init()
    {
        addController("luminex", LuminexController.class);
    }

    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    public boolean hasScripts()
    {
        return true;
    }

    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    public void startup(ModuleContext moduleContext)
    {
        AssayService.get().registerAssayProvider(new LuminexAssayProvider());
        ExperimentService.get().registerExperimentDataHandler(new LuminexDataHandler());
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return Collections.singleton("luminex");
    }

    @Override
    @NotNull
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(LuminexSchema.getSchema());
    }

    @NotNull
    @Override
    public Set<Class> getJUnitTests()
    {
        return PageFlowUtil.<Class>set(LuminexDataHandler.TestCase.class, LuminexExcelParser.TestCase.class);
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new LuminexUpgradeCode();
    }

    public static class LuminexUpgradeCode implements UpgradeCode
    {
        public static final double ADD_RESULTS_DOMAIN_UPGRADE = 11.11;

        /** called at 11.10->11.11 */
        @SuppressWarnings({"UnusedDeclaration"})
        public void addResultsDomain(final ModuleContext moduleContext)
        {
            // This needs to happen later, after all of the AssayProviders have been registered
            ContextListener.addStartupListener(new StartupListener()
            {
                @Override
                public void moduleStartupComplete(ServletContext servletContext)
                {
                    AssayService.get().upgradeAssayDefinitions(moduleContext.getUpgradeUser(), ADD_RESULTS_DOMAIN_UPGRADE);
                }
            });
        }
    }
}