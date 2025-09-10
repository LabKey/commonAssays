/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

package org.labkey.signaldata;

import org.labkey.api.action.ApiResponse;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.ReadOnlyApiAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.files.FileContentService;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.util.FileUtil;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.signaldata.assay.SignalDataAssayDataHandler;
import org.labkey.vfs.FileLike;
import org.springframework.validation.BindException;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.labkey.api.files.FileContentService.UPLOADED_FILE;

public class SignalDataController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(SignalDataController.class);
    static final String NAME = "signaldata";

    public SignalDataController()
    {
        setActionResolver(_actionResolver);
    }


    /**
     * Meant to mimic PipelineController.getPipelineContainerAction but with the incorporated SignalData path context
     */
    @RequiresPermission(ReadPermission.class)
    public static class getSignalDataPipelineContainerAction extends ReadOnlyApiAction<Object>
    {
        @Override
        public ApiResponse execute(Object form, BindException errors) throws Exception
        {
            ApiSimpleResponse resp = new ApiSimpleResponse();
            PipeRoot root = PipelineService.get().findPipelineRoot(getContainer());

            String containerPath = null;
            URI webdavURL = null;

            if (null != root)
            {
                containerPath = root.getContainer().getPath();
                webdavURL = root.getWebdavURL();
                webdavURL = webdavURL.resolve(SignalDataAssayDataHandler.NAMESPACE);

                //Create folder if needed
                FileLike sdFileRoot = root.getRootFileLike().resolveChild(SignalDataAssayDataHandler.NAMESPACE);
                if(!sdFileRoot.exists())
                    sdFileRoot.mkdirs();
            }

            resp.put("containerPath", containerPath);
            resp.put("webDavURL", Objects.toString(webdavURL, null));

            return resp;
        }
    }

    public static class SignalDataResourceForm
    {
        private List<String> _paths;
        private List<String> _files;

        public List<String> getFiles()
        {
            return _files;
        }

        public void setFiles(List<String> files)
        {
            _files = files;
        }

        public List<String> getPaths()
        {
            return _paths;
        }

        public void setPaths(List<String> paths)
        {
            _paths = paths;
        }
    }

    @RequiresPermission(UpdatePermission.class)
    public static class getSignalDataResourceAction extends MutatingApiAction<SignalDataResourceForm>
    {
        @Override
        public ApiResponse execute(SignalDataResourceForm form, BindException errors) throws Exception
        {
            List<Map<String, String>> results = new ArrayList<>();
            Container c = getContainer();
            FileContentService svc = FileContentService.get();
            TableInfo ti = ExpSchema.TableType.Data.createTable(new ExpSchema(getUser(), c), ExpSchema.TableType.Data.toString(), null);
            QueryUpdateService qus = ti.getUpdateService();
            int maxUrlSize = ExperimentService.get().getTinfoData().getColumn("DataFileURL").getScale();
            int idx = 0;

            for (String path : form.getPaths())
            {
                WebdavResource resource = WebdavService.get().lookup(path);
                String fileName = form.getFiles().get(idx++);

                if (null != resource)
                {
                    ExpData data = svc.getDataObject(resource, c);
                    if (data == null)
                    {
                        // create the ExpData object if it doesn't already exist
                        File file = resource.getFile();
                        if (null != file)
                        {
                            data = ExperimentService.get().createData(c, UPLOADED_FILE);
                            data.setName(file.getName());
                            data.setDataFileURI(file.toURI());

                            String dataFileURL = data.getDataFileUrl();
                            if (dataFileURL == null || dataFileURL.length() <= maxUrlSize)
                            {
                                data.save(getUser());
                            }
                            else
                            {
                                throw new ValidationException(String.format("The data file URL is too long to store in the database (max %d).", maxUrlSize));
                            }
                        }
                    }

                    if (null != data)
                    {
                        File canonicalFile = FileUtil.getAbsoluteCaseSensitiveFile(resource.getFile());
                        String url = canonicalFile.toURI().toURL().toString();
                        List<Map<String, Object>> rows = qus.getRows(getUser(), c, Collections.singletonList(Map.of(ExpDataTable.Column.DataFileUrl.name(), url)));

                        if (rows.size() == 1)
                        {
                            Map<String, String> props = new HashMap<>();
                            props.put("FilePath", path);
                            props.put("FileName", fileName);
                            results.add(props);
                            for (Map.Entry<String, Object> entry : rows.get(0).entrySet())
                            {
                                Object value = entry.getValue();
                                if (null != value)
                                    props.put(entry.getKey(), String.valueOf(value));
                            }
                        }
                        else
                            throw new RuntimeException(String.format("Unexpected number of rows returned for DataFileUrl '%s': %d", url, rows.size()));
                    }
                }
            }
            return new ApiSimpleResponse(Map.of("files", results));
        }
    }
}