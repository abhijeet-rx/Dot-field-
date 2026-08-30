package com.dotfield.discovery;

import com.dotfield.dto.JobDiscoveryRequest;
import com.dotfield.dto.RawJobListing;

import java.util.List;

public interface JobSource {

    String getSourceName();

    boolean supports(String source);

    List<RawJobListing> discover(JobDiscoveryRequest request);

}
