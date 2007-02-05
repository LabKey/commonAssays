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
package org.labkey.ms2.pipeline;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.XMLValidationParser;
import org.labkey.api.util.AppProps;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.pipeline.*;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.ms2.protocol.*;

import java.io.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.*;

/**
 * MS2PipelineManager class
 * <p/>
 * Created: Sep 21, 2005
 *
 * @author bmaclean
 */
public class MS2PipelineManager
{
    private static final String SEQUEST = MS2PipelineProvider.SEQUEST;
    private static Logger _log = Logger.getLogger(MS2PipelineProvider.class);
    protected static String _pipelineDefaults = "default_input.xml";
    protected static String _pipelineTandemXML = "tandem.xml";
    protected static String _pipelineMascotXML = "mascot.xml";
    protected static String _pipelineDBDir = "databases";
    protected static String _pipelineAllPepXML = "all.pep.xml";

    protected static String _pipelineMzXMLExt = ".mzXML";
    protected static String _pipelineThermoRawExt = ".RAW";
    protected static String _pipelineWatersRawExt = ".raw";
    protected static String _pipelineDataAnnotationExt = ".xar.xml";
    protected static String _pipelineTandemOutExt = ".xtan.xml";
    protected static String _pipelineMascotResultExt = ".dat";
    protected static String _pipelineMgfExt = ".mgf";
    protected static String _pipelineProtXSLIntermediateExt = ".pep-prot.xsl";
    protected static String _pipelineProtSHTMLIntermediateExt = ".pep-prot.shtml";
    protected static String _pipelinePepXSLIntermediateExt = ".pep.xsl";
    protected static String _pipelinePepSHTMLIntermediateExt = ".pep.shtml";
    protected static String _pipelinePepXMLExt = ".pep.xml";
    protected static String _pipelineProtXMLExt = ".prot.xml";
    protected static String _pipelineProtXMLIntermediateExt = ".pep-prot.xml";
    protected static String _pipelineCometHtmlPrefix = "prophet_";
    protected static String _pipelineCometHtmlExt = ".htm";
    protected static String _pipelineSearchExperimentExt = ".search.xar.xml";
    protected static String _pipelineLogExt = ".log";
    protected static String _pipelineStatusExt = ".status";
    protected static String _pipelineDtaExt = "dta";
    protected static String _pipelineSequestXML = "sequest.xml";
     protected static String _pipelineSequestResultExt = ".out";
    protected static String _pipelineSequestRawXmlExt = "-raw.xml";

    private static final String ALLOW_SEQUENCE_DB_UPLOAD_KEY = "allowSequenceDbUpload";
    public static final String SEQUENCE_DB_ROOT_TYPE = "SEQUENCE_DATABASE";

    //todo this the right way
    public static String _allFractionsMzXmlFileBase = "all";

    public static String getBaseName(File file)
    {
        return getBaseName(file, 1);
    }

    public static String getBaseName(File file, int dots)
    {
        String baseName = file.getName();
        while (dots-- > 0)
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        return baseName;
    }

    public static File getSequestRawXmlFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineSequestRawXmlExt);
    }

    public static File getMzXMLFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineMzXMLExt);
    }

    public static boolean isMzXMLFile(File file)
    {
        return file.getName().endsWith(_pipelineMzXMLExt);
    }

    public static boolean isThermoRawFile(File file)
    {
        return file.getName().endsWith(_pipelineThermoRawExt) && file.isFile();
    }

    public static boolean isWatersRawDir(File file)
    {
        return file.getName().endsWith(_pipelineWatersRawExt) && file.isDirectory();
    }

    public static File getAnnotationFile(File dirData, String baseName)
    {
        File xarDir = new File(dirData, "xars");
        return new File(xarDir, baseName + _pipelineDataAnnotationExt);
    }

    public static File getLegacyAnnotationFile(File dirData, String baseName)
    {
        return new File(dirData, baseName + _pipelineDataAnnotationExt);
    }

    public static File getTandemOutFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineTandemOutExt);
    }

//WDN:20060831 sequestdev
    public static File getDtaFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName);
    }

    public static File getSequestOutFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName);
    }
//END-WDN:20060831 sequestdev

//wch: mascotdev
    public static File getMgfFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineMgfExt);
    }

    public static File getMascotOutFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineMascotResultExt);
    }
//END-wch: mascotdev

    public static File getPepXMLFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelinePepXMLExt);
    }

    public static File getProtXMLFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelineProtXMLExt);
    }

    public static File getProtXMLIntermediatFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelineProtXMLIntermediateExt);
    }

    public static boolean isMs2ResultsFile(File file)
    {
        return isPepXMLFile(file) || isCometHtmlFile(file);
    }

//wch: mascotdev
    public static boolean isMascotResultFile(File file)
    {
        return file.getName().toLowerCase().endsWith(_pipelineMascotResultExt);
    }

    public static File getProtXSLIntermediatFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelineProtXSLIntermediateExt);
    }

    public static File getProtSHTMLIntermediatFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelineProtSHTMLIntermediateExt);
    }

    public static File getPepXSLIntermediatFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelinePepXSLIntermediateExt);
    }

    public static File getPepSHTMLIntermediatFile(File dirAnalysis, String baseName)
    {
        if (baseName == null)
            baseName = "all";
        return new File(dirAnalysis, baseName + _pipelinePepSHTMLIntermediateExt);
    }
//END-wch: mascotdev

    public static boolean isPepXMLFile(File file)
    {
        return file.getName().toLowerCase().endsWith(_pipelinePepXMLExt);
    }

    public static boolean isProtXMLFile(File file)
    {
        String nameLc = file.getName().toLowerCase();
        return (nameLc.endsWith(_pipelineProtXMLExt) ||
                nameLc.endsWith(_pipelineProtXMLIntermediateExt));
    }

    public static boolean isCometHtmlFile(File file)
    {
        return (file.getName().startsWith(_pipelineCometHtmlPrefix) &&
                file.getName().toLowerCase().endsWith(_pipelineCometHtmlExt));
    }

    public static File getSearchExperimentFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineSearchExperimentExt);
    }

    public static boolean isSearchExperimentFile(File file)
    {
        return file.getName().toLowerCase().endsWith(_pipelineSearchExperimentExt);
    }

    public static File getStatusFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineStatusExt);
    }

    public static File getLogFile(File dirAnalysis, String baseName)
    {
        return new File(dirAnalysis, baseName + _pipelineLogExt);
    }

    public static File getLogFile(URI uriRoot, String logPath)
    {
        return new File(uriRoot.resolve(logPath));
    }

//wch: mascotdev
    public static File getDefaultInputFile(URI uriRoot, String searchEngine)
    {
        return new File(uriRoot.getPath(), getDefaultInputFilename(searchEngine));
    }
//END-wch: mascotdev

    public static String getDataDescription(File dirData, String baseName, String protocolName)
    {
        String dataName = "";
        if (dirData != null)
        {
            dataName = dirData.getName();
            if ("xml".equals(dataName))
            {
                dirData = dirData.getParentFile();
                if (dirData != null)
                    dataName = dirData.getName();
            }
        }

        StringBuffer description = new StringBuffer(dataName);
        if (baseName != null)
        {
            if (description.length() > 0)
                description.append("/");
            description.append(baseName);
        }
        description.append(" (").append(protocolName).append(")");
        return description.toString();
    }

    public static class UploadFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File file)
        {
            if (isMascotResultFile(file))
                return true;

            if (isMs2ResultsFile(file))
            {
                File parent = file.getParentFile();
                String basename = getBaseName(file, 2);
                return !fileExists(getProtXMLFile(parent, basename)) &&
                        !fileExists(getProtXMLIntermediatFile(parent, basename)) &&
                        !fileExists(getSearchExperimentFile(parent, basename));
            }

            return false;
        }
    }

    public static PipelineProvider.FileEntryFilter getUploadFilter()
    {
        return new UploadFileFilter();
    }

    public static class AnalyzeFileFilter extends PipelineProvider.FileEntryFilter
    {
        public boolean accept(File file)
        {
            // Show all mzXML files.
            if (isMzXMLFile(file))
                return true;

            // If no corresponding mzXML file, show raw files.
            if (isThermoRawFile(file) || isWatersRawDir(file))
            {
                String basename = getBaseName(file);
                File dirData = file.getParentFile();
                if (!fileExists(getMzXMLFile(dirData, basename)))
                    return true;
            }

            return false;
        }
    }

    public static PipelineProvider.FileEntryFilter getAnalyzeFilter()
    {
        return new AnalyzeFileFilter();
    }

//wch: mascotdev
    public static String getDefaultInputFilename(String searchEngine)
    {
        if ("mascot".equalsIgnoreCase (searchEngine))
        {
            return "mascot_" + _pipelineDefaults;
        }
        else if("".equals(searchEngine) || "x!tandem".equalsIgnoreCase (searchEngine))
        {
            return _pipelineDefaults;
        }
        else if(SEQUEST.equalsIgnoreCase (searchEngine))
        {
            return SEQUEST + "_" +_pipelineDefaults;
        }
        else
        {
            //TODO: replace all the spaces
            return searchEngine.toLowerCase() + "_" + _pipelineDefaults;
        }
    }
//END-wch: mascotdev

//wch: mascotdev
    public static String getDefaultInputXML(URI uriRoot, String searchEngine) throws FileNotFoundException, IOException
//END-wch: mascotdev
    {
        BufferedReader reader = null;
        try
        {
//wch: mascotdev
            File fileDefault = getDefaultInputFile(uriRoot, searchEngine);
//END-wch: mascotdev
            if (fileDefault.exists())
            {
                reader = new BufferedReader(new FileReader(fileDefault));
            }
            else
            {
//wch: mascotdev
                String resourceStream = null;
                if ("mascot".equalsIgnoreCase(searchEngine))
                {
                    resourceStream = "org/labkey/ms2/pipeline/MascotDefaults.xml";
                }
                else if(SEQUEST.equalsIgnoreCase(searchEngine))
                {
                    resourceStream = "org/labkey/ms2/pipeline/SequestDefaults.xml";
                }
                else
                {
                    resourceStream = "org/labkey/ms2/pipeline/XTandemDefaults.xml";
                }
                InputStream is = MS2PipelineManager.class.getClassLoader().
                        getResourceAsStream(resourceStream);
                reader = new BufferedReader(new InputStreamReader(is));
//END-wch: mascotdev
            }

            StringBuffer defaults = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null)
            {
                defaults.append(line).append("\n");
            }
            return defaults.toString();
        }
        catch (FileNotFoundException enf)
        {
            _log.error("Default input file missing. Check product setup.", enf);
            throw enf;
        }
        catch (IOException eio)
        {
            _log.error("Error reading default input file.", eio);
            throw eio;
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException eio)
                {
                }
            }
        }
    }

//wch: mascotdev
    public static void setDefaultInputXML(URI uriRoot, String xml, String searchEngine) throws IOException
    {
        if (xml == null || xml.length() == 0)
        {
            if ("".equals(searchEngine) || "x!tandem".equalsIgnoreCase(searchEngine))
            {
                throw new IllegalArgumentException("You must supply default parameters for X! Tandem.");
            }
            else
            {
                throw new IllegalArgumentException("You must supply default parameters for " + searchEngine + ".");
            }
        }

        if ("mascot".equalsIgnoreCase(searchEngine))
        {
            MascotInputParser parser = new MascotInputParser();
            parser.parse(xml);
            if (parser.getErrors() != null)
            {
                XMLValidationParser.Error err = parser.getErrors()[0];
                if (err.getLine() == 0)
                    throw new IllegalArgumentException(err.getMessage());
                else
                    throw new IllegalArgumentException("Line " + err.getLine() + ": " + err.getMessage());
            }
        }
        else if (SEQUEST.equalsIgnoreCase(searchEngine))
        {
            SequestInputParser parser = new SequestInputParser();
            parser.parse(xml);
            if (parser.getErrors() != null)
            {
                XMLValidationParser.Error err = parser.getErrors()[0];
                if (err.getLine() == 0)
                    throw new IllegalArgumentException(err.getMessage());
                else
                    throw new IllegalArgumentException("Line " + err.getLine() + ": " + err.getMessage());
            }
        }
        else
        {
            XTandemInputParser parser = new XTandemInputParser();
            parser.parse(xml);
            if (parser.getErrors() != null)
            {
                XMLValidationParser.Error err = parser.getErrors()[0];
                if (err.getLine() == 0)
                    throw new IllegalArgumentException(err.getMessage());
                else
                    throw new IllegalArgumentException("Line " + err.getLine() + ": " + err.getMessage());
            }
        }

        File fileDefault = new File(uriRoot.getPath(), getDefaultInputFilename(searchEngine));
//END-wch: mascotdev

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(fileDefault));
            writer.write(xml, 0, xml.length());
        }
        catch (IOException eio)
        {
            _log.error("Error writing default input file.", eio);
            throw eio;
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException eio)
                {
                    _log.error("Error writing default input file.", eio);
                    throw eio;
                }
            }
        }
    }

//wch: mascotdev
    public static Map<String, String[]> getSequenceDBNames(URI sequenceDbRoot, String searchEngine)
    {
        if ("mascot".equalsIgnoreCase(searchEngine))
        {
            AppProps appProps = AppProps.getInstance();
            MascotClientImpl mascotClient = new MascotClientImpl(appProps.getMascotServer(), null);
            mascotClient.setProxyURL(appProps.getMascotHTTPProxy());
            return mascotClient.getSequenceDBNames();
        }
        if ("sequest".equalsIgnoreCase(searchEngine))
        {
            AppProps appProps = AppProps.getInstance();
            if(!"".equals(appProps.getSequestServer()))
            {
                SequestClient sequestClient = new SequestClient(appProps.getSequestServer(), null);
                // TODO sequestClient.setProxyURL(appProps.getSequestHTTPProxy());
                return sequestClient.getSequenceDBNames();
            }
            else
            {
                return addSequenceDBNames(new File(sequenceDbRoot), "", new LinkedHashMap<String, String[]>(), searchEngine);
            }
        }
        else
        {
            return addSequenceDBNames(new File(sequenceDbRoot), "", new LinkedHashMap<String, String[]>(), searchEngine);
        }
    }
//END-wch: mascotdev

//wch: mascotdev
    public static Map<String, String[]> addSequenceDBNames(File dir, String path, Map<String, String[]> m, String searchEngine)
//END-wch: mascotdev
    {
        File[] dbFiles;
        if(searchEngine.equals(SEQUEST))
        {
            dbFiles = dir.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    final String name = f.getName();
                    //added filters for Sequest indexed databases
                    if (name.startsWith(".") ||
                            name.endsWith(".check") ||
                            name.endsWith(".out") ||
                            name.endsWith(".idx") ||
                            name.endsWith(".dgt") ||
                            name.endsWith(".log"))
                        return false;
                    return true;
                }
            });
        }
        else
        {
            dbFiles = dir.listFiles(new FileFilter()
            {
                public boolean accept(File f)
                {
                    final String name = f.getName();
                    //added filters for Sequest indexed databases
                    if (name.startsWith(".") ||
                            name.endsWith(".check") ||
                            name.endsWith(".out") ||
                            name.endsWith(".idx") ||
                            name.endsWith(".dgt") ||
                            name.endsWith(".hdr") ||
                            name.endsWith(".log"))
                        return false;
                    return true;
                }
            });
        }
        if (dbFiles == null)
            return m;
        Arrays.sort(dbFiles, new Comparator<File>()
        {
            public int compare(File f1, File f2)
            {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });
        ArrayList<String> listNames = new ArrayList<String>();
        ArrayList<File> listSubdirs = new ArrayList<File>();
        if (dbFiles != null)
        {
            for (File dbFile : dbFiles)
            {
                if (dbFile.isDirectory())
                    listSubdirs.add(dbFile);
                else
                    listNames.add(dbFile.getName());
            }
        }

        if (listNames.size() > 0)
            m.put(path, listNames.toArray(new String[listNames.size()]));

        for (File subdir : listSubdirs)
        {
//wch: mascotdev
             addSequenceDBNames(subdir, path + subdir.getName() + "/", m, searchEngine);
//END-wch: mascotdev
        }

        return m;
    }

    public static void addSequenceDB(Container container, String name, BufferedReader reader) throws IOException, SQLException
    {
        File fileDB = getSequenceDBFile(getSequenceDatabaseRoot(container), name);
        if (fileDB.exists())
        {
            throw new IllegalArgumentException("The sequence database '" + name +
                    "' already exists. Please choose another name.");
        }

        File dirDB = fileDB.getParentFile();
        if (!dirDB.exists())
            dirDB.mkdirs();

        BufferedWriter writer = null;
        try
        {
            String line;
            writer = new BufferedWriter(new FileWriter(fileDB));
            while ((line = reader.readLine()) != null)
            {
                /* @todo: check FASTA file format, and throw exception, if bad. */
                writer.write(line, 0, line.length());
                writer.write("\n", 0, 1);
            }
        }
        catch (IOException eio)
        {
            _log.error("Error writing sequence database.", eio);
            throw eio;
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException eio)
                {
                    _log.error("Error writing sequence database.", eio);
                    throw eio;
                }
            }
        }
    }

    public static boolean removeSequenceDB(URI uriSequenceRoot, String name)
    {
        File fileDB = getSequenceDBFile(uriSequenceRoot, name);
        return fileDB.delete();
    }

    public static File getSequenceDBFile(URI uriSequenceRoot, String name)
    {
        File fileRoot = new File(uriSequenceRoot);
        File file = new File(uriSequenceRoot.getPath() + name);
        if (!file.getAbsolutePath().startsWith(fileRoot.getAbsolutePath()))
            throw new IllegalArgumentException("Invalid sequence database '" + name + "'.");

        return file;
    }

//wch: mascotdev
    private static File getAnalysisDir(URI uriData, String name, String searchEngine)
//END-wch: mascotdev
    {
        File dirDataRoot = new File(uriData);
        if (name == null || name.length() == 0)
            return dirDataRoot;

        if (name.indexOf(File.separatorChar) != -1 || name.indexOf('/') != -1)
            throw new IllegalArgumentException("Invalid analysis name '" + name + "'.");

        if ("xml".equals(dirDataRoot.getName()) || "raw".equals(dirDataRoot.getName()))
            dirDataRoot = dirDataRoot.getParentFile();

        File dirAnalysisRoot = new File(dirDataRoot,
//wch: mascotdev
                getSearchProtocolInstance(searchEngine).getName());
//END-wch: mascotdev
        return new File(dirAnalysisRoot, name);
    }

    public static Map<File, FileStatus> getAnalysisFileStatus(URI uriData, String protocolName, Container c, String searchEngine) throws IOException
    {
        File dirData = new File(uriData);
        File[] mzXMLFiles = dirData.listFiles(getAnalyzeFilter());

        Map<File, FileStatus> analysisMap = new LinkedHashMap<File, FileStatus>();
        if (mzXMLFiles != null && mzXMLFiles.length > 0)
        {
            Arrays.sort(mzXMLFiles, new Comparator<File>()
            {
                public int compare(File o1, File o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            File dirAnalysis = null;
            if (protocolName != null)
            {
                dirAnalysis = getAnalysisDir(uriData, protocolName, searchEngine);
                if (!dirAnalysis.exists())
                    dirAnalysis = null;
            }

            boolean all = getLogFile(dirAnalysis, "all").exists();
            boolean allComplete = all && getSearchExperimentFile(dirAnalysis, "all").exists();

            for (File file : mzXMLFiles)
            {
                FileStatus status = FileStatus.UNKNOWN;
                String baseName = getBaseName(file);
                if (dirAnalysis != null && dirAnalysis.exists())
                {
                    if (allComplete ||
                            (!all && getSearchExperimentFile(dirAnalysis, baseName).exists()))
                        status = FileStatus.COMPLETE;
                    else if (getLogFile(dirAnalysis, baseName).exists())
                        status = FileStatus.RUNNING;
                }
                if (status == FileStatus.UNKNOWN)
                {
                    if (findAnnotationFile(file, baseName) != null)
                    {
                        status = FileStatus.ANNOTATED;
                    }
                }
                if (status == FileStatus.UNKNOWN)
                {
                    if (ExperimentService.get().getCreatingRun(file, c) != null)
                    {
                        status = FileStatus.ANNOTATED;
                    }
                }
                analysisMap.put(file, status);
            }
        }
        return analysisMap;
    }

    public static File findAnnotationFile(File file)
    {
        return findAnnotationFile(file, getBaseName(file));
    }

    private static File findAnnotationFile(File file, String baseName)
    {
        File dirData = file.getParentFile();
        File annotationFile = getAnnotationFile(dirData, baseName);
        if (annotationFile.exists())
        {
            return annotationFile;
        }
        annotationFile = getAnnotationFile(dirData, _allFractionsMzXmlFileBase);
        if (annotationFile.exists())
        {
            return annotationFile;
        }
        annotationFile =getLegacyAnnotationFile(dirData, baseName);
        if (annotationFile.exists())
        {
            return annotationFile;
        }
        annotationFile = getLegacyAnnotationFile(dirData, _allFractionsMzXmlFileBase);
        if (annotationFile.exists())
        {
            return annotationFile;
        }
        return null;
    }

    public static File[] getAnalysisFiles(URI uriData, String protocolName, FileStatus status, Container c, String searchEngine) throws IOException
    {
        Map<File, FileStatus> mzXMLFileStatus = getAnalysisFileStatus(uriData, protocolName, c, searchEngine);
        List<File> fileList = new ArrayList<File>();
        for (File fileMzXML : mzXMLFileStatus.keySet())
        {
            if (status == null || status.equals(mzXMLFileStatus.get(fileMzXML)))
                fileList.add(fileMzXML);
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    public static File getConfigureXMLFile(URI uriData, String name, String searchEngine)
    {
        if ("mascot".equalsIgnoreCase(searchEngine))
        {
            return getMascotXMLFile (uriData, name);
        }
        else if(SEQUEST.equalsIgnoreCase(searchEngine))
        {
            return getSequestXMLFile(uriData, name);
        }
        else
        {
            // we take X! Tandem as the default case
            return getTandemXMLFile (uriData, name);
        }
    }

    public static File getTandemXMLFile(URI uriData, String name)
    {
        File dirAnalysis = getAnalysisDir(uriData, name, "X!Tandem");
        return new File(dirAnalysis, _pipelineTandemXML);
    }

//WDN:20060901 sequestdev
    public static File getSequestXMLFile(URI uriData, String name)
    {
        File dirAnalysis = getAnalysisDir(uriData, name, SEQUEST);
        return new File(dirAnalysis, _pipelineSequestXML);
    }
//END-WDN:20060901 sequestdev

    public static File getMascotXMLFile(URI uriData, String name)
    {
        File dirAnalysis = getAnalysisDir(uriData, name, "Mascot");
        return new File(dirAnalysis, _pipelineMascotXML);
    }

    public static PipelineProtocolFactory getSearchProtocolInstance (String searchEngine)
    {
        PipelineProtocolFactory searchProtocol = null;
        if ("mascot".equalsIgnoreCase(searchEngine))
        {
            searchProtocol = MascotSearchProtocolFactory.get();
        }
        //WDN:20060828 sequestdev
        else if (SEQUEST.equalsIgnoreCase(searchEngine))
        {
            searchProtocol = SequestSearchProtocolFactory.get();
        }
        //END-WDN:20060828 sequestdev
        else
        {
            // we take X! Tandem as the default case
            searchProtocol = XTandemSearchProtocolFactory.get();
        }
        return searchProtocol;
    }

    public static void runAnalysis(ViewBackgroundInfo info,
                                   URI uriRoot,
                                   URI uriData,
                                   URI uriSequenceRoot,
                                   PipelineProtocol protocol,
                                   String searchEngine) throws IOException
    {
        String protocolName = protocol.getName();
        File dirData = new File(uriData);
        if (!dirData.exists())
        {
            throw new IllegalArgumentException("The specified data directory does not exist.");
        }

        AppProps appProps = AppProps.getInstance();
        String mascotServer = appProps.getMascotServer();
        String mascotHTTPProxy = appProps.getMascotHTTPProxy();
        if ("mascot".equalsIgnoreCase(searchEngine) && (!appProps.hasMascotServer() || 0==mascotServer.length()))
            throw new IllegalArgumentException("Mascot server has not been specified in site customization.");

        File[] annotatedFiles = getAnalysisFiles(uriData, protocolName, FileStatus.ANNOTATED, info.getContainer(), searchEngine);
        File[] unprocessedFile = getAnalysisFiles(uriData, protocolName, FileStatus.UNKNOWN, info.getContainer(), searchEngine);
        List<File> mzXMLFileList = new ArrayList<File>();
        mzXMLFileList.addAll(Arrays.asList(annotatedFiles));
        mzXMLFileList.addAll(Arrays.asList(unprocessedFile));
        File[] mzXMLFiles = mzXMLFileList.toArray(new File[mzXMLFileList.size()]);
        if (mzXMLFiles.length == 0)
            throw new IllegalArgumentException("Analysis for this protocol is already complete.");

        // Make sure defaults are set before the search begins.
        // Cluster pipeline has its own default input semantics.
        boolean hasCluster = AppProps.getInstance().hasPipelineCluster();
        if (!hasCluster && !getDefaultInputFile(uriRoot, searchEngine).exists())
            setDefaultInputXML(uriRoot, getDefaultInputXML(uriRoot, searchEngine), searchEngine);

        File fileConfigureXML = getConfigureXMLFile(uriData, protocolName, searchEngine);

        PipelineService service = PipelineService.get();
        for (File fileMzXML : mzXMLFiles)
        {
            // Make sure configure.xml file exists for the job when it runs.
            if (!fileConfigureXML.exists())
            {
                PipelineProtocolFactory searchProtocol = getSearchProtocolInstance (searchEngine);
//                PipelineProtocol protocol = searchProtocol.load(uriRoot, protocolName);
                if ("mascot".equalsIgnoreCase(searchEngine))
                {
                    MascotSearchProtocol specificProtocol = (MascotSearchProtocol) protocol;
                    specificProtocol.setEmail(info.getUser().getEmail());
                    specificProtocol.setMascotServer(mascotServer);
                    specificProtocol.setMascotHTTPProxy(mascotHTTPProxy);
                    specificProtocol.saveInstance(fileConfigureXML);
                }
                else if(SEQUEST.equalsIgnoreCase(searchEngine))
                {
                    SequestSearchProtocol specificProtocol = (SequestSearchProtocol) protocol;
                    specificProtocol.setEmail(info.getUser().getEmail());
                    specificProtocol.saveInstance(fileConfigureXML);
                }
                else
                {
                    // we take X! Tandem as the default case
                    XTandemSearchProtocol specificProtocol = (XTandemSearchProtocol) protocol;
                    specificProtocol.setEmail(info.getUser().getEmail());
                    specificProtocol.saveInstance(fileConfigureXML);
                }
            }

            // If there is a cluster, just set status to "WAITING", and let
            // the cluster take over the processing.  A new job will get created
            // when the cluster tells the web server to upload the search results.
            PipelineJob job = null;
            if ("mascot".equalsIgnoreCase(searchEngine))
            {
                job = new MascotPipelineJob(info,
                                             protocolName,
                                             uriRoot,
                                             uriSequenceRoot,
                                             new File[] { fileMzXML },
                                             fileConfigureXML,
                                             hasCluster,
                                             false);
            }
            else if(SEQUEST.equalsIgnoreCase(searchEngine))
            {
               job = new SequestPipelineJob(info,
                                             protocolName,
                                             uriRoot,
                                             uriSequenceRoot,
                                             new File[] { fileMzXML },
                                             fileConfigureXML,
                                             hasCluster,
                                             false);
            }
            else
            {
                // we take X! Tandem as the default case
                job = new XTandemPipelineJob(info,
                                             protocolName,
                                             uriRoot,
                                             uriSequenceRoot,
                                             new File[] { fileMzXML },
                                             fileConfigureXML,
                                             hasCluster,
                                             false);
            }

            // If there is a cluster, just let the cluster find and handle the job.
            if (hasCluster)
                job.setStatus(PipelineJob.WAITING_STATUS);
            // Otherwise, queue for local search.
            else
                service.queueJob(job);
        }

        if (hasCluster)
        {
            // If there is a cluster, and fractions are being processed,
            // prime the pipeline status with status for the fractions job.
            File fileFractions = new File(dirData, "all.xar.xml");
            if (fileFractions.exists())
            {
                PipelineJob job = null;
                if ("mascot".equalsIgnoreCase(searchEngine))
                {
                    job = new MascotPipelineJob(info,
                                                 protocolName,
                                                 uriRoot,
                                                 uriSequenceRoot,
                                                 mzXMLFiles,
                                                 fileConfigureXML,
                                                 hasCluster,
                                                 false);
                }
                else
                {
                    // we take X! Tandem as the default case
                    job = new XTandemPipelineJob(info,
                                                 protocolName,
                                                 uriRoot,
                                                 uriSequenceRoot,
                                                 mzXMLFiles,
                                                 fileConfigureXML,
                                                 hasCluster,
                                                 false);
                }
                job.setStatus("WAITING");
            }
        }
    }

    public static String getSearchEngine (File file)
    {
        String searchEngine = "X!Tandem";
        String[] pathParts;
        if (System.getProperty("file.separator").equals("\\"))
            pathParts = file.getAbsolutePath().split("\\\\");
        else
            pathParts = file.getAbsolutePath().split(System.getProperty("file.separator"));
        // TODO: do we take DataDir as the anchor point or end of string as anchor point?
        if (pathParts.length>2){
            searchEngine = pathParts[pathParts.length-3];
        }
        return searchEngine;
    }

    public static PipelineJob runUpload(ViewBackgroundInfo info,
                                   URI uriRoot,
                                   URI uriData,
                                   URI uriSequenceRoot,
                                   String protocolName,
                                   File fileUpload) throws IOException
    {
        File dirData = new File(uriData);
        if (!dirData.exists())
        {
            return null;
        }

        boolean hasCluster = AppProps.getInstance().hasPipelineCluster();

        String searchEngine = getSearchEngine(fileUpload);
        boolean mascotFile = "mascot".equalsIgnoreCase(searchEngine);
        File fileConfigureXML = getConfigureXMLFile(uriData, protocolName, mascotFile ? "Mascot" : "X!Tandem");

        String baseName = getBaseName(fileUpload, 2);

        File[] filesMzXML;
        if (!"all".equals(baseName))
        {
            filesMzXML = new File[] { getMzXMLFile(dirData, baseName) };
        }
        else
        {
            // Anything that is running or complete.
            Map<File, FileStatus> mzXMLFileStatus = getAnalysisFileStatus(uriData, protocolName, info.getContainer(), searchEngine);
            List<File> fileList = new ArrayList<File>();
            for (File fileMzXML : mzXMLFileStatus.keySet())
            {
                FileStatus status = mzXMLFileStatus.get(fileMzXML);
                if (status.equals(FileStatus.COMPLETE) || status.equals(FileStatus.RUNNING))
                    fileList.add(fileMzXML);
            }
            filesMzXML = fileList.toArray(new File[fileList.size()]);
        }

        if (filesMzXML.length == 0)
            return null;

        PipelineJob job;
        if (mascotFile)
        {
            job = new MascotPipelineJob(info,
                      protocolName,
                      uriRoot,
                      uriSequenceRoot,
                      filesMzXML,
                      fileConfigureXML,
                      hasCluster,
                      true);
        }
        else
        {
            job = new XTandemPipelineJob(info,
                      protocolName,
                      uriRoot,
                      uriSequenceRoot,
                      filesMzXML,
                      fileConfigureXML,
                      hasCluster,
                      true);
        }

        // Queue for upload.
        PipelineService.get().queueJob(job);

        return job;
    }
//END-wch: mascot

    public static URI getSequenceDatabaseRoot(Container container) throws SQLException
    {
        URI dbRoot = PipelineService.get().getPipelineRoot(container, SEQUENCE_DB_ROOT_TYPE);
        if (dbRoot == null)
        {
            // return default root
            URI root = PipelineService.get().getPipelineRoot(container);
            if (root != null)
                dbRoot = new File(root.getPath(), _pipelineDBDir).toURI();
        }
        return dbRoot;
    }

    public static void setSequenceDatabaseRoot(User user, Container container, URI rootSeq, boolean allowUpload) throws SQLException
    {
        PipelineService service = PipelineService.get();

        // If the new root is just the default, then clear the entry.
        URI root = service.getPipelineRoot(container);
        if (rootSeq != null && root != null && rootSeq.equals(new File(root.getPath(), _pipelineDBDir).toURI()))
             rootSeq = null;

        service.setPipelineRoot(user, container, rootSeq, SEQUENCE_DB_ROOT_TYPE);
        if (root != null)
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, allowUpload ? "true" : "false");
        else
            service.setPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY, null);
    }

    public static boolean allowSequenceDatabaseUploads(User user, Container container) throws SQLException
    {
        if (!container.hasPermission(user, ACL.PERM_INSERT))
            return false;
        Object obj = PipelineService.get().getPipelineProperty(container, ALLOW_SEQUENCE_DB_UPLOAD_KEY);
        return Boolean.parseBoolean((String) obj);
    }

//wch: mascotdev
    public static void copyFile(File source, File destination) throws IOException
    {
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(destination);
        byte[] buffer = new byte [4096]; // use 4-KB fragment
        int readLen;
        while ((readLen=in.read(buffer))>0)
        {
            out.write(buffer,0,readLen);
        }
        in.close();
        out.close();
    }
//END-wch: mascotdev
}
