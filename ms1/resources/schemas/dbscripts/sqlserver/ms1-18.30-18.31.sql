
UPDATE ms1.files SET MzXmlUrl = 'file:///' + substring(MzXmlUrl, 7, 800) WHERE MzXmlUrl LIKE 'file:/_%' AND MzXmlUrl NOT LIKE 'file:///%' AND MzXmlUrl IS NOT NULL;