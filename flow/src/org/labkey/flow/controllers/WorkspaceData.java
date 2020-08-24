/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.security.User;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SessionHelper;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.analysis.model.Analysis;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.analysis.model.IWorkspace;
import org.labkey.flow.analysis.model.SubsetPart;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.analysis.web.GraphSpec;
import org.labkey.flow.analysis.web.SpecBase;
import org.labkey.flow.analysis.web.StatisticSpec;
import org.labkey.flow.analysis.web.SubsetSpec;
import org.labkey.flow.data.FlowProtocol;
import org.labkey.flow.persist.AnalysisSerializer;
import org.labkey.flow.persist.AttributeCache;
import org.springframework.validation.Errors;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.labkey.api.action.SpringActionController.ERROR_MSG;

public class WorkspaceData implements Serializable
{
    private static final Logger _log = LogManager.getLogger(WorkspaceData.class);

    String path;
    String name;
    String originalPath;
    String token;
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

    // On form POST, get the workspace from session using the token
    public void setToken(String token) throws Exception
    {
        this.token = token;
        if (token != null)
        {
            var request = HttpView.currentRequest();
            if (request != null)
            {
                _object = (IWorkspace)SessionHelper.getStashedAttribute(request, token);
            }
        }
    }

    public void clearStashedWorkspace(HttpServletRequest request)
    {
        if (token != null)
            SessionHelper.clearStashedAttribute(request, token);
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

    public void validate(User user, Container container, Errors errors, HttpServletRequest request)
    {
        try
        {
            validate(user, container, errors);
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

    public void validate(User user, Container container, Errors errors) throws Exception
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

                FlowProtocol protocol = FlowProtocol.ensureForContainer(user,  container);
                validateKeywordCasing(container, errors, protocol.isCaseSensitiveKeywords());
                validateStatAndGraphCasing(container, errors, protocol.isCaseSensitiveStatsAndGraphs());
            }
            else
            {
                throw new WorkspaceValidationException("No workspace file was specified.");
            }
        }
    }

    private void validateKeywordCasing(Container c, Errors errors, boolean isCaseSensitive)
    {
        List<String> messages = new ArrayList<>(10);
        Set<String> seenKeyword = new HashSet<>();

        for (ISampleInfo sample : _object.getSamples())
        {
            for (String keyword : sample.getKeywords().keySet())
            {
                checkAttribute(c, messages, sample, keyword, AttributeCache.KEYWORDS, seenKeyword, null, null);
                if (messages.size() > 10)
                    break;
            }
        }

        addCaseSensitiveWarnings(errors, messages, isCaseSensitive);
    }

    private void validateStatAndGraphCasing(Container c, Errors errors, boolean isCaseSensitive)
    {
        List<String> messages = new ArrayList<>(10);
        Set<StatisticSpec> seenStat = new HashSet<>();
        Set<GraphSpec> seenGraph = new HashSet<>();

        Map<SubsetSpec, Integer> subsetMismatches = new HashMap<>();
        Map<String, Integer> parameterMismatches = new HashMap<>();

        for (ISampleInfo sample : _object.getSamples())
        {
            Analysis analysis = _object.getSampleAnalysis(sample);
            if (analysis == null)
                continue;

            for (StatisticSpec spec : analysis.getStatistics())
            {
                checkAttribute(c, messages, sample, spec, AttributeCache.STATS, seenStat, subsetMismatches, parameterMismatches);
                if (messages.size() > 10)
                    break;
            }

            for (GraphSpec spec : analysis.getGraphs())
            {
                checkAttribute(c, messages, sample, spec, AttributeCache.GRAPHS, seenGraph, subsetMismatches, parameterMismatches);
                if (messages.size() > 10)
                    break;
            }
        }

        if (!subsetMismatches.isEmpty() && !parameterMismatches.isEmpty())
            _log.debug("Mismatch counts while parsing workspace: " + _object.getName() + "\n" + subsetMismatches.toString() + "\n" + parameterMismatches.toString());

        addCaseSensitiveWarnings(errors, messages, isCaseSensitive);
    }

    <Z extends Comparable<Z>> void checkAttribute(
            Container c, List<String> messages, ISampleInfo sample, Z attr, AttributeCache cache, Set<Z> seen,
            Map<SubsetSpec, Integer> subsetMismatches, Map<String, Integer> parameterMismatches)
    {
        // Skip checking if we've already seen this attribute
        if (seen.contains(attr))
            return;
        seen.add(attr);

        // If we don't have a match, one will be created during the import process
        AttributeCache.Entry entry = cache.byAttribute(c, attr);
        if (entry == null)
            return;

        // If we have an existing attribute, check if the casing matches
        String attrString = attr.toString();
        if (!matchesCasing(entry, attrString))
        {
            if (attr instanceof SpecBase)
            {
                SpecBase existing = (SpecBase)entry.getAttribute();
                // don't report the same error again
                if (seenMismatch(subsetMismatches, parameterMismatches, existing.getSubset(), existing.getParameters(), ((SpecBase)attr).getSubset(), ((SpecBase)attr).getParameters()))
                    return;
            }

            messages.add(_object.getName() + ": Sample " + sample.getLabel() + ": " + entry.getType() + " '" + attrString + "' has different casing than existing entry '" + entry.getAttribute() + "'." +
                    " Please consider correcting the casing before importing or create '" + attrString + "' as an alias for '" + entry.getAttribute() + "'.");
        }
    }

    // Issue 37449: Case-sensitivity on flow gates ignores aliasing
    // Check if the casing of the attribute string matches the attribute entry or one of it's aliases
    boolean matchesCasing(AttributeCache.Entry entry, String attrString)
    {
        // Check that the entry's attribute matches the expected value
        if (attrString.equals(entry.getAttribute().toString()))
            return true;

        // If the passed in attribute isn't the preferred attribute, fetch it now.
        AttributeCache.Entry preferred = entry.getAliasedEntry();

        // Check that any of the preferred entry's aliases matches the expected value
        Collection<AttributeCache.Entry> aliases = preferred != null ? preferred.getAliases() : entry.getAliases();
        for (AttributeCache.Entry alias : aliases)
        {
            if (attrString.equals(alias.getAttribute().toString()))
                return true;
        }

        // no match found
        return false;
    }

    // Add the messages to either the Errors collection or the workspace's warning list
    private void addCaseSensitiveWarnings(Errors errors, List<String> messages, boolean isCaseSensitive)
    {
        if (messages.isEmpty())
            return;

        if (isCaseSensitive)
        {
            for (String msg : messages)
                errors.reject(ERROR_MSG, msg);
        }
        else
        {
            _object.getWarnings().addAll(messages);
        }
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

    public Map<String, String> getHiddenFields(ViewContext context)
    {
        Map<String, String> ret = new HashMap<>();
        if (path != null)
        {
            ret.put("path", path);
            if (originalPath != null)
                ret.put("originalPath", originalPath);
        }

        if (_object != null && token == null)
        {
            // Stash the workspace in session and reference it via a token
            this.token = SessionHelper.stashAttribute(context.getRequest(), _object, TimeUnit.MINUTES.toMillis(10));
            ret.put("token", token);
            ret.put("name", name);
        }

        return ret;
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
