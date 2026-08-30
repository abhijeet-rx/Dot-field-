package com.dotfield.matching;

import com.dotfield.entity.RemoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirements {

    @Builder.Default
    private Set<String> requiredSkills = new HashSet<>();

    @Builder.Default
    private Set<String> preferredSkills = new HashSet<>();

    private Integer minimumExperienceYears;
    private String experienceTechnology; // Null if general experience requirement

    private DegreeLevel requiredEducationLevel;
    private String requiredEducationField; // e.g. "computer science", "engineering", null if unspecified
    private String requiredEducation; // Summary string e.g. "Bachelor in Computer Science"

    private String location;
    private RemoteType remoteType;

}
