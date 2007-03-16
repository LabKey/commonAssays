package org.labkey.ms2.pipeline;

import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.util.PathRelativizer;
import org.labkey.api.util.XMLValidationParser;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.net.URI;

/**
 * User: jeckels
 * Date: Nov 9, 2006
 */
public abstract class AbstractMS2SearchPipelineJob extends PipelineJob
{
    protected Integer _experimentRowId;
    protected String _name;
    protected String _baseName;
    protected File _dirMzXML;
    protected File[] _filesMzXML;
    protected URI _uriRoot;
    protected URI _uriSequenceRoot;
    protected File _dirAnalysis;
    protected File _filePepXML;

    public AbstractMS2SearchPipelineJob(String provider, ViewBackgroundInfo info, File filesMzXML[], String name, URI uriRoot, URI uriSequenceRoot)
    {
        super(provider, info);
        _filesMzXML = filesMzXML;
        _dirMzXML = filesMzXML[0].getParentFile();
        _name = name;

        _uriRoot = uriRoot;
        _uriSequenceRoot = uriSequenceRoot;
    }

    protected String getInstanceDetailsSnippet(File mzXMLFile, File analysisDir, File[] databaseFiles, File configFile) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("                      <exp:InstanceDetails>\n");
        sb.append("                        <exp:InstanceInputs>\n");
        sb.append("                          <exp:DataLSID DataFileUrl=\"");
        sb.append(PathRelativizer.relativizePathUnix(analysisDir, mzXMLFile));
        sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        sb.append("                          <exp:DataLSID DataFileUrl=\"");
        sb.append(PathRelativizer.relativizePathUnix(analysisDir, configFile));
        sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        for (File dbFile : databaseFiles)
        {
            sb.append("                          <exp:DataLSID DataFileUrl=\"");
            sb.append(PathRelativizer.relativizePathUnix(analysisDir, dbFile));
            sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        }
        sb.append("                        </exp:InstanceInputs>\n");
        sb.append("                      </exp:InstanceDetails>\n");
        return sb.toString();
    }

    protected boolean hasValidPerl()
    {
        boolean hasPerl = true;
        BufferedReader procReader = null;
        try
        {
            Process proc = new ProcessBuilder("perl", "-v").start();
            procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = procReader.readLine()) != null)
            {
                if (line.indexOf("cygwin") != -1)
                    hasPerl = false;
            }
            if (proc.waitFor() != 0)
                hasPerl = false;
        }
        catch (IOException e)
        {
            hasPerl = false;
        }
        catch (InterruptedException e)
        {
            hasPerl = false;
        }
        finally
        {
            if (procReader != null)
            {
                try { procReader.close(); }
                catch (IOException e) {}
            }
        }

        return hasPerl;
    }

    protected String getStartingInputDataSnippet(File f, File analysisDir) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t<exp:Data rdf:about=\"${AutoFileLSID}\">\n");
        sb.append("\t\t\t<exp:Name>");
        sb.append(f.getName());
        sb.append("</exp:Name>\n");
        sb.append("\t\t\t<exp:CpasType>Data</exp:CpasType>\n");
        sb.append("\t\t\t<exp:DataFileUrl>");
        sb.append(PathRelativizer.relativizePath(analysisDir, f));
        sb.append("</exp:DataFileUrl>\n");
        sb.append("\t\t</exp:Data>\n");
        return sb.toString();
    }

    protected void getLabelOptions(String paramQuant, List<String> quantOpts)
    {
        String[] quantSpecs = paramQuant.split(",");
        for (String spec : quantSpecs)
        {
            String[] specVals = spec.split("@");
            if (specVals.length != 2)
                continue;
            String mass = specVals[0].trim();
            String aa = specVals[1].trim();
            quantOpts.add("-n" + aa + "," + mass);
        }
    }

    private String getCommandEscapeChar()
    {
//        String nameOs = System.getProperty("os.name");
//        if (nameOs != null && nameOs.startsWith("Windows"))
//            return "^";
//        else
//  Apparently \ works best on Windows too after all.
            return "\\";
    }
    
    public String getQuantitationCmd(BioMLInputParser parser, File dirMzXML)
    {
        String paramAlgorithm = parser.getInputParameter("pipeline quantitation, algorithm");
        if (paramAlgorithm == null)
            return null;
        if (!"q3".equalsIgnoreCase(paramAlgorithm) && !"xpress".equalsIgnoreCase(paramAlgorithm))
            return null;    // CONSIDER: error message.

        List<String> quantOpts = new ArrayList<String>();

        String paramQuant = parser.getInputParameter("pipeline quantitation, residue label mass");
        if (paramQuant != null)
            getLabelOptions(paramQuant, quantOpts);

        paramQuant = parser.getInputParameter("pipeline quantitation, mass tolerance");
        if (paramQuant != null)
            quantOpts.add("-m");

        paramQuant = parser.getInputParameter("pipeline quantitation, heavy elutes before light");
        if (paramQuant != null)
            if("yes".equalsIgnoreCase(paramQuant))
                quantOpts.add("-b");

        paramQuant = parser.getInputParameter("pipeline quantitation, fix");
        if (paramQuant != null)
        {
            if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-H");
            else if ("light".equalsIgnoreCase(paramQuant))
                quantOpts.add("-L");
        }

        paramQuant = parser.getInputParameter("pipeline quantitation, fix elution reference");
        if (paramQuant != null)
        {
            String refFlag = "-f";
            if ("peak".equalsIgnoreCase(paramQuant))
                refFlag = "-F";
            paramQuant = parser.getInputParameter("pipeline quantitation, fix elution difference");
            if (paramQuant != null)
                quantOpts.add(refFlag + paramQuant);
        }

        paramQuant = parser.getInputParameter("pipeline quantitation, metabolic search type");
        if (paramQuant != null)
        {
            if ("normal".equalsIgnoreCase(paramQuant))
                quantOpts.add("-M");
            else if ("heavy".equalsIgnoreCase(paramQuant))
                quantOpts.add("-N");
        }

        String esc = getCommandEscapeChar();
        String dirMzXMLPathName = dirMzXML.toString();
        // Strip trailing file separater, since on Windows this might be a \, which will
        // cause escaping difficulties.
        if (dirMzXMLPathName.endsWith(File.separator))
            dirMzXMLPathName = dirMzXMLPathName.substring(0, dirMzXMLPathName.length() - 1);
        quantOpts.add("-d" + esc + "\"" + dirMzXML + esc + "\"");

        if ("xpress".equals(paramAlgorithm))
            return ("\"-X" + StringUtils.join(quantOpts.iterator(), ' ') + "\"");

        String paramMinPP = parser.getInputParameter("pipeline quantitation, min peptide prophet");
        if (paramMinPP != null)
            quantOpts.add("--minPeptideProphet=" + paramMinPP);
        String paramMaxDelta = parser.getInputParameter("pipeline quantitation, max fractional delta mass");
        if (paramMaxDelta != null)
            quantOpts.add("--maxFracDeltaMass=" + paramMaxDelta);
        String paramCompatQ3 = parser.getInputParameter("pipeline quantitation, q3 compat");
        if ("yes".equalsIgnoreCase(paramCompatQ3))
            quantOpts.add("--compat");

        return ("\"-C1java -client -Xmx256M -jar "
                + esc + "\"" /* + path to bin */ + "msInspect/viewerApp.jar" + esc + "\""
                + " --q3 " + StringUtils.join(quantOpts.iterator(), ' ') + "\""
                + " -C2Q3ProteinRatioParser");
    }

    public boolean isXPressQuantitation(BioMLInputParser parser)
    {
        return "xpress".equalsIgnoreCase(parser.getInputParameter("pipeline quantitation, algorithm"));
    }

    public ViewURLHelper getStatusHref()
    {
        if (_experimentRowId != null)
        {
            ViewURLHelper ret = getViewURLHelper().clone();
            ret.setPageFlow("Experiment");
            ret.setAction("details");
            ret.setExtraPath(getContainer().getPath());
            ret.deleteParameters();
            ret.addParameter("rowId", _experimentRowId.toString());
            return ret;
        }
        return null;
    }

    protected void replaceString(StringBuilder sb, String oldString, String newString)
    {
        oldString = "@@" + oldString + "@@";
        int index = sb.indexOf(oldString);
        while (index != -1)
        {
            sb.replace(index, index + oldString.length(), newString);
            index = sb.indexOf(oldString);
        }
    }

    public String getDescription()
    {
        if (_filesMzXML.length > 1)
            return MS2PipelineManager.getDataDescription(_dirMzXML, null, _name);
        else
        {
            String baseName = MS2PipelineManager.getBaseName(_filesMzXML[0]);
            return MS2PipelineManager.getDataDescription(_dirMzXML, baseName, _name);
        }
    }

    public boolean isFractions()
    {
        return _filesMzXML.length > 1;
    }

    public File getPepXMLFile()
    {
        return _filePepXML;
    }

    public void run()
    {
        // If the pepXML file already exists, then just upload it.  Most likely
        // case is that the site has a cluster for searching.
        if (_filePepXML.exists())
            upload();
        else
            search();
    }

    public abstract void upload();
    public abstract void search();

    protected String getDataLSIDSnippet(File[] files, File analysisDir, String baseRoleName) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        for (File file : files)
        {
            sb.append("                                <exp:DataLSID DataFileUrl=\"");
            sb.append(PathRelativizer.relativizePathUnix(analysisDir, file));
            sb.append("\" RoleName=\"");
            sb.append(baseRoleName);
            sb.append("\">${AutoFileLSID}</exp:DataLSID>\n");
        }
        return sb.toString();
    }

    protected String getMzXMLPaths(File analysisDir)
            throws IOException
    {
        StringBuilder result = new StringBuilder();
        File mzXMLDir = _filesMzXML[0].getParentFile();
        File[] allMzXMLFiles = mzXMLDir.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".mzxml");
            }
        });
        Set<File> fileSet1 = new HashSet<File>(Arrays.asList(allMzXMLFiles));
        Set<File> fileSet2 = new HashSet<File>(Arrays.asList(_filesMzXML));
        if (fileSet1.equals(fileSet2))
        {
            result.append(PathRelativizer.relativizePathUnix(analysisDir, mzXMLDir));
            result.append("*.mzxml");
        }
        else
        {
            for (File f : _filesMzXML)
            {
                if (result.length() > 0)
                {
                    result.append(";");
                }
                result.append(PathRelativizer.relativizePathUnix(analysisDir, f));
            }
        }

        return result.toString();
    }
}
