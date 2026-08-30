package com.dotfield.extractor;

import java.util.Map;

public interface JobExtractor {

    boolean supports(String source);

    ExtractedJob extract(Map<String, Object> rawData, String source);

}
