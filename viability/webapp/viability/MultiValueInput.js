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
            style: "display:block;",
            listeners: {
                'specialkey': function (f, e) {
                    if (e.getKey() == e.TAB && !e.shiftKey)
                    {
                        var index = fields.indexOf(f);
                        if (index == fields.length - 1)
                        {
                            if (f.getValue()) {
                                self.addInput('', true);
                            } else {
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

    for (var i=0; i < initialValues.length; i++)
    {
        this.addInput(initialValues[i]);
    }
};

