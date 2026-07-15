package com.zeta.business.learningresource;

/** 学员资源库中的资料分类。 */
public enum LearningResourceType {
    DEBUG_OUTLINE,
    MANUAL,
    DRAWING,
    VIDEO_COURSE;

    public boolean isPdf() {
        return this != VIDEO_COURSE;
    }
}
