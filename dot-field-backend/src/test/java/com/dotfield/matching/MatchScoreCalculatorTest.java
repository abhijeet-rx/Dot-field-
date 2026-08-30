package com.dotfield.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchScoreCalculatorTest {

    private MatchScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MatchScoreCalculator();
    }

    @Test
    void calculate_allDimensionsAvailable_usesBaseWeights() {
        // Skill=80 (0.60), Exp=100 (0.20), Edu=100 (0.10), Loc=100 (0.10)
        // 48 + 20 + 10 + 10 = 88
        MatchScoreCalculator.ScoreResult result = calculator.calculate(80, 100, 100, 100);

        assertEquals(88, result.overallScore());
        assertEquals("STRONG_MATCH", result.matchCategory());
    }

    @Test
    void calculate_unknownDimensions_redistributesWeightsProportionally() {
        // Skill=80 (0.60), Exp=50 (0.20), Edu=null, Loc=null
        // Available weights = 0.80. Skill norm = 0.75, Exp norm = 0.25
        // (80 * 0.75) + (50 * 0.25) = 60 + 12.5 = 72.5 -> 73
        MatchScoreCalculator.ScoreResult result = calculator.calculate(80, 50, null, null);

        assertEquals(73, result.overallScore());
        assertEquals("GOOD_MATCH", result.matchCategory());
    }

    @Test
    void calculate_allDimensionsUnknown_returns0AndWeakMatch() {
        MatchScoreCalculator.ScoreResult result = calculator.calculate(null, null, null, null);

        assertEquals(0, result.overallScore());
        assertEquals("WEAK_MATCH", result.matchCategory());
    }

    @Test
    void categorize_testsBoundaryConditions() {
        assertEquals("WEAK_MATCH", calculator.categorize(0));
        assertEquals("WEAK_MATCH", calculator.categorize(39));
        assertEquals("PARTIAL_MATCH", calculator.categorize(40));
        assertEquals("PARTIAL_MATCH", calculator.categorize(59));
        assertEquals("GOOD_MATCH", calculator.categorize(60));
        assertEquals("GOOD_MATCH", calculator.categorize(79));
        assertEquals("STRONG_MATCH", calculator.categorize(80));
        assertEquals("STRONG_MATCH", calculator.categorize(100));
    }
}
