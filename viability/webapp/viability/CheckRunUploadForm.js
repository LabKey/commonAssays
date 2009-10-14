function checkRunUploadForm(form)
{
    var suffixes = [ "ParticipantID", "VisitID", "SpecimenIDs" ];
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

                    var msg = "Missing " + suffix + " value";
                    if (sampleNum)
                    {
                        msg += " for sample number " + sampleNum;
                    }
                    msg += ".  Save anyway?";

                    return confirm(msg);
                }
            }
        }
    }
    return true;
}