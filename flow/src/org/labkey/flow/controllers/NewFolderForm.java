package org.labkey.flow.controllers;

import org.labkey.api.view.ViewForm;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

public class NewFolderForm extends ViewForm
{
    public String ff_folderName;
    public Set<String> ff_copyAnalysisScript = Collections.emptySet();
    public boolean ff_copyProtocol;

    public void setFf_folderName(String name)
    {
        ff_folderName = name;
    }

    public void setFf_copyAnalysisScript(String[] analysisScript)
    {
        ff_copyAnalysisScript = new HashSet<String>(Arrays.asList(analysisScript));
    }

    public void setFf_copyProtocol(boolean b)
    {
        ff_copyProtocol = b;
    }
}
