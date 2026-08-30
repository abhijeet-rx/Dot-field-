package com.dotfield.repository;

import com.dotfield.entity.EmploymentType;
import com.dotfield.entity.Job;
import com.dotfield.entity.JobStatus;
import com.dotfield.entity.RemoteType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> withFilters(
            JobStatus status,
            String company,
            String source,
            RemoteType remoteType,
            EmploymentType employmentType) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (company != null && !company.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("company")), "%" + company.trim().toLowerCase() + "%"));
            }

            if (source != null && !source.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("source")), source.trim().toUpperCase()));
            }

            if (remoteType != null) {
                predicates.add(cb.equal(root.get("remoteType"), remoteType));
            }

            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
