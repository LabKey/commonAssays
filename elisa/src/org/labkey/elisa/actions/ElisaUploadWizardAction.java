package org.labkey.elisa.actions;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.RequiresPermissionClass;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.study.PlateTemplate;
import org.labkey.api.study.actions.PlateBasedUploadWizardAction;
import org.labkey.api.study.actions.PlateUploadFormImpl;
import org.labkey.elisa.ElisaAssayProvider;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: 10/9/12
 */
@RequiresPermissionClass(InsertPermission.class)
public class ElisaUploadWizardAction extends PlateBasedUploadWizardAction<PlateUploadFormImpl<ElisaAssayProvider>, ElisaAssayProvider>
{
    public ElisaUploadWizardAction()
    {
        addStepHandler(new ConcentrationStepHandler());
    }

    protected PlateConcentrationPropertyHelper createConcentrationPropertyHelper(Container container, ExpProtocol protocol, ElisaAssayProvider provider)
    {
        PlateTemplate template = provider.getPlateTemplate(container, protocol);
        return new PlateConcentrationPropertyHelper(provider.getConcentrationWellGroupDomain(protocol).getProperties(), template);
    }

    public class ConcentrationStepHandler extends RunStepHandler
    {
        public static final String NAME = "CONCENTRATIONS";
        private Map<String, Map<DomainProperty, String>> _postedConcentrationProperties = null;

        @Override
        public String getName()
        {
            return NAME;
        }
    }
}
