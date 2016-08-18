-- Create a new set of properties for Mascot settings
INSERT INTO prop.propertysets (category, objectid, userid) SELECT 'MascotConfig' AS Category, EntityId, -1 AS UserId FROM core.containers WHERE parent IS NULL;

-- Migrate existing Mascot settings
UPDATE prop.properties SET "set" = (SELECT MAX("set") FROM prop.propertysets)
  WHERE name LIKE 'Mascot%' AND "set" = (SELECT "set" FROM prop.propertysets WHERE category = 'SiteConfig' AND userid = -1 AND objectid = (SELECT entityid FROM core.containers WHERE parent IS NULL));
