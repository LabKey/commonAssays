/*
 * Copyright (c) 2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
function checkRunUploadForm(form)
{
    var suffixes = [ "ParticipantID", "VisitID", "SpecimenIDs" ];
    var haserrors = false;
    var errors = { };
    var len = form.length;
    for (var i = 0; i < len; i++)
    {
        var element = form.elements[i];
        for (var j in suffixes)
        {
            var suffix = suffixes[j];
            if (element.name && element.name.indexOf("_pool_") == 0 && element.name.indexOf(suffix) == element.name.length - suffix.length)
            {
                if (!element.value)
                {
                    haserrors = true;
                    var prefix = element.name.substring(0, element.name.length - suffix.length);
                    var sampleNum = null;
                    try {
                        var sampleNumElt = form.elements[prefix + "SampleNum"][0];
                        if (sampleNumElt)
                            sampleNum = sampleNumElt.value;
                    }
                    catch (e) {
                        // ignore.
                    }

                    if (!errors[sampleNum])
                        errors[sampleNum] = [];
                    errors[sampleNum].push(suffix);
                }
            }
        }
    }

    var submit = true;
    if (haserrors)
    {
        var msg = "Some values are missing for the following pools:\n\n";
        for (var sampleNum in errors)
        {
            var list = errors[sampleNum];
            msg += "  Sample number " + sampleNum + ": " + (list.join(", ")) + "\n";
        }
        msg += "\nSave anyway?";
        submit = confirm(msg);
    }

    if (submit)
        LABKEY.setDirty(false);
    else {
        // user canceled so reactivate the form buttons by removing their style labkey-disabled-button
        Ext4.get(Ext4.DomQuery.select('table tbody tr td div span a'), form).removeCls("labkey-disabled-button");
    }
    return submit;
}

LABKEY.setDirty(true);
window.onbeforeunload = LABKEY.beforeunload();
