/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.labkey.api.data.*;
import org.labkey.api.exp.ExperimentRunFilter;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.module.DefaultModule;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.pipeline.*;
import org.labkey.ms2.protein.ProteinController;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.scoring.ScoringController;
import org.labkey.ms2.search.ProteinSearchWebPart;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: migra
 * Date: Jul 18, 2005
 * Time: 3:25:52 PM
 */
public class MS2Module extends DefaultModule implements ContainerManager.ContainerListener
{
    private static final Logger _log = Logger.getLogger(MS2Module.class);

    public static final String NAME = "MS2";

    private static MS2SearchExperimentRunFilter _ms2SearchRunFilter = new MS2SearchExperimentRunFilter("MS2 Searches", MS2Schema.GENERAL_SEARCH_EXPERIMENT_RUNS_TABLE_NAME);
    private static ExperimentRunFilter _samplePrepRunFilter = new ExperimentRunFilter("MS2 Sample Preparation", MS2Schema.SCHEMA_NAME, MS2Schema.SAMPLE_PREP_EXPERIMENT_RUNS_TABLE_NAME);

    public static final String MS2_SAMPLE_PREPARATION_RUNS_NAME = "MS2 Sample Preparation Runs";
    public static final String MS2_RUNS_ENHANCED_NAME = "MS2 Runs (Enhanced)";

    public MS2Module()
    {
        super(NAME, 2.20, "/org/labkey/ms2", true,
                new WebPartFactory("MS2 Runs"){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        return new MS2WebPart();
                    }
                },
                new WebPartFactory(MS2_RUNS_ENHANCED_NAME){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        WebPartView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), _ms2SearchRunFilter, true);
                        result.setTitle("MS2 Runs");
                        return result;
                    }
                },
                new WebPartFactory(MS2_SAMPLE_PREPARATION_RUNS_NAME){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        WebPartView result = ExperimentService.get().createExperimentRunWebPart(new ViewContext(portalCtx), _samplePrepRunFilter, true);
                        result.setTitle(MS2_SAMPLE_PREPARATION_RUNS_NAME);
                        return result;
                    }
                },
                new WebPartFactory("MS2 Statistics","right"){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        return new MS2StatsWebPart();
                    }
                },
                new WebPartFactory(ProteinSearchWebPart.NAME, "right"){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        return new ProteinSearchWebPart(!"right".equalsIgnoreCase(webPart.getLocation()));
                    }
                },
                new WebPartFactory(ProteinSearchWebPart.NAME){
                    public WebPartView getWebPartView(ViewContext portalCtx, Portal.WebPart webPart)
                    {
                        return new ProteinSearchWebPart(!"right".equalsIgnoreCase(webPart.getLocation()));
                    }
                });
        addController("MS2", MS2Controller.class);
        addController("protein", ProteinController.class);
        addController("MS2-Pipeline", PipelineController.class);
        addController("MS2-Scoring", ScoringController.class);

        MS2Schema.register();
        CustomAnnotationSchema.register();

        MS2Service.register(new MS2ServiceImpl());
    }


    @Override
    public void startup(ModuleContext context)
    {
        PipelineService service = PipelineService.get();
        service.registerPipelineProvider(new MS2PipelineProvider());
        service.registerPipelineProvider(new XTandemLocalPipelineProvider());
        service.registerPipelineProvider(new XTandemCPipelineProvider());
        service.registerPipelineProvider(new CometCPipelineProvider());
        service.registerPipelineProvider(new SequestLocalPipelineProvider());
        service.registerPipelineProvider(new MascotLocalPipelineProvider());
        service.registerPipelineProvider(new MascotCPipelineProvider());

        service.registerPipelineProvider(new InspectCPipelineProvider());
        service.registerPipelineProvider(new ProteinProphetPipelineProvider());

        ExperimentService.get().registerExperimentRunFilter(_samplePrepRunFilter);
        ExperimentService.get().registerExperimentRunFilter(_ms2SearchRunFilter);
        ExperimentService.get().registerExperimentRunFilter(new MS2SearchExperimentRunFilter("X!Tandem Searches", MS2Schema.XTANDEM_SEARCH_EXPERIMENT_RUNS_TABLE_NAME));
        ExperimentService.get().registerExperimentRunFilter(new MS2SearchExperimentRunFilter("Mascot Searches", MS2Schema.MASCOT_SEARCH_EXPERIMENT_RUNS_TABLE_NAME));
        ExperimentService.get().registerExperimentRunFilter(new MS2SearchExperimentRunFilter("Sequest Searches", MS2Schema.SEQUEST_SEARCH_EXPERIMENT_RUNS_TABLE_NAME));

        ExperimentService.get().registerExperimentDataHandler(new PepXmlExperimentDataHandler());
        ExperimentService.get().registerExperimentDataHandler(new ProteinProphetExperimentDataHandler());

        //We are the first creator of this...
        ContainerManager.addContainerListener(this);
        ModuleLoader.getInstance().registerFolderType(new MS2FolderType(this));
        super.startup(context);
    }


    @Override
    public Collection<String> getSummary(Container c)
    {
        Collection<String> list = new LinkedList<String>();
        try
        {
            long count = MS2Manager.getRunCount(c);
            if (count > 0)
                list.add("" + count + " MS2 Run" + (count > 1 ? "s" : ""));
        }
        catch (SQLException x)
        {
            list.add(x.getMessage());
        }
        return list;
    }

    //
    // ContainerListener
    //
    public void containerCreated(Container c)
    {
    }


    public void containerDeleted(Container c, User user)
    {
        MS2Manager.markAsDeleted(c, user);
    }


    public void propertyChange(PropertyChangeEvent evt)
    {
    }


    @Override
    public void afterSchemaUpdate(ModuleContext moduleContext, ViewContext viewContext)
    {
        double version = moduleContext.getInstalledVersion();
        User user = viewContext.getUser();

        Map<File, String> hashes = new HashMap<File, String>();

        if (version > 0 && version < 1.12)
        {
            TableInfo protFastaTable = ProteinManager.getTableInfoFastaLoads();
            updateHashes(protFastaTable, "FileName", "FastaId", user, hashes);
        }
        if (version > 0 && version < 1.14)
        {
            TableInfo table = ProteinManager.getTableInfoFastaFiles();
            updateHashes(table, "FileName", "FastaId", user, hashes);
        }
    }

    private void updateHashes(TableInfo table, String fileColumnName, String rowIdName, User user, Map<File, String> hashes)
    {
        // Update the checksums for our fasta files.  This is nice to do because we've changed
        // to a more efficient algorithm for computing these values which has changed the data.
        // This update will allow us to be more efficient in the future by allowing short-circuiting
        // of FASTA loads of files that have already been loaded.  However, it's okay if the updates
        // fail for some reason, since the fasta loader prevents double insertion of sequences.
        try
        {
            ResultSet rs = Table.select(table, Table.ALL_COLUMNS, null, null);
            while (rs.next())
            {
                String filename = null;
                try
                {
                    filename = rs.getString(fileColumnName);
                    if (filename != null)
                    {
                        File fastaFile = new File(filename);
                        NetworkDrive.ensureDrive(fastaFile.getAbsolutePath());
                        if (fastaFile.exists())
                        {
                            String hash = hashes.get(fastaFile);
                            if (hash == null)
                            {
                                hash = HashHelpers.hashFileContents(filename);
                                hashes.put(fastaFile, hash);
                            }

                            Map<String,String> valueMap = new HashMap<String,String>();
                            valueMap.put("FileChecksum", hash);
                            Table.update(user, table, valueMap, rs.getInt(rowIdName), null);
                        }
                    }
                }
                catch (IOException e)
                {
                    _log.error("Unable to calculate new checksum for fasta file " + filename, e);
                }
                catch (SQLException e)
                {
                    _log.error("Unable to calculate new checksum for fasta file: " + filename, e);
                }
            }
        }
        catch (SQLException e)
        {
            _log.error("Unable to update fasta file checksums.", e);
        }
    }


    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(MS2Schema.SCHEMA_NAME, ProteinManager.getSchemaName());
    }

    @Override
    public Set<DbSchema> getSchemasToTest()
    {
        return PageFlowUtil.set(MS2Manager.getSchema(), ProteinManager.getSchema());
    }


    @Override
    public Set<String> getModuleDependencies()
    {
        Set<String> result = new HashSet<String>();
        result.add("Pipeline");
        result.add("Experiment");
        return result;
    }


    @Override
    public Set<Class<? extends TestCase>> getJUnitTests()
    {
        return new HashSet<Class<? extends TestCase>>(Arrays.asList(
            org.labkey.ms2.pipeline.SequestParamsBuilder.TestCase.class,
            org.labkey.ms2.pipeline.PositiveDoubleParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.NaturalNumberParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.NaturalNumberParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.RealNumberParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.BooleanParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.PositiveIntegerParamsValidator.TestCase.class,
            org.labkey.ms2.pipeline.ListParamsValidator.TestCase.class,
            org.labkey.ms2.reader.RandomAccessMzxmlIterator.TestCase.class));
    }
}
