package org.labkey.flow.controllers;

import org.labkey.api.view.ViewForm;
import org.labkey.flow.data.FlowObject;

abstract public class CommentForm extends ViewForm
{
    abstract public FlowObject getObject();
}
