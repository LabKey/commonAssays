/*
 * Copyright (c) 2008-2010 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.pipeline.*;
import org.labkey.api.pipeline.file.AbstractFileAnalysisProtocol;
import org.labkey.api.util.FileType;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.URIUtil;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.NotFoundException;
import org.labkey.ms2.pipeline.client.GWTSearchServiceResult;
import org.labkey.ms2.pipeline.client.SearchService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
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
        provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        getProtocols("", searchEngine, path, fileNames);
        if(results.getSelectedProtocol() == null || results.getSelectedProtocol().equals("") )
            getSequenceDbs(results.getDefaultSequenceDb(), searchEngine, false);
        getMascotTaxonomy(searchEngine);
        getEnzymes(searchEngine);
        getResidueMods(searchEngine);
        return results;
    }

    private URI getPipelineRootURI()
    {
        PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(getContainer());
        if (pipeRoot == null)
        {
            throw new NotFoundException("No pipeline root configurd for " + getContainer().getPath());
        }
        return pipeRoot.getRootPath().toURI();
    }

    private URI getSequenceRootURI()
    {
        PipeRoot pipeRoot = PipelineService.get().findPipelineRoot(getContainer());
        if (pipeRoot == null)
        {
            throw new NotFoundException("No pipeline root configurd for " + getContainer().getPath());
        }
        return MS2PipelineManager.getSequenceDatabaseRoot(pipeRoot.getContainer());
    }

    public GWTSearchServiceResult getProtocol(String searchEngine, String protocolName, String path, String[] fileNames)
    {
        if(provider == null)
        {
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            if (provider == null)
            {
                results.setSelectedProtocol("Loading Error");
                _log.debug("Problem loading protocols: provider equals null");
                results.appendError("Problem loading protocol: provider equals null\n");
            }
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
            getMzXml(path, fileNames, searchEngine, false);
            return results;
        }
        URI uriRoot = getPipelineRootURI();

        boolean protocolExists = false;
        AbstractMS2SearchProtocolFactory protocolFactory = provider.getProtocolFactory();
        try
        {
            if(protocol == null)
            {
                File protocolFile = protocolFactory.getParametersFile(new File(URIUtil.resolve(uriRoot, path)), protocolName);
                if (NetworkDrive.exists(protocolFile))
                {
                    protocolExists = true;
                    protocol = protocolFactory.loadInstance(protocolFile);

                    // Don't allow the instance file to override the protocol name.
                    protocol.setName(protocolName);
                }
                else
                {
                    protocol = protocolFactory.load(uriRoot, protocolName);
                }
            }
        }
        catch(IOException e)
        {
            results.setSelectedProtocol("");
            results.setProtocolDescription("");
            results.setProtocolXml("");
            PipelineService.get().rememberLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                getUser(),"");
            getMzXml(path, fileNames, searchEngine, false);
            _log.error("Could not find " + protocolName + ".");
        }
        if (protocol != null)
        {
            results.setSelectedProtocol(protocolName);
            if (protocol.getDbNames().length > 0)
            {
                results.setDefaultSequenceDb(protocol.getDbNames()[0]);
                if(!provider.dbExists(getSequenceRootURI(), protocol.getDbNames()[0]))
                    results.appendError("The database " + protocol.getDbNames()[0] + " cannot be found.");
            }
            else
            {
                _log.debug("Problem loading protocol: no database in protocol");
                results.appendError("Problem loading protocol: No database in protocol");
            }

            PipelineService.get().rememberLastSequenceDbSetting(provider.getProtocolFactory(), getContainer(),
                        getUser(),"",results.getDefaultSequenceDb());
            results.setProtocolDescription(protocol.getDescription());
            results.setProtocolXml(protocol.getXml());
        }
        getMzXml(path, fileNames, searchEngine, protocolExists);
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
            results.setMascotTaxonomyList(provider.getTaxonomyList());
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
            results.setEnzymeMap(provider.getEnzymes());
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
            results.setMod0Map(provider.getResidue0Mods());
            results.setMod1Map(provider.getResidue1Mods());
        }
        catch(IOException e)
        {
            results.appendError("Trouble retrieving residue mods list: " + e.getMessage());
        }
        return results;

    }

    private void getProtocols(String defaultProtocol, String searchEngine, String path, String[] fileNames)
    {
        ArrayList<String> protocolList = new ArrayList<String>();
        if(defaultProtocol == null || defaultProtocol.length() == 0 )
        {
            if(provider == null)
            {
                provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
            }
            defaultProtocol = PipelineService.get().getLastProtocolSetting(provider.getProtocolFactory(), getContainer(),
                    getUser());
            if(defaultProtocol == null) defaultProtocol = "";
        }
        getProtocol(searchEngine, defaultProtocol, path, fileNames);

        URI dirRootURI = getPipelineRootURI();

        String[] protocols = provider.getProtocolFactory().getProtocolNames(dirRootURI, new File(URIUtil.resolve(dirRootURI, path)));
        for(String protName:protocols)
        {
            if(!protName.equals("default"))
                protocolList.add(protName);
        }
        results.setProtocols(protocolList);
    }

    public void getSequenceDbPaths(String searchEngine)
    {
        getSequenceDbPaths(searchEngine, false);
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
                URI dirSequenceRootURI = getSequenceRootURI();
                sequenceDbPaths =  provider.getSequenceDbPaths(dirSequenceRootURI);
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
        ArrayList<String> returnList = new ArrayList<String>();
        if(defaultDb != null && defaultDb.endsWith("/"))
        {
            String savedDb =
                    PipelineService.get().getLastSequenceDbSetting(provider.getProtocolFactory(),getContainer(),getUser());
            if(savedDb != null && savedDb.length() > 0)
            {
                if(defaultDb.equals("/") && (savedDb.indexOf("/") == -1 || savedDb.indexOf("/") == 0 ) )
                {
                    defaultDb = savedDb;
                }
                else if(savedDb.indexOf("/") != -1 && defaultDb.indexOf("/") != 0)
                {
                    String test = savedDb.replaceFirst(defaultDb, "");
                    if(test.indexOf("/") == -1) defaultDb = savedDb;
                }
            }
        }
        getSequenceDbPaths(searchEngine,refresh);

        if(relativePath.equals("/"))
        {
            defaultDbPath = getSequenceRootURI().getPath();
        }
        else
        {
            defaultDbPath = getSequenceRootURI().getPath() + relativePath;
        }
        URI defaultDbPathURI;
        try
        {
            if(provider.hasRemoteDirectories())
            {
                relativePath = relativePath.replaceAll(" ","%20");
                URI uriPath = new URI(relativePath);
                sequenceDbs =  provider.getSequenceDbDirList(uriPath);
            }
            else
            {
                defaultDbPathURI = new File(defaultDbPath).toURI();
                sequenceDbs =  provider.getSequenceDbDirList(defaultDbPathURI);
            }          
            if(sequenceDbs == null)
            {
                results.appendError("Could not find the default sequence database path : " + defaultDbPath);
                defaultDbPathURI = getSequenceRootURI();
                sequenceDbs = provider.getSequenceDbDirList(defaultDbPathURI);
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
            sequenceDbs = new ArrayList<String>();
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
            returnList = new ArrayList<String>();
            returnList.add("None found.");
        }
        results.setSequenceDbs(returnList, relativePath);
        return results;
    }

    private void getMzXml(String path, String[] fileNames, String searchEngine, boolean protocolExists)
    {
        if (provider == null)
            provider = (AbstractMS2SearchPipelineProvider) PipelineService.get().getPipelineProvider(searchEngine);
        if (protocol == null)
            protocolExists = false;

        PipeRoot pr;
        URI uriRoot;
        try
        {
            Container c = getContainer();
            pr = PipelineService.get().findPipelineRoot(c);
            if (pr == null || !URIUtil.exists(pr.getUri()))
                throw new IOException("Can't find root directory.");

            uriRoot = pr.getUri();
            URI uriData = URIUtil.resolve(uriRoot, path);
            if (uriData == null)
                throw new IOException("Invalid data directory.");

            File dirData = new File(uriData);
            File dirAnalysis = null;
            if (protocol != null)
                dirAnalysis = protocol.getAnalysisDir(dirData);

            results.setActiveJobs(false);
            results.setFileInputNames(new ArrayList<String>());
            results.setFileInputStatus(new ArrayList<String>());

            Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
            for (String name : fileNames)
            {
                if (name == null || name.indexOf("..") != -1 || name.indexOf("/") != -1 || name.indexOf("\\") != -1)
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
                    AbstractFileAnalysisProtocol.getDataSetBaseName(dirData));
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
            String path = PipelineJobService.statusPathOf(fileStatus.getAbsolutePath());
            try
            {
                PipelineStatusFile sf = PipelineService.get().getStatusFile(path);
                if (sf == null)
                    return null;

                if (sf.isActive())
                    results.setActiveJobs(true);
                return sf.getStatus();
            }
            catch (SQLException e)
            {
            }
        }

        // Failed to get status.  Assume job is active, and return unknown status.
        results.setActiveJobs(true);
        return "UNKNOWN";
    }
}