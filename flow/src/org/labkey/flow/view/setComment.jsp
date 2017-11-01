<%
/*
 * Copyright (c) 2008-2017 LabKey Corporation
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
%>
<%@ page import="org.labkey.api.exp.api.ExperimentUrls"%>
<%@ page import="org.labkey.api.security.permissions.UpdatePermission"%>
<%@ page import="org.labkey.api.util.PageFlowUtil" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.flow.data.FlowObject" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>

<%
    FlowObject flowObj = (FlowObject)getModelBean();
    ActionURL setFlagUrl = urlProvider(ExperimentUrls.class).getSetFlagURL(getContainer());
    setFlagUrl.addParameter("lsid", flowObj.getLSID());
    setFlagUrl.addParameter("redirect", false);

    boolean canEdit = getViewContext().hasPermission(UpdatePermission.class);
    String contextPath = getViewContext().getContextPath();
%>
<% if (canEdit) { %>
<span class="extContainer x-form-field-wrap">
    <input class="extContainer" type="text"
           id="comment" name="comment" size="65"
           value="<%=h(flowObj.getExpObject().getComment())%>" />
</span>
<script type="text/javascript">
LABKEY.requiresExt3(function() {
    var CommentField = Ext.extend(Ext.form.TextField, {
        initComponent : function () {
            CommentField.superclass.initComponent.call(this);
        },

        initEvents : function () {
            CommentField.superclass.initEvents.call(this);
            this._originalValue = this.getValue();
            this.on('change', this.doSubmit, this);
            this.on('specialkey', this.handleSpecial, this);
        },

        handleSpecial : function (self, e) {
            var key = e.getKey();
            if (key == Ext.EventObject.ESC)
            {
                this.setValue(this._originalValue);
            }
            else if (key == Ext.EventObject.ENTER)
            {
                this.doSubmit();
            }
        },

        doSubmit : function () {
            this.hideStatus();
            if (this.getValue() !== this._originalValue)
            {
                var newValue = this.getValue();

                this.statusMessage('loading', "Updating...");
                Ext.Ajax.request({
                    url: "<%=setFlagUrl%>&comment=" + encodeURIComponent(textField.getValue()),
                    success: function (response) {
                        this.statusMessage('success', "Comment updated");
                        this._originalValue = newValue;
                    },
                    failure: function (response) {
                        this.statusMessage('error', "Error updating comment: " + response.responseText);
                    },
                    scope: this
                });
            }
        },

        hideStatus : function () {
            if (this.statusEl)
            {
                this.statusEl.hide();
                this.un('resize', this.alignStatusIcon, this);
            }
            if (this.delayHide)
            {
                this.delayHide.cancel();
                delete this.delayHide;
            }
        },

        statusMessage : function (status, msg) {
            if (!this.rendered)
                return;
            if (!this.statusEl)
            {
                var elp = this.getErrorCt();
                if (!elp) return;
                this.statusEl = elp.createChild({
                    style: {
                        position: "absolute",
                        left: "0px", top: "0px",
                        width: "16px", height: "18px",
                        'padding-left': "18px",
                        font: "normal 11px tahoma, arial, helvetica, sans-serif",
                        'line-height': "18px",
                        display: "block",
                        visibility: "hidden",
                        background: "transparent url(<%=contextPath%>/_.gif) no-repeat 0 2px"
                    }
                });
                this.alignStatusIcon();
                this.on('resize', this.alignStatusIcon, this);
            }

            switch (status)
            {
                case 'loading':
                    this.statusEl.setStyle("background-image", "url(<%=contextPath%>/<%=PageFlowUtil.extJsRoot()%>/resources/images/default/grid/loading.gif)");
                    this.statusEl.setStyle("color", "silver");
                    break;
                case 'success':
                    this.statusEl.setStyle("background-image", "url(<%=contextPath%>/<%=PageFlowUtil.extJsRoot()%>/resources/images/default/tree/drop-yes.gif)");
                    this.statusEl.setStyle("color", "green");
                    if (!this.delayHide)
                    {
                        this.delayHide = new Ext.util.DelayedTask(function () { this.statusEl.hide({duration:0.6}); }, this);
                    }
                    this.delayHide.delay(4000);
                    break;
                case 'error':
                    this.statusEl.setStyle("background-image", "url(<%=contextPath%>/<%=PageFlowUtil.extJsRoot()%>/resources/images/default/form/exclamation.gif)")
                    this.statusEl.setStyle("color", "red");
                    break;
            }

            this.statusEl.update(msg);
            this.statusEl.show();
        },

        alignStatusIcon : function () {
            this.statusEl.alignTo(this.el, 'tl-tr', [2, 0]);
        }
    });
    var textField = new CommentField({
        applyTo: 'comment',
        emptyText: "Type to enter a comment",
        fieldLabel: 'Comment',
        msgTarget: 'side',
        labelLength: 65
    });
});
</script>
<% } else { %>
<%=h(flowObj.getExpObject().getComment())%>
<% } %>
