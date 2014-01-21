/*
 * Copyright (c) 2008-2014 LabKey Corporation
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

import org.labkey.api.data.Entity;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.util.MemTracker;

import java.io.Serializable;


public class FeatureExtractionRun extends Entity implements Serializable, Cloneable
{
    protected byte[] _ts;
    protected int rowId;
    protected String barcode;
    protected String grid;
    protected String protocol;
    protected String description;

    protected String path;
    protected String lowResImage;
    protected String qcReport;
    protected String mageML;
    protected String feature;
    protected String alignment;
    protected int statusid;

    public FeatureExtractionRun()
    {
        MemTracker.getInstance().put(this);
    }

    public int getRowId()
    {
        return rowId;
    }

    public void setRowId(int rowId)
    {
        this.rowId = rowId;
    }

    public String getName() {
        return barcode;
    }

    public String getCreatedByName(User currentUser)
    {
        return UserManager.getDisplayName(getCreatedBy(), currentUser);
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getGrid() {
        return grid;
    }

    public void setGrid(String grid) {
        this.grid = grid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLowResImage() {
        return lowResImage;
    }

    public void setLowResImage(String lowResImage) {
        this.lowResImage = lowResImage;
    }

    public String getQcReport() {
        return qcReport;
    }

    public void setQcReport(String qcReport) {
        this.qcReport = qcReport;
    }

    public String getMageML() {
        return mageML;
    }

    public void setMageML(String mageML) {
        this.mageML = mageML;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getAlignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public int getStatusId() {
        return statusid;
    }

    public void setStatusId(int statusid) {
        this.statusid = statusid;
    }


}
