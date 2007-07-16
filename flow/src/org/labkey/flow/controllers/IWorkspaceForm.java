package org.labkey.flow.controllers;

import org.labkey.flow.analysis.model.FlowJoWorkspace;

import java.util.Map;

public interface IWorkspaceForm
{
    Map<String, String> getHiddenFields() throws Exception;
    FlowJoWorkspace getWorkspace() throws Exception;
}
