package com.dotfield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Education> education = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
        skill.setProfile(this);
    }

    public void removeSkill(Skill skill) {
        skills.remove(skill);
        skill.setProfile(null);
    }

    public void addEducation(Education edu) {
        education.add(edu);
        edu.setProfile(this);
    }

    public void removeEducation(Education edu) {
        education.remove(edu);
        edu.setProfile(null);
    }

    public void addProject(Project project) {
        projects.add(project);
        project.setProfile(this);
    }

    public void removeProject(Project project) {
        projects.remove(project);
        project.setProfile(null);
    }

    public void addExperience(Experience exp) {
        experience.add(exp);
        exp.setProfile(this);
    }

    public void removeExperience(Experience exp) {
        experience.remove(exp);
        exp.setProfile(null);
    }
}
