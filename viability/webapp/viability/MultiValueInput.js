/*
 * Copyright (c) 2009-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var MultiValueInput = function (fieldId, initialValues)
{
    var dom = document.getElementById(fieldId);
    var fields = [];
    var self = this;

    // collect all MultiValueInput form inputs and return the index of the passed in input.
    function collectInputs(input, inputs)
    {
        var form = input.form;
        var len = form.elements.length;
        var index = -1;
        for (var i = 0; i < len; i++)
        {
            var node = form.elements[i];
            if (node.className.indexOf('labkey-multi-value-input') != -1)
            {
                var count = inputs.push(node);
                if (node == input)
                    index = count-1;
            }
        }
        return index;
    }

    // move focus down to next 'labkey-multi-value-input' field
    function goDown(input)
    {
        var inputs = [];
        var index = collectInputs(input, inputs);
        if (index > -1 && index < inputs.length - 1)
        {
            var node = inputs[index+1];
            if (node)
                node.focus();
        }
    }

    // move focus up to previous 'labkey-multi-value-input' field
    function goUp(input)
    {
        var inputs = [];
        var index = collectInputs(input, inputs);
        if (index > 0 && index < inputs.length)
        {
            var node = inputs[index-1];
            if (node)
                node.focus();
        }
    }

    this.addInput = function (value, getfocus) {
        var field = new Ext.form.TextField({
            name: fieldId,
            editable: true,
            value: value,
            cls: "labkey-multi-value-input",
            listeners: {
                'specialkey': function (f, e) {
                    var key = e.getKey();
                    if ((key == e.TAB || key == e.ENTER || key == e.DOWN) && !e.shiftKey)
                    {
                        if (key == e.ENTER)
                            e.stopEvent();

                        var index = fields.indexOf(f);
                        if (index == fields.length - 1)
                        {
                            if (f.getValue()) {
                                // if last field has a value, add a new field
                                self.addInput('', true);
                            } else if (fields.length > 1) {
                                // if last field is empty, remove it
                                fields.pop();
                                // defer destroy so focus is moved to next element on TAB
                                f.destroy.defer(10, f);
                            }
                        }

                        if (key == e.DOWN)
                            goDown(f.el.dom);
                    }
                    else if (key == e.UP)
                    {
                        var index = fields.indexOf(f);
                        if (index == fields.length - 1)
                        {
                            if (!(f.getValue()) && fields.length > 1) {
                                // if last field is empty, remove it
                                fields.pop();
                                // defer destroy so focus is moved to next element on TAB
                                f.destroy.defer(10, f);
                            }
                        }

                        goUp(f.el.dom);
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

