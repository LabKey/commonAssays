/*
 * Copyright (c) 2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var MultiValueInput = function (fieldId, initialValues)
{
    var dom = document.getElementById(fieldId);
    var fields = [];
    var self = this;

    this.addInput = function (value, getfocus) {
        var field = new Ext.form.TextField({
            name: fieldId,
            editable: true,
            value: value,
            style: "display:block;font-family:monospace",
            listeners: {
                'specialkey': function (f, e) {
                    var key = e.getKey();
                    if ((key == e.TAB || key == e.ENTER) && !e.shiftKey)
                    {
                        if (key == e.ENTER)
                            e.stopEvent();
                        var index = fields.indexOf(f);
                        if (index == fields.length - 1)
                        {
                            if (f.getValue()) {
                                self.addInput('', true);
                            } else if (fields.length > 1) {
                                fields.pop();
                                // defer destroy so focus is moved to next element
                                f.destroy.defer(10, f);
                            }
                        }
                    }
                }
            }
        });

        fields.push(field);
        field.render(dom);
        if (getfocus)
            field.focus(true, 10);

    };

    if (initialValues === undefined || initialValues.length == 0)
    {
        this.addInput('');
    }
    else
    {
        for (var i=0; i < initialValues.length; i++)
        {
            this.addInput(initialValues[i]);
        }
    }
};

