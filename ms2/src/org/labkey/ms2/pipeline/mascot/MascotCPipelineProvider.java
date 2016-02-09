/*
 * Copyright (c) 2006-2016 LabKey Corporation
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
package org.labkey.ms2.pipeline.mascot;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineActionConfig;
import org.labkey.api.pipeline.PipelineDirectory;
import org.labkey.api.pipeline.PipelineValidationException;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.pipeline.AbstractMS2SearchPipelineProvider;
import org.labkey.ms2.pipeline.AbstractMS2SearchProtocolFactory;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.ms2.pipeline.PipelineController;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MascotCPipelineProvider class
 * <p/>
 * Created: Nov 1, 2005
 *
 * @author bmaclean
 */
public class MascotCPipelineProvider extends AbstractMS2SearchPipelineProvider
{
    public static String name = "Mascot";
    private static final String ACTION_LABEL = "Mascot Peptide Search";

    public MascotCPipelineProvider(Module owningModule)
    {
        super(name, owningModule);
    }

    public boolean isStatusViewableFile(Container container, String name, String basename)
    {
        if ("mascot.xml".equals(name))
            return true;

        return super.isStatusViewableFile(container, name, basename);
    }

    public void updateFileProperties(ViewContext context, PipeRoot pr, PipelineDirectory directory, boolean includeAll)
    {
        if (!MascotConfig.findMascotConfig(context.getContainer()).hasMascotServer())
            return;
        if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
        {
            return;
        }

        String actionId = createActionId(PipelineController.SearchMascotAction.class, ACTION_LABEL);
        addAction(actionId, PipelineController.SearchMascotAction.class, ACTION_LABEL,
                directory, directory.listFiles(MS2PipelineManager.getAnalyzeFilter()), true, true, includeAll);
    }

    @Override
    public List<PipelineActionConfig> getDefaultActionConfigSkipModuleEnabledCheck(Container container)
    {
        if (MascotConfig.findMascotConfig(container).hasMascotServer())
        {
            String actionId = createActionId(PipelineController.SearchMascotAction.class, ACTION_LABEL);
            return Collections.singletonList(new PipelineActionConfig(actionId, PipelineActionConfig.displayState.toolbar, ACTION_LABEL, true));
        }
        return super.getDefaultActionConfigSkipModuleEnabledCheck(container);
    }

    public HttpView getSetupWebPart(Container container)
    {
        return new SetupWebPart();
    }

    private static class SetupWebPart extends WebPartView
    {
        public SetupWebPart()
        {
            super(FrameType.DIV);
        }

        @Override
        protected void renderView(Object model, PrintWriter out) throws Exception
        {
            ViewContext context = getViewContext();
            if (!context.getContainer().hasPermission(context.getUser(), InsertPermission.class))
                return;
            StringBuilder html = new StringBuilder();
            html.append("<table><tr><td style=\"font-weight:bold;\">Mascot specific settings:</td></tr>");
            ActionURL setDefaultsURL = new ActionURL(PipelineController.SetMascotDefaultsAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(setDefaultsURL.getLocalURIString()).append("\">Set defaults</a>")
                    .append(" - Specify the default XML parameters file for Mascot.</td></tr>");
            ActionURL configMascotURL = new ActionURL(MS2Controller.MascotConfigAction.class, context.getContainer());
            html.append("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;")
                    .append("<a href=\"").append(configMascotURL.getLocalURIString()).append("\">Configure Mascot Server</a>")
                    .append(" - Specify connection information for the Mascot Server.</td></tr>");
            html.append("</table>");
            out.write(html.toString());
        }
    }

    public AbstractMS2SearchProtocolFactory getProtocolFactory()
    {
        return MascotSearchProtocolFactory.get();
    }

    public List<String> getSequenceDbPaths(File sequenceRoot) throws IOException
    {
        return null;//No directories for Mascot databases.
    }

    @Override
    public boolean dbExists(Container container, File sequenceRoot, String db) throws IOException
    {
        return new CaseInsensitiveHashSet(getSequenceDbDirList(container, sequenceRoot)).contains(db);
    }

    public List<String> getSequenceDbDirList(Container container, File sequenceRoot) throws IOException
    {
        MascotConfig config = ensureMascotConfig(container);

        MascotClientImpl mascotClient = new MascotClientImpl(config.getMascotServer(), null);
        mascotClient.setProxyURL(config.getMascotHTTPProxy());
        List<String> sequenceDBs = mascotClient.getSequenceDbList();

        if (0 == sequenceDBs.size())
        {
            // TODO: Would be nice if the Mascot client just threw its own connectivity exception.
            String connectivityResult = mascotClient.testConnectivity(false);
            if (!"".equals(connectivityResult))
                throw new IOException(connectivityResult);
        }
        return sequenceDBs;
    }

    public List<String> getTaxonomyList(Container container) throws IOException
    {
        MascotConfig config = ensureMascotConfig(container);

        MascotClientImpl mascotClient = new MascotClientImpl(config.getMascotServer(), null);
        mascotClient.setProxyURL(config.getMascotHTTPProxy());
        List<String> taxonomy = mascotClient.getTaxonomyList();

        if (0 == taxonomy.size())
        {
            // TODO: Would be nice if the Mascot client just threw its own connectivity exception.
            String connectivityResult = mascotClient.testConnectivity(false);
            if (!"".equals(connectivityResult))
                throw new IOException(connectivityResult);
        }
        return taxonomy;
//        ArrayList mock = new ArrayList();
//        mock.add("All entries");
//        mock.add(". . Archaea (Archaeobacteria)");
//        mock.add(". . Eukaryota (eucaryotes)");
//        mock.add(". . . . Alveolata (alveolates)");
//        mock.add(". . . . . . Plasmodium falciparum (malaria parasite)");
//        mock.add(". . . . . . Other Alveolata");
//        mock.add(". . . . Metazoa (Animals)");
//        mock.add(". . . . . . Caenorhabditis elegans");
//        mock.add(". . . . . . Drosophila (fruit flies)");
//        mock.add(". . . . . . Chordata (vertebrates and relatives)");
//        mock.add(". . . . . . . . bony vertebrates");
//        mock.add(". . . . . . . . . . lobe-finned fish and tetrapod clade");
//        mock.add(". . . . . . . . . . . . Mammalia (mammals)");
//        mock.add(". . . . . . . . . . . . . . Primates");
//        mock.add(". . . . . . . . . . . . . . . . Homo sapiens (human)");
//        mock.add(". . . . . . . . . . . . . . . . Other primates");
//        mock.add(". . . . . . . . . . . . . . Rodentia (Rodents)");
//        mock.add(". . . . . . . . . . . . . . . . Mus.");
//        mock.add(". . . . . . . . . . . . . . . . . . Mus musculus (house mouse)");
//        mock.add(". . . . . . . . . . . . . . . . Rattus");
//        mock.add(". . . . . . . . . . . . . . . . Other rodentia");
//        mock.add(". . . . . . . . . . . . . . Other mammalia");
//        mock.add(". . . . . . . . . . . . Xenopus laevis (African clawed frog)");
//        mock.add(". . . . . . . . . . . . Other lobe-finned fish and tetrapod clade");
//        mock.add(". . . . . . . . . . Actinopterygii (ray-finned fishes)");
//        mock.add(". . . . . . . . . . . . Fugu rubripes (Japanese Pufferfish)");
//        mock.add(". . . . . . . . . . . . Danio rerio (zebra fish)");
//        mock.add(". . . . . . . . . . . . Other Actinopterygii");
//        mock.add(". . . . . . . . . . Other bony vertebrates");
//        mock.add(". . . . . . . . Other Chordata");
//        mock.add(". . . . . . Other Metazoa");
//        mock.add(". . . . Dictyostelium discoideum");
//        mock.add(". . . . Fungi");
//        mock.add(". . . . . . Saccharomyces Cerevisiae (baker's yeast)");
//        mock.add(". . . . . . Schizosaccharomyces pombe (fission yeast)");
//        mock.add(". . . . . . Pneumocystis carinii");
//        mock.add(". . . . . . Other Fungi");
//        mock.add(". . . . Viridiplantae (Green Plants)");
//        mock.add(". . . . . . Arabidopsis thaliana (thale cress)");
//        mock.add(". . . . . . Oryza sativa (rice)");
//        mock.add(". . . . . . Other green plants");
//        mock.add(". . . . Other Eukaryota");
//        mock.add(". . Bacteria (Eubacteria)");
//        mock.add(". . . . Proteobacteria (purple bacteria)");
//        mock.add(". . . . . . Escherichia coli");
//        mock.add(". . . . . . Campylobacter jejuni");
//        mock.add(". . . . . . Other Proteobacteria");
//        mock.add(". . . . Firmicutes (gram-positive bacteria)");
//        mock.add(". . . . . . Mycoplasma");
//        mock.add(". . . . . . Bacillus subtilis");
//        mock.add(". . . . . . Streptococcus Pneumoniae");
//        mock.add(". . . . . . Streptomyces coelicolor");
//        mock.add(". . . . . . Other Firmicutes");
//        mock.add(". . . . Other Bacteria");
//        mock.add(". . Viruses");
//        mock.add(". . . . Hepatitis C virus");
//        mock.add(". . . . Other viruses");
//        mock.add(". . Other (includes plasmids and artificial sequences)");
//        mock.add(". . unclassified");
//        mock.add(". . Species information unavailable");
//        return mock;
    }

    public Map<String, List<String>> getEnzymes(Container container) throws IOException
    {
        MascotConfig config = ensureMascotConfig(container);

        MascotClientImpl mascotClient = new MascotClientImpl(config.getMascotServer(), null);
        mascotClient.setProxyURL(config.getMascotHTTPProxy());
        Map<String, List<String>> enzymes = mascotClient.getEnzymeMap();

        if (0 == enzymes.size())
        {
            throw new IOException("Could not find any enzymes, perhaps labkeydbmgmt.pl is out of date?");
        }
        return enzymes;
    }

    @NotNull
    private MascotConfig ensureMascotConfig(Container container) throws IOException
    {
        MascotConfig config = MascotConfig.findMascotConfig(container);
        if (!config.hasMascotServer())
            throw new IOException("Mascot Server has not been configured.");
        return config;
    }

    public Map<String, String> getResidue0Mods(Container container) throws IOException
    {
        MascotConfig config = ensureMascotConfig(container);

        MascotClientImpl mascotClient = new MascotClientImpl(config.getMascotServer(), null);
        mascotClient.setProxyURL(config.getMascotHTTPProxy());
        Map<String,String> mods = mascotClient.getResidueModsMap();

        if (0 == mods.size())
        {
            String connectivityResult = mascotClient.testConnectivity(false);
            if (!"".equals(connectivityResult))
                throw new IOException(connectivityResult);
        }
        return mods;
    }

    public Map<String, String> getResidue1Mods(Container container) throws IOException
    {
        return null;  //no difference between static and dynamic mods in mascot 
    }

    public String getHelpTopic()
    {
        return "pipelineMascot";
    }

    public boolean supportsDirectories()
    {
        return false;
    }

    public boolean remembersDirectories()
    {
        return false;
    }

    public boolean hasRemoteDirectories()
    {
        return false;
    }

    public void ensureEnabled(Container container) throws PipelineValidationException
    {
        MascotConfig config = MascotConfig.findMascotConfig(container);
        String mascotServer = config.getMascotServer();
        if ((!config.hasMascotServer() || 0==mascotServer.length()))
            throw new PipelineValidationException("Mascot server has not been specified in site customization.");
    }
}
