/*
 * Copyright (c) 2014-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function removeFilter(filterIdx)
{
    var fieldSet = Ext.getCmp('filtersFieldSet');
    var itemIndex = fieldSet.items.findIndex('filterIdx', filterIdx);
    if (itemIndex > -1)
    {
        var item = fieldSet.items.get(itemIndex);
        fieldSet.remove(item);
        fieldSet.doLayout();
    }
}

function createKeywordFilter(filterIdx, filter)
{
    return {
        xtype:'compositefield', filterIdx: filterIdx, fieldLabel: 'Keyword', items: [
            {xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'keyword'},
            {xtype:'combo', name:'filter[' + filterIdx + '].property', store:FlowPropertySet.keywords, value:filter.property},
            {xtype:'textfield', name:'filter[' + filterIdx + '].value', value:filter.value},
            {xtype:'button', text:'Remove', handler: function () { removeFilter(filterIdx); } }
        ]
    };
}

function addKeywordFilter(e)
{
    var newItem = createKeywordFilter(++form.filterCount, {property:null, value:null});

    var fieldSet = Ext.getCmp('filtersFieldSet');
    var insertAt = fieldSet.items.findIndex('id', 'add-keyword-button');
    fieldSet.insert(insertAt, newItem);
    fieldSet.doLayout();
}

function createSampleFilter(filterIdx, filter)
{
    return {
        xtype:'compositefield', filterIdx: filterIdx, fieldLabel: 'Sample Property', items: [
            {xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'sample'},
            {xtype:'combo', name:'filter[' + filterIdx + '].property', store:SampleSet.properties, value:filter.property},
            {xtype:'textfield', name:'filter[' + filterIdx + '].value', value:filter.value},
            {xtype:'button', text:'Remove', handler: function () { removeFilter(filterIdx); } }
        ]
    };
}

function addSampleFilter()
{
    var newItem = createSampleFilter(++form.filterCount, {property:null, value:null});

    var fieldSet = Ext.getCmp('filtersFieldSet');
    var insertAt = fieldSet.items.findIndex('id', 'add-sample-button');
    fieldSet.insert(insertAt, newItem);
    fieldSet.doLayout();
}

function createStatisticFilter(filterIdx, filter)
{
    return {xtype: 'fieldset', filterIdx: filterIdx, frame: false, border: false, margin: "10px 0 0 0", items: [
        {xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'statistic'},
        {xtype:'statisticField', fieldLabel: 'Statistic', name:'filter[' + filterIdx + '].property', value:filter.property},
        {xtype:'compositefield', items: [
            {xtype:'opCombo', name:'filter[' + filterIdx + '].op', value:filter.op},
            {xtype:'textfield', name:'filter[' + filterIdx + '].value', value:filter.value},
            {xtype:'button', text:'Remove', handler: function () { removeFilter(filterIdx); } }
        ]},
    ]};
}

function addStatisticFilter()
{
    var newItem = createStatisticFilter(++form.filterCount, {property:null, value:null});

    var fieldSet = Ext.getCmp('filtersFieldSet');
    var insertAt = fieldSet.items.findIndex('id', 'add-statistic-button');
    fieldSet.insert(insertAt, newItem);
    fieldSet.doLayout();
}

function createFieldKeyFilter(filterIdx, filter)
{
    return {
        xtype:'compositefield', filterIdx: filterIdx, fieldLabel: 'Field', items: [
            {xtype:'hidden', name:'filter[' + filterIdx + '].type', value:'fieldkey'},
            {xtype:'textfield', name:'filter[' + filterIdx + '].property', value:filter.property, width: 175},
            {xtype:'opCombo', name:'filter[' + filterIdx + '].op', value:filter.op, width: 150},
            {xtype:'textfield', name:'filter[' + filterIdx + '].value', value:filter.value},
            {xtype:'button', text:'Remove', handler: function () { removeFilter(filterIdx); } }
        ]
    };
}

function addFieldKeyFilter()
{
    var newItem = createFieldKeyFilter(++form.filterCount, {property:null, value:null});

    var fieldSet = Ext.getCmp('filtersFieldSet');
    var insertAt = fieldSet.items.findIndex('id', 'add-fieldkey-button');
    fieldSet.insert(insertAt, newItem);
    fieldSet.doLayout();
}

