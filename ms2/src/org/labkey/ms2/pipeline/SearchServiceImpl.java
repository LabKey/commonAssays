/*
 * Copyright (c) 2008-2016 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.pipeline.PipelineStatusFile;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.pipeline.client.GWTSearchServiceResult;
import org.labkey.ms2.pipeline.client.SearchService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * User: Bill
 * Date: Jan 29, 2008
 * Time: 4:04:45 PM
 */
public class SearchServiceImpl extends BaseRemoteService implements SearchService
{

    private static Logger _log = Logger.getLogger(SearchServiceImpl.class);
    private GWTSearchServiceResult results= new GWTSearchServiceResult();
    private AbstractMS2SearchPipelineProvider provider;
    private AbstractMS2SearchProtocol protocol;

    public SearchServiceImpl(ViewContext context)
    {
        super(context);
    }

    public GWTSearchServiceResult getSearchServiceResult(String searchEngine, String path, String[] fileNames)
    {
        AbstractMS2PipelineProvider baseProvider = (AbstractMS2PipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        getProtocols("", baseProvider, searchEngine, path, fileNames);
        if (baseProvider != null && baseProvider.isSearch())
        {
            if (results.getSelectedProtocol() == null || results.getSelectedProtocol().equals(""))
                getSequenceDbs(results.getDefaultSequenceDb(), searchEngine, false);
            getMascotTaxonomy(searchEngine);
            getEnzymes(searchEngine);
            getResidueMods(searchEngine);
        }
        return results;
    }

    private PipeRoot getPipelineRoot()
    {
        PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(getContainer());
        if (pipeRoot == null)
        {
            throw new NotFoundException("No pipeline root configured for " + getContainer().getPath());
        }
        return pipeRoot;
    }

    private File getSequenceRoot()
    {
        PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(getContainer());
        if (pipeRoot == null)
        {
            throw new NotFoundException("No pipeline root configured for " + getContainer().getPath());
        }
        return MS2PipelineManager.getSequenceDatabaseRoot(pipeRoot.getContainer(), true);
    }

    public GWTSearchServiceResult getProtocol(String searchEngine, String protocolName, String path, String[] fileNames)
    {
        AbstractMS2PipelineProvider provider = (AbstractMS2PipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        if (provider == null)
        {
            results.setSelectedProtocol("Loading Error");
            _log.debug("Problem loading protocols: provider equals null");
            results.appendError("Problem loading protocol: provider equals null\n");
        }

        if(protocolName == null || protocolName.length() == 0)
        {
            protocolName = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(protocolName == null || protocolName.length() == 0)
                protocolName = "new";
        }
        if(protocolName.equals("new"))
        {
            results.setSelectedProtocol("");
            getMzXml(path, fileNames, false);
            return results;
        }
        PipeRoot root = getPipelineRoot();

        boolean protocolExists = false;
        AbstractMS2SearchProtocolFactory protocolFactory = provider.getProtocolFactory();
        try
        {
            if(protocol == null)
            {
                File protocolFile = protocolFactory.getParametersFile(root.resolvePath(path), protocolName, root);
                if (protocolFile != null && NetworkDrive.exists(protocolFile))
                {
                    protocolExists = true;
                    protocol = protocolFactory.loadInstance(protocolFile);

                    // Don't allow the instance file to override the protocol name.
                    protocol.setName(protocolName);
                }
                else
                {
                    protocol = protocolFactory.load(root, protocolName, false);
                }
            }
        }
        catch(IOException e)
        {
            results.setSelectedProtocol("");
            results.setProtocolDescription("");
            results.setProtocolXml("");
            PipelineService.get().rememberLastProtocolSetting(provider.getProtocolFactory(), getContainer(), getUser(), "");
            getMzXml(path, fileNames, false);
            _log.error("Could not load " + protocolName + ".", e);
        }
        if (protocol != null)
        {
            results.setSelectedProtocol(protocolName);
            if (provider.isSearch())
            {
                if (protocol.getDbNames().length == 0)
                {
                    _log.debug("Problem loading protocol: no database in protocol");
                    results.appendError("Problem loading protocol: No database in protocol");
                }

                for (String dbName : protocol.getDbNames())
                {
                    boolean dbExists;
                    try
                    {
                        dbExists = provider.dbExists(getContainer(), getSequenceRoot(), dbName);
                    }
                    catch (IOException e)
                    {
                        _log.error("Unable to get DBs for " + getSequenceRoot(), e);
                        dbExists = false;
                    }
                    if (!dbExists)
                    {
                        results.appendError("The database " + dbName + " cannot be found.");
                    }
                }
                results.setDefaultSequenceDb(StringUtils.join(protocol.getDbNames(), ";"));
            }

            PipelineService.get().rememberLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                        getUser(),"",results.getDefaultSequenceDb());
            results.setProtocolDescription(protocol.getDescription());
            results.setProtocolXml(protocol.getXml());
        }
        getMzXml(path, fileNames, protocolExists);
        return results;
    }

    public GWTSearchServiceResult getMascotTaxonomy(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading taxonomy: provider equals null\n");
            }
        }
        try
        {
            results.setMascotTaxonomyList(provider.getTaxonomyList(getContainer()));
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving taxonomy list: " + e.getMessage());
        }
        return results;
    }

    public GWTSearchServiceResult getEnzymes(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading enzymes: provider equals null\n");
            }
        }
        try
        {
            results.setEnzymeMap(provider.getEnzymes(getContainer()));
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving enzyme list: " + e.getMessage());
        }
        return results;
    }

    public GWTSearchServiceResult getResidueMods(String searchEngine)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.appendError("Problem loading residue modifications: provider equals null\n");
            }
        }
        try
        {
            results.setMod0Map(provider.getResidue0Mods(getContainer()));
            results.setMod1Map(provider.getResidue1Mods(getContainer()));
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving residue mods list: " + e.getMessage());
        }
        return results;

    }

    private void getProtocols(String defaultProtocol, AbstractMS2PipelineProvider provider, String searchEngine, String path, String[] fileNames)
    {
        ArrayList<String> protocolList = new ArrayList<>();
        if(defaultProtocol == null || defaultProtocol.length() == 0 )
        {
            if(provider == null)
            {
                provider = (AbstractMS2PipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            }
            defaultProtocol = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(defaultProtocol == null) defaultProtocol = "";
        }
        getProtocol(searchEngine, defaultProtocol, path, fileNames);

        PipeRoot root = getPipelineRoot();

        String[] protocols = provider.getProtocolFactory().getProtocolNames(root, root.resolvePath(path), false);
        for(String protName:protocols)
        {
            if(!protName.equals("default"))
                protocolList.add(protName);
        }
        results.setProtocols(protocolList);
    }

    private void getSequenceDbPaths(String searchEngine, boolean refresh)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }
        if(!provider.supportsDirectories()) return;

        List<String> sequenceDbPaths = PipelineService.get().getLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                getContainer(),getUser());
        if(sequenceDbPaths == null || sequenceDbPaths.size() == 0 || refresh)
        {
            try
            {
                File dirSequenceRoot = getSequenceRoot();
                sequenceDbPaths = provider.getSequenceDbPaths(dirSequenceRoot);
                if(sequenceDbPaths == null) throw new IOException("Fasta directory not found.");
                if(provider.remembersDirectories())
                {
                    PipelineService.get().rememberLastSequenceDbPathsSetting(provider.getProtocolFactory(),
                            getContainer(),getUser(), sequenceDbPaths);
                }
            }
            catch(IOException e)
            {
                 results.appendError("There was a problem retrieving the database list from the server:\n"
                        + e.getMessage());
            }
        }
        results.setSequenceDbPaths(sequenceDbPaths);
    }

    public GWTSearchServiceResult getSequenceDbs(String defaultDb, String searchEngine, boolean refresh)
    {
        if(defaultDb == null) defaultDb = "";
        String relativePath;
        String savedRelativePath;
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        }
        if((defaultDb.length() == 0)||(defaultDb.endsWith("/")))
        {
            String savedDefaultDb = PipelineService.get().getLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(savedDefaultDb == null ||savedDefaultDb.length() == 0)
            {
                savedRelativePath = defaultDb;
                defaultDb = "";
            }
            else
            {
                savedRelativePath = savedDefaultDb.substring(0, savedDefaultDb.lastIndexOf('/') + 1);
            }
            if(defaultDb.equals(""))
            {
                relativePath = savedRelativePath;
            }
            else
            {
                relativePath = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
            }
            if(relativePath.equals(savedRelativePath) && (savedDefaultDb != null && savedDefaultDb.length() != 0))
            {
                defaultDb = savedDefaultDb;
            }
            else
            {
                defaultDb = relativePath;
            }
        }
        else
        {
            relativePath = defaultDb.substring(0, defaultDb.lastIndexOf('/') + 1);
        }
        return getSequenceDbs(relativePath, defaultDb, searchEngine, refresh);
    }

    private GWTSearchServiceResult getSequenceDbs(String relativePath, String defaultDb, String searchEngine, boolean refresh)
    {
        List<String> sequenceDbs = null;
        String defaultDbPath;
        List<String> returnList = new ArrayList<>();
        if(defaultDb != null && defaultDb.endsWith("/"))
        {
            String savedDb =
                    PipelineService.get().getLastSequenceDbSetting(provider.getProtocolFactory(),getContainer(),getUser());
            if(savedDb != null && savedDb.length() > 0)
            {
                if(defaultDb.equals("/") && (!savedDb.contains("/") || savedDb.indexOf("/") == 0 ) )
                {
                    defaultDb = savedDb;
                }
                else if(savedDb.contains("/") && defaultDb.indexOf("/") != 0)
                {
                    String test = savedDb.replaceFirst(defaultDb, "");
                    if(!test.contains("/")) defaultDb = savedDb;
                }
            }
        }
        getSequenceDbPaths(searchEngine, refresh);

        if(relativePath.equals("/"))
        {
            defaultDbPath = getSequenceRoot().toURI().getPath();
        }
        else
        {
            defaultDbPath = getSequenceRoot().toURI().getPath() + relativePath;
        }
        URI defaultDbPathURI;
        try
        {
            if(provider.hasRemoteDirectories())
            {
                relativePath = relativePath.replaceAll(" ","%20");
                URI uriPath = new URI(relativePath);
                sequenceDbs =  provider.getSequenceDbDirList(getContainer(), new File(uriPath));
            }
            else
            {
                defaultDbPathURI = new File(defaultDbPath).toURI();
                sequenceDbs =  provider.getSequenceDbDirList(getContainer(), new File(defaultDbPathURI));
            }          
            if(sequenceDbs == null)
            {
                results.appendError("Could not find the default sequence database path : " + defaultDbPath);
                defaultDbPathURI = getSequenceRoot().toURI();
                sequenceDbs = provider.getSequenceDbDirList(getContainer(), new File(defaultDbPathURI));
            }
            else
            {
                results.setDefaultSequenceDb(defaultDb);
            }
        }
        catch(URISyntaxException e)
        {
            results.appendError("There was a problem parsing the database database path:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }
        catch(IOException e)
        {
            results.appendError("There was a problem retrieving the database list from the server:\n"
                    + e.getMessage());
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }

        if(sequenceDbs == null || sequenceDbs.size() == 0  )
        {
            sequenceDbs = new ArrayList<>();
            sequenceDbs.add("None found.");
            results.setSequenceDbs(sequenceDbs, relativePath);
            return results;
        }

        for(String db:sequenceDbs)
        {
            if(!db.endsWith("/"))
            {
                returnList.add(db);
            }
        }

        if(returnList.size() == 0  )
        {
            returnList = new ArrayList<>();
            returnList.add("None found.");
        }
        results.setSequenceDbs(returnList, relativePath);
        return results;
    }

    private void getMzXml(String path, String[] fileNames, boolean protocolExists)
    {
        if (protocol == null)
            protocolExists = false;

        PipeRoot pr;
        try
        {
            Container c = getContainer();
            pr = PipelineService.get().findPipelineRoot(c);
            if (pr == null || !pr.isValid())
                throw new IOException("Can't find root directory.");

            File dirData = pr.resolvePath(path);
            File dirAnalysis = null;
            if (protocol != null)
                dirAnalysis = protocol.getAnalysisDir(dirData, pr);

            results.setActiveJobs(false);
            results.setFileInputNames(new ArrayList<>());
            results.setFileInputStatus(new ArrayList<>());

            Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
            for (String name : fileNames)
            {
                if (name == null || StringUtils.containsAny(name, "..", "/", "\\"))
                {
                    results.appendError("Invalid file name " + name);
                }
                else
                {
                    results.getFileInputNames().add(name);
                    if (protocolExists)
                        results.getFileInputStatus().add(getInputStatus(protocol, dirData, dirAnalysis, name, true));
                }
            }
            if (protocolExists)
                results.getFileInputStatus().add(getInputStatus(protocol, dirData, dirAnalysis, null, false));
        }
        catch (IOException e)
        {
            results.appendError(e.getMessage());
        }
    }

    private String getInputStatus(AbstractMS2SearchProtocol protocol, File dirData, File dirAnalysis,
                              String fileInputName, boolean statusSingle)
    {
        File fileStatus = null;

        if (!statusSingle)
        {
            fileStatus = PipelineJob.FT_LOG.newFile(dirAnalysis,
                    protocol.getJoinedBaseName());
        }
        else if (fileInputName != null)
        {
            File fileInput = new File(dirData, fileInputName);
            FileType ft = protocol.findInputType(fileInput);
            if (ft != null)
                fileStatus = PipelineJob.FT_LOG.newFile(dirAnalysis, ft.getBaseName(fileInput));
        }

        if (fileStatus != null)
        {
            PipelineStatusFile sf = PipelineService.get().getStatusFile(fileStatus);
            if (sf == null)
                return null;

            if (sf.isActive())
                results.setActiveJobs(true);
            return sf.getStatus();
        }

        // Failed to get status.  Assume job is active, and return unknown status.
        results.setActiveJobs(true);
        return "UNKNOWN";
    }
}
