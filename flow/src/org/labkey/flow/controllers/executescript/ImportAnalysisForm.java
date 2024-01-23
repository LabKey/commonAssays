/*
 * Copyright (c) 2008-2018 LabKey Corporation
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
package org.labkey.flow.controllers.executescript;

import org.labkey.api.action.HasAllowBindParameter;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.util.SafeToRenderEnum;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.controllers.WorkspaceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * User: kevink
 * Date: Jul 14, 2008 4:06:04 PM
 */
public class ImportAnalysisForm implements HasAllowBindParameter
{
    public static final String NAME = "importAnalysis";
    
    // unicode small comma (probably not in the gate name so is safer than comma as a separator char in LovCombo)
    public static final String PARAMETER_SEPARATOR = "\ufe50";
    private boolean goBack = false;

    public enum SelectFCSFileOption implements SafeToRenderEnum
    {
        None, Included, Previous, Browse
    }

    private final WorkspaceData workspace = new WorkspaceData();
    private final SelectedSamples selectedSamples = new SelectedSamples();

    private int step = AnalysisScriptController.ImportAnalysisStep.SELECT_ANALYSIS.getNumber();
    private SelectFCSFileOption selectFCSFilesOption = null;
    private boolean keywordRunsExist = false;
    private String importGroupNames = Workspace.ALL_SAMPLES;
    private boolean resolving = false;
    private AnalysisEngine selectAnalysisEngine = null;
    private boolean createAnalysis;
    private String newAnalysisName;
    private int existingAnalysisId;
    private String targetStudy;

    // FCSFile directories selected in the pipeline browser for association with the imported workspace analysis.
    private String[] keywordDir;
    private boolean confirm;

    private boolean studyChanged = false;

    public void setStudyChanged(boolean studyChanged)
    {
        this.studyChanged = studyChanged;
    }

    public boolean getStudyChanged()
    {
        return this.studyChanged;
    }

    public int getStep()
    {
        return step;
    }

    public void setStep(int step)
    {
        this.step = step;
    }

    public AnalysisScriptController.ImportAnalysisStep getWizardStep()
    {
        return AnalysisScriptController.ImportAnalysisStep.fromNumber(step);
    }

    public void setWizardStep(AnalysisScriptController.ImportAnalysisStep step)
    {
        this.step = step.getNumber();
    }

    public WorkspaceData getWorkspace()
    {
        return workspace;
    }

    public boolean isResolving()
    {
        return resolving;
    }

    public void setResolving(boolean resolving)
    {
        this.resolving = resolving;
    }

    public SelectedSamples getSelectedSamples()
    {
        return selectedSamples;
    }

    public SelectFCSFileOption getSelectFCSFilesOption()
    {
        return selectFCSFilesOption;
    }

    public void setSelectFCSFilesOption(SelectFCSFileOption selectFCSFilesOption)
    {
        this.selectFCSFilesOption = selectFCSFilesOption;
    }

    // not a POSTed parameter - For rending the SELECT_FCSFILES step
    public boolean getKeywordRunsExist()
    {
        return keywordRunsExist;
    }

    protected void setKeywordRunsExist(boolean keywordRunsExist)
    {
        this.keywordRunsExist = keywordRunsExist;
    }

    public AnalysisEngine getSelectAnalysisEngine()
    {
        return selectAnalysisEngine;
    }

    public void setSelectAnalysisEngine(AnalysisEngine selectAnalysisEngine)
    {
        this.selectAnalysisEngine = selectAnalysisEngine;
    }

    public List<String> getImportGroupNameList()
    {
        return split(importGroupNames);
    }

    public String getImportGroupNames()
    {
        return importGroupNames;
    }

    public void setImportGroupNames(String importGroupNames)
    {
        this.importGroupNames = importGroupNames;
    }

    public boolean isCreateAnalysis()
    {
        return createAnalysis;
    }

    public void setCreateAnalysis(boolean createAnalysis)
    {
        this.createAnalysis = createAnalysis;
    }

    public String getNewAnalysisName()
    {
        return newAnalysisName;
    }

    public void setNewAnalysisName(String newAnalysisName)
    {
        this.newAnalysisName = newAnalysisName;
    }

    public int getExistingAnalysisId()
    {
        return existingAnalysisId;
    }

    public void setExistingAnalysisId(int existingAnalysisId)
    {
        this.existingAnalysisId = existingAnalysisId;
    }

    public String getTargetStudy()
    {
        return targetStudy;
    }

    public void setTargetStudy(String targetStudy)
    {
        this.targetStudy = targetStudy;
    }

    public String[] getKeywordDir()
    {
        return this.keywordDir;
    }

    public void setKeywordDir(String[] keywordDir)
    {
        this.keywordDir = keywordDir;
    }

    public boolean isConfirm()
    {
        return confirm;
    }

    public void setConfirm(boolean confirm)
    {
        this.confirm = confirm;
    }

    public boolean isGoBack()
    {
        return goBack;
    }

    public void setGoBack(boolean goBack)
    {
        this.goBack = goBack;
    }


    private List<String> split(String list)
    {
        if (list == null)
            return Collections.emptyList();

        List<String> ret = new ArrayList<>();
        for (String s : list.split(PARAMETER_SEPARATOR))
            ret.add(s.trim());
        return ret;
    }


    private static final Pattern pat = Pattern.compile("((workspace)|(selectedSamples\\.rows\\[[^\\]]*\\]))(\\.\\w*)");

    @Override
    public Predicate<String> allowBindParameter()
    {
        return (name) ->
        {
            if (name.startsWith(SpringActionController.FIELD_MARKER))
                name = name.substring(SpringActionController.FIELD_MARKER.length());
            if (HasAllowBindParameter.getDefaultPredicate().test(name))
                return true;
            if (pat.matcher(name).matches())
                return true;
            return false;
        };
    }
}
