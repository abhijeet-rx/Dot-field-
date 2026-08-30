package com.dotfield.entity;

import com.dotfield.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JobEntityTest {

    @Autowired
    private JobRepository jobRepository;

    @Test
    void jobEntity_prePersist_setsDefaultsAndTimestamps() {
        Job job = Job.builder()
                .title("Platform Engineer")
                .company("Uber")
                .build();

        Job savedJob = jobRepository.saveAndFlush(job);

        assertNotNull(savedJob.getId());
        assertNotNull(savedJob.getCreatedAt());
        assertNotNull(savedJob.getUpdatedAt());
        assertEquals(JobStatus.SAVED, savedJob.getStatus());
        assertEquals("MANUAL", savedJob.getSource());
    }

    @Test
    void jobEntity_prePersist_normalizesSourceToUppercase() {
        Job job = Job.builder()
                .title("Platform Engineer")
                .company("Uber")
                .source("linkedin")
                .build();

        Job savedJob = jobRepository.saveAndFlush(job);

        assertEquals("LINKEDIN", savedJob.getSource());
    }

    @Test
    void jobEntity_preUpdate_updatesTimestamp() throws InterruptedException {
        Job job = Job.builder()
                .title("Security Engineer")
                .company("Cloudflare")
                .salaryMin(new BigDecimal("130000.00"))
                .build();

        Job savedJob = jobRepository.saveAndFlush(job);
        var initialCreatedAt = savedJob.getCreatedAt();
        var initialUpdatedAt = savedJob.getUpdatedAt();

        Thread.sleep(20);

        savedJob.setTitle("Lead Security Engineer");
        Job updatedJob = jobRepository.saveAndFlush(savedJob);

        assertEquals(initialCreatedAt, updatedJob.getCreatedAt());
        assertTrue(updatedJob.getUpdatedAt().isAfter(initialUpdatedAt));
    }
}
