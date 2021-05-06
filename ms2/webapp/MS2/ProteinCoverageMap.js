if (!LABKEY.ms2) {
    LABKEY.ms2 = {};
}

if (!LABKEY.ms2.ProteinCoverageMap) {
    LABKEY.ms2.ProteinCoverageMap = {

        // add checkbox change event
        registerSelectAll: function() {
            $(".featureCheckboxItem").change(function() {
                if (this.checked) {
                    // if all are manually checked
                    if ($('.featureCheckboxItem:checked').length === $('.featureCheckboxItem').length) {
                        $("#showFeatures").prop('checked', true);
                    }
                } else {
                    $("#showFeatures").prop('checked', false);
                }
            });

            $("#showFeatures").change(function() {
                if (this.checked) {
                    $(".featureCheckboxItem").each(function() {
                        $(this).prop('checked', true).trigger("change");
                    });
                } else {
                    $(".featureCheckboxItem").prop('checked', false).trigger('change');
                }
            });
        },

        toggleStyleColor: function (className, color) {
            if (document.getElementById(className).checked) {
                $('.' + className).css('background-color', color);
            }
            else {
                $("." + className).removeAttr('style');
            }
        }
    }
}
