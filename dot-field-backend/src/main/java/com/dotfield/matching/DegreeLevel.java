package com.dotfield.matching;

public enum DegreeLevel {
    HIGH_SCHOOL(1),
    ASSOCIATE(2),
    BACHELOR(3),
    MASTER(4),
    DOCTORATE(5),
    UNKNOWN(0);

    private final int level;

    DegreeLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
