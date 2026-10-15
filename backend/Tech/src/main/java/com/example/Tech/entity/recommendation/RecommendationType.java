package com.example.Tech.entity.recommendation;

/** Kind of list asked for; the path is the AI service endpoint that computes it (report table 3.23). */
public enum RecommendationType {
    PERSONAL("/recommend"),
    SIMILAR("/similar"),
    UPSELL("/upsell"),
    CROSS_SELL("/cross-sell");

    private final String aiPath;

    RecommendationType(String aiPath) {
        this.aiPath = aiPath;
    }

    public String aiPath() {
        return aiPath;
    }

    public boolean needsProduct() {
        return this != PERSONAL;
    }
}
