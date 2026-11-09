package com.example.Tech.entity.recommendation;

/**
 * Answer to "Tầm giá" in the shopping-preference questionnaire. The AI service maps it to the price segment of a
 * product inside its own category (lowest, middle or highest third of the prices of that category).
 */
public enum PriceLevel {
    LOW,
    MID,
    HIGH
}
