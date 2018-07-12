/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.SubsetPart;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeCache;
import org.springframework.validation.Errors;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

public class WorkspaceData implements Serializable
{
    static final private Logger _log = Logger.getLogger(WorkspaceData.class);

    String path;
    String name;
    String originalPath;
    IWorkspace _object;
    // UNDONE: Placeholder for when analysis archives (or ACS archives) include FCS files during import.
    boolean _includesFCSFiles;

    public void setPath(String path)
    {
        if (path != null)
        {
            path = PageFlowUtil.decode(path);
            this.path = path;
            this.name = new File(path).getName();
        }
    }

    public String getPath()
    {
        return path;
    }

    public void setOriginalPath(String path)
    {
        if (path != null)
        {
            path = PageFlowUtil.decode(path);
            this.originalPath = path;
        }
    }

    public String getOriginalPath()
    {
        return this.originalPath;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    public void setObject(String object) throws Exception
    {
        _object = (IWorkspace) PageFlowUtil.decodeObject(object);
    }

    public IWorkspace getWorkspaceObject()
    {
        return _object;
    }

    public boolean isIncludesFCSFiles()
    {
        return _includesFCSFiles;
    }

    public void setIncludesFCSFiles(boolean includesFCSFiles)
    {
        _includesFCSFiles = includesFCSFiles;
    }

    public void validate(Container container, Errors errors, HttpServletRequest request)
    {
        try
        {
            validate(container, errors);
        }
        catch (FlowException | WorkspaceValidationException ex)
        {
            errors.reject(ERROR_MSG, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
        catch (Exception ex)
        {
            errors.reject(ERROR_MSG, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            ExceptionUtil.decorateException(ex, ExceptionUtil.ExceptionInfo.ExtraMessage, "name: " + this.name + ", path: " + this.path, true);
            ExceptionUtil.logExceptionToMothership(request, ex);
        }
    }

    public void validate(Container container, Errors errors) throws WorkspaceValidationException, IOException
    {
        if (_object == null)
        {
            if (path != null)
            {
                PipeRoot pipeRoot;
                try
                {
                    pipeRoot = PipelineService.get().findPipelineRoot(container);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("An error occurred trying to retrieve the pipeline root: " + e, e);
                }

                if (pipeRoot == null)
                {
                    throw new WorkspaceValidationException("There is no pipeline root in this folder.");
                }

                File file = pipeRoot.resolvePath(path);
                if (file == null)
                {
                    throw new WorkspaceValidationException("The path '" + path + "' is invalid.");
                }
                if (!file.exists())
                {
                    throw new WorkspaceValidationException("The file '" + path + "' does not exist.");
                }
                if (!file.canRead())
                {
                    throw new WorkspaceValidationException("The file '" + path + "' is not readable.");
                }

                if (file.getName().endsWith(AnalysisSerializer.STATISTICS_FILENAME))
                {
                    // Set path to parent directory
                    file = file.getParentFile();
                    this.path = pipeRoot.relativePath(file);
                }
                else if (path.endsWith(".zip"))
                {
                    // Extract external analysis zip into pipeline
                    File tempDir = pipeRoot.resolvePath(PipelineService.UNZIP_DIR);
                    if (tempDir.exists() && !FileUtil.deleteDir(tempDir))
                        throw new IOException("Failed to delete temp directory");

                    String originalPath = path;
                    File zipFile = pipeRoot.resolvePath(path);
                    file = AnalysisSerializer.extractArchive(zipFile, tempDir);

                    String workspacePath = pipeRoot.relativePath(file);
                    this.path = workspacePath;
                    this.originalPath = originalPath;
                }

                _object = readWorkspace(file, path);
                validateCasing(container, errors);
            }
            else
            {
                throw new WorkspaceValidationException("No workspace file was specified.");
            }
        }
    }

    private void validateCasing(Container c, Errors errors)
    {
        Set<String> seenKeyword = new HashSet<>();
        Set<StatisticSpec> seenStat = new HashSet<>();
        Set<GraphSpec> seenGraph = new HashSet<>();

        Map<SubsetSpec, Integer> subsetMismatches = new HashMap<>();
        Map<String, Integer> parameterMismatches = new HashMap<>();

        for (ISampleInfo sample : _object.getSamples())
        {
            for (String keyword : sample.getKeywords().keySet())
            {
                if (seenKeyword.contains(keyword))
                    continue;
                seenKeyword.add(keyword);

                AttributeCache.KeywordEntry entry = AttributeCache.KEYWORDS.byName(c, keyword);
                if (entry == null)
                    continue;

                if (!keyword.equals(entry.getName()))
                {
                    errors.reject(ERROR_MSG, _object.getName() + ": Sample " + sample.getLabel() + ": Keyword '" + keyword + "' has different casing than existing keyword '" + entry.getName() + "'.  Please correct the casing before importing.");
                    if (errors.getErrorCount() > 10)
                        break;
                }
            }

            Analysis analysis = _object.getSampleAnalysis(sample);
            if (analysis == null)
                continue;

            for (StatisticSpec spec : analysis.getStatistics())
            {
                if (seenStat.contains(spec))
                    continue;
                seenStat.add(spec);

                AttributeCache.StatisticEntry entry = AttributeCache.STATS.byAttribute(c, spec);
                if (entry == null)
                    continue;

                if (!spec.toString().equals(entry.getAttribute().toString()))
                {
                    if (seenMismatch(subsetMismatches, parameterMismatches, entry.getAttribute().getSubset(), entry.getAttribute().getParameter(), spec.getSubset(), spec.getParameter()))
                        continue;

                    errors.reject(ERROR_MSG, _object.getName() + ": Sample " + sample.getLabel() + ": Statistic '" + spec + "' has different casing than existing statistic '" + entry.getName() + "'.  Please correct the casing before importing.");
                    if (errors.getErrorCount() > 10)
                        break;
                }
            }

            for (GraphSpec spec : analysis.getGraphs())
            {
                if (seenGraph.contains(spec))
                    continue;
                seenGraph.add(spec);

                AttributeCache.GraphEntry entry = AttributeCache.GRAPHS.byAttribute(c, spec);
                if (entry == null)
                    continue;

                if (!spec.toString().equals(entry.getAttribute().toString()))
                {
                    if (seenMismatch(subsetMismatches, parameterMismatches, entry.getAttribute().getSubset(), entry.getAttribute().getParameters(), spec.getSubset(), spec.getParameters()))
                        continue;

                    errors.reject(ERROR_MSG, _object.getName() + ": Sample " + sample.getLabel() + ": Graph '" + spec + "' has different casing than existing graph '" + entry.getName() + "'.  Please correct the casing before importing.");
                    if (errors.getErrorCount() > 10)
                        break;
                }
            }
        }

        if (!subsetMismatches.isEmpty() && !parameterMismatches.isEmpty())
            _log.debug("Mismatch counts while parsing workspace: " + _object.getName() + "\n" + subsetMismatches.toString() + "\n" + parameterMismatches.toString());
    }

    boolean seenMismatch(Map<SubsetSpec, Integer> subsetMismatches, Map<String, Integer> parameterMismatches, SubsetSpec spec1, String param1, SubsetSpec spec2, String param2)
    {
        return seenMismatch(subsetMismatches, parameterMismatches, spec1, param1 == null ? null : new String[] { param1 }, spec2, param2 == null ? null : new String[] { param2 });
    }

    // we already know there is a mismatch, but we want to identify where it is.  returns true if we've seen this mismatch before.
    boolean seenMismatch(Map<SubsetSpec, Integer> subsetMismatches, Map<String, Integer> parameterMismatches, SubsetSpec spec1, String[] params1, SubsetSpec spec2, String[] params2)
    {
        boolean seenMismatch = false;
        if (spec1 != null && spec2 != null)
        {
            SubsetSpec subsetMismatch = findSubsetMismatch(spec1, spec2);
            if (subsetMismatch != null)
            {
                // skip reporting this mismatch if we've already seen a similar one
                Integer count = subsetMismatches.merge(subsetMismatch, 1, (a, b) -> a+b);
                seenMismatch = count > 1;
            }
        }

        if (params1 != null && params2 != null && params1.length == params2.length)
        {
            for (int i = 0; i < params1.length; i++)
            {
                if (!params1[i].equals(params2[i]))
                {
                    String parameterMismatch = params2[i];
                    // skip reporting this mismatch if we've already seen a similar one
                    Integer count = parameterMismatches.merge(parameterMismatch, 1, (a, b) -> a+b);
                    seenMismatch |= count > 1;
                }
            }
        }

        return seenMismatch;
    }

    // Return the portion of actual that is common between expected and actual and include the mismatched part of actual
    SubsetSpec findSubsetMismatch(SubsetSpec expected, SubsetSpec actual)
    {
        SubsetPart[] expectedParts = expected.getSubsets();
        SubsetPart[] actualParts = actual.getSubsets();
        if (expectedParts.length != actualParts.length)
            return null;

        SubsetSpec common = null;
        for (int i = 0, len = expectedParts.length; i < len; i++)
        {
            SubsetPart expectedPart = expectedParts[i];
            SubsetPart actualPart = actualParts[i];
            common = new SubsetSpec(common, actualPart);
            if (!expectedPart.equals(actualPart))
                return common;
        }

        return null;
    }


    private static IWorkspace readWorkspace(File file, String path) throws WorkspaceValidationException
    {
        try
        {
            if (file.isDirectory() && new File(file, AnalysisSerializer.STATISTICS_FILENAME).isFile())
            {
                return AnalysisSerializer.readAnalysis(file);
            }
            else
            {
                return Workspace.readWorkspace(file);
            }
        }
        catch (IOException e)
        {
            throw new WorkspaceValidationException("Unable to load analysis for '" + path + "': " + e.getMessage(), e);
        }
    }

    public Map<String, String> getHiddenFields()
    {
        if (path != null)
        {
            Map<String, String> ret = new HashMap<>();
            ret.put("path", path);
            if (originalPath != null)
                ret.put("originalPath", originalPath);
            return ret;
        }
        else
        {
            Map<String, String> ret = new HashMap<>();
            if (_object != null)
            {
                try
                {
                    ret.put("object", PageFlowUtil.encodeObject(_object));
                }
                catch (IOException e)
                {
                    throw UnexpectedException.wrap(e);
                }
                ret.put("name", name);

            }
            return ret;
        }
    }

    public static class WorkspaceValidationException extends Exception
    {
        public WorkspaceValidationException()
        {
            super();
        }

        public WorkspaceValidationException(String message)
        {
            super(message);
        }

        public WorkspaceValidationException(String message, Throwable cause)
        {
            super(message, cause);
        }

        public WorkspaceValidationException(Throwable cause)
        {
            super(cause);
        }
    }
}
