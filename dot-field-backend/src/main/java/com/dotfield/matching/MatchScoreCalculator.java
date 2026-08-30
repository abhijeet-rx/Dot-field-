package com.dotfield.matching;

import org.springframework.stereotype.Component;

@Component
public class MatchScoreCalculator {

    private static final double SKILL_WEIGHT = 0.60;
    private static final double EXP_WEIGHT = 0.20;
    private static final double EDU_WEIGHT = 0.10;
    private static final double LOC_WEIGHT = 0.10;

    public record ScoreResult(
            int overallScore,
            String matchCategory
    ) {}

    public ScoreResult calculate(Integer skillScore, Integer experienceScore, Integer educationScore, Integer locationScore) {
        double totalAvailableWeight = 0.0;

        if (skillScore != null) totalAvailableWeight += SKILL_WEIGHT;
        if (experienceScore != null) totalAvailableWeight += EXP_WEIGHT;
        if (educationScore != null) totalAvailableWeight += EDU_WEIGHT;
        if (locationScore != null) totalAvailableWeight += LOC_WEIGHT;

        if (totalAvailableWeight == 0.0) {
            return new ScoreResult(0, "WEAK_MATCH");
        }

        double weightedSum = 0.0;

        if (skillScore != null) {
            double normWeight = SKILL_WEIGHT / totalAvailableWeight;
            weightedSum += skillScore * normWeight;
        }

        if (experienceScore != null) {
            double normWeight = EXP_WEIGHT / totalAvailableWeight;
            weightedSum += experienceScore * normWeight;
        }

        if (educationScore != null) {
            double normWeight = EDU_WEIGHT / totalAvailableWeight;
            weightedSum += educationScore * normWeight;
        }

        if (locationScore != null) {
            double normWeight = LOC_WEIGHT / totalAvailableWeight;
            weightedSum += locationScore * normWeight;
        }

        int finalScore = (int) Math.round(weightedSum);
        finalScore = Math.min(100, Math.max(0, finalScore));

        String category = categorize(finalScore);

        return new ScoreResult(finalScore, category);
    }

    public String categorize(int score) {
        if (score >= 80) {
            return "STRONG_MATCH";
        } else if (score >= 60) {
            return "GOOD_MATCH";
        } else if (score >= 40) {
            return "PARTIAL_MATCH";
        } else {
            return "WEAK_MATCH";
        }
    }

}
