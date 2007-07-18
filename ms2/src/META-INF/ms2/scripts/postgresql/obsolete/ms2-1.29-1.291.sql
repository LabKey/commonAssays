-- Update GeneCards URL
UPDATE prot.ProtInfoSources SET Url = 'http://www.genecards.org/cgi-bin/carddisp?{}&alias=yes'
    WHERE Name = 'GeneCards';