package Flow.Protocol;

import Flow.BaseFlowController;
import org.apache.beehive.netui.pageflow.annotations.Jpf;
import org.apache.beehive.netui.pageflow.Forward;
import org.labkey.api.security.ACL;
import org.labkey.api.jsp.FormPage;
import org.labkey.api.query.api.FieldKey;
import org.labkey.api.view.ViewForward;
import org.fhcrc.cpas.flow.data.FlowProtocol;

import java.util.Map;
import java.util.LinkedHashMap;

@Jpf.Controller
public class ProtocolController extends BaseFlowController<ProtocolController.Action>
{
    public enum Action
    {
        begin,
        showProtocol,
        joinSampleSet,
        updateSamples,
    }

    @Jpf.Action
    protected Forward begin(ProtocolForm form) throws Exception
    {
        FlowProtocol protocol = form.getProtocol();
        if (protocol != null)
        {
            return new ViewForward(protocol.urlShow());
        }
        return null;
    }

    @Jpf.Action
    protected Forward showProtocol(ProtocolForm form) throws Exception
    {
        requiresPermission(ACL.PERM_READ);
        return renderInTemplate(FormPage.getView(ProtocolController.class, form, "showProtocol.jsp"), form.getProtocol(), "Protocol", Action.showProtocol);
    }

    @Jpf.Action
    protected Forward joinSampleSet(JoinSampleSetForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        if (isPost())
        {
            Map<String, FieldKey> fields = new LinkedHashMap();
            for (int i = 0; i < form.ff_samplePropertyURI.length; i ++)
            {
                String samplePropertyURI = form.ff_samplePropertyURI[i];
                FieldKey fcsKey = form.ff_dataField[i];
                if (samplePropertyURI == null || fcsKey == null)
                    continue;
                fields.put(samplePropertyURI, fcsKey);
            }
            form.getProtocol().setSampleSetJoinFields(getUser(), fields);
            return new ViewForward(form.getProtocol().urlFor(Action.updateSamples));
        }
        return renderInTemplate(FormPage.getView(ProtocolController.class, form, "joinSampleSet.jsp"), form.getProtocol(), "Join Samples", Action.joinSampleSet);
    }

    @Jpf.Action
    protected Forward updateSamples(UpdateSamplesForm form) throws Exception
    {
        requiresPermission(ACL.PERM_UPDATE);
        form.fileCount = form.getProtocol().updateSampleIds(getUser());
        return renderInTemplate(FormPage.getView(ProtocolController.class, form, "updateSamples.jsp"), form.getProtocol(), "Update Samples", Action.updateSamples);
    }

}
