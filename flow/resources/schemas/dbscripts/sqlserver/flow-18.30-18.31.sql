
UPDATE flow.object SET uri = 'file:///' + substring(uri, 7, 400) WHERE uri LIKE 'file:/_%' AND uri NOT LIKE 'file:///%' AND uri IS NOT NULL;