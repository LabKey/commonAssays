
UPDATE flow.object SET uri = 'file:///' || substr(uri, 7) WHERE uri LIKE 'file:/_%' AND uri NOT LIKE 'file:///%' AND uri IS NOT NULL;