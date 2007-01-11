package Flow;

import org.labkey.api.view.ViewForm;
import org.fhcrc.cpas.flow.data.FlowObject;

abstract public class CommentForm extends ViewForm
{
    abstract public FlowObject getObject();
}
