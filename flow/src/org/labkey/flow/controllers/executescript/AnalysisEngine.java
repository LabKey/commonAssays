package org.labkey.flow.controllers.executescript;

/**
* User: kevink
* Date: 10/21/12
*/
public enum AnalysisEngine
{
    // LabKey's analysis engine
    LabKey(true),

    // Analysis results read by LabKey's FlowJo workspace parser
    FlowJoWorkspace(false),

    // Execute external FlowJo process
    FlowJo(true),

    // Execute external R process
    R(true),

    // Generic external analysis archive
    Archive(false);

    private boolean _requiresPipeline;

    AnalysisEngine(boolean requiresPipeline)
    {
        _requiresPipeline = requiresPipeline;
    }

    public boolean requiresPipeline()
    {
        return _requiresPipeline;
    }
}
