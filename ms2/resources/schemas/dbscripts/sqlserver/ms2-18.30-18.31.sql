
UPDATE ms2.fractions SET MzXmlUrl = 'file:///' + substring(MzXmlUrl, 7, 400) WHERE MzXmlUrl LIKE 'file:/_%' AND MzXmlUrl NOT LIKE 'file:///%' AND MzXmlUrl IS NOT NULL;