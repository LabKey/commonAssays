/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.microarray;

import jxl.Workbook;
import jxl.write.*;
import org.labkey.api.exp.ProtocolParameter;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.study.actions.BaseAssayAction;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.util.DateUtil;
import org.labkey.api.view.NotFoundException;
import org.labkey.microarray.assay.MicroarrayAssayProvider;
import org.labkey.microarray.designer.client.MicroarrayAssayDesigner;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: jeckels
 * Date: Feb 2, 2009
 */
@RequiresPermission(ACL.PERM_READ)
public class MicroarrayBulkPropertiesTemplateAction extends BaseAssayAction<MicroarrayRunUploadForm>
{
    public ModelAndView getView(MicroarrayRunUploadForm form, BindException errors) throws Exception
    {
        ExpProtocol protocol = form.getProtocol(true);

        AssayProvider p = form.getProvider();
        if (!(p instanceof MicroarrayAssayProvider))
        {
            throw new NotFoundException("Could not find microarray assay provider");
        }
        MicroarrayAssayProvider provider = (MicroarrayAssayProvider)p;

        Domain runDomain = provider.getRunDomain(protocol);
        List<DomainProperty> propertiesToCollect = new ArrayList<DomainProperty>(Arrays.asList(runDomain.getProperties()));

        getViewContext().getResponse().reset();

        // First, set the content-type, so that your browser knows which application to launch
        getViewContext().getResponse().setContentType("application/vnd.ms-excel");
        String filename = protocol.getName() + "Template" + DateUtil.formatDate() + ".xls";
        getViewContext().getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        WritableWorkbook workbook = Workbook.createWorkbook(getViewContext().getResponse().getOutputStream());
        WritableSheet sheet = workbook.createSheet("MicroarrayTemplate", 0);
        int col = 0;

        WritableFont boldFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
        WritableCellFormat cellFormat = new WritableCellFormat(boldFont);
        cellFormat.setWrap(false);
        cellFormat.setVerticalAlignment(jxl.format.VerticalAlignment.TOP);

        ProtocolParameter cy3Param = protocol.getProtocolParameters().get(MicroarrayAssayDesigner.CY3_SAMPLE_NAME_COLUMN_PARAMETER_URI);
        String cy3Name;
        if (cy3Param != null)
        {
            cy3Name = cy3Param.getStringValue();
        }
        else
        {
            cy3Name = "Sample1";
        }

        ProtocolParameter cy5Param = protocol.getProtocolParameters().get(MicroarrayAssayDesigner.CY5_SAMPLE_NAME_COLUMN_PARAMETER_URI);
        String cy5Name;
        if (cy5Param != null)
        {
            cy5Name = cy5Param.getStringValue();
        }
        else
        {
            cy5Name = "Sample2";
        }

        sheet.addCell(new Label(col++, 0, "Barcode", cellFormat));
        sheet.addCell(new Label(col++, 0, cy3Name, cellFormat));
        sheet.addCell(new Label(col++, 0, cy5Name, cellFormat));

        for (DomainProperty domainProperty : propertiesToCollect)
        {
            sheet.addCell(new Label(col++, 0, domainProperty.getName(), cellFormat));
        }

        for (int i = 0; i < col; i++)
        {
            sheet.setColumnView(i, 15);
        }

        int row = 1;
        List<Map<String, File>> allFiles = form.getSelectedDataCollector().getFileCollection(form);
        for (Map<String, File> files : allFiles)
        {
            for (File file : files.values())
            {
                Document doc = form.getMageML(file);
                sheet.addCell(new Label(0, row++, form.getBarcode(doc)));
            }
        }

        workbook.write();
        getViewContext().getResponse().getOutputStream().flush();
        workbook.close();
        return null;
    }
}