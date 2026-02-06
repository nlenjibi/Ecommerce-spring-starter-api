package com.smart_ecomernce_api.smart_ecomernce_api.modules.review;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration; /**
 * Review-specific properties configuration
 */
@Configuration
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {

    private Moderation moderation = new Moderation();
    private Voting voting = new Voting();
    private Limits limits = new Limits();

    public static class Moderation {
        private boolean autoApproveVerified = true;
        private boolean requireApproval = false;

        // Getters and setters
        public boolean isAutoApproveVerified() { return autoApproveVerified; }
        public void setAutoApproveVerified(boolean autoApproveVerified) {
            this.autoApproveVerified = autoApproveVerified;
        }
        public boolean isRequireApproval() { return requireApproval; }
        public void setRequireApproval(boolean requireApproval) {
            this.requireApproval = requireApproval;
        }
    }

    public static class Voting {
        private boolean allowMultiple = false;
        private int cooldownMinutes = 5;

        // Getters and setters
        public boolean isAllowMultiple() { return allowMultiple; }
        public void setAllowMultiple(boolean allowMultiple) {
            this.allowMultiple = allowMultiple;
        }
        public int getCooldownMinutes() { return cooldownMinutes; }
        public void setCooldownMinutes(int cooldownMinutes) {
            this.cooldownMinutes = cooldownMinutes;
        }
    }

    public static class Limits {
        private int minCommentLength = 10;
        private int maxCommentLength = 2000;
        private int maxImages = 5;
        private int maxPros = 10;
        private int maxCons = 10;

        // Getters and setters
        public int getMinCommentLength() { return minCommentLength; }
        public void setMinCommentLength(int minCommentLength) {
            this.minCommentLength = minCommentLength;
        }
        public int getMaxCommentLength() { return maxCommentLength; }
        public void setMaxCommentLength(int maxCommentLength) {
            this.maxCommentLength = maxCommentLength;
        }
        public int getMaxImages() { return maxImages; }
        public void setMaxImages(int maxImages) {
            this.maxImages = maxImages;
        }
        public int getMaxPros() { return maxPros; }
        public void setMaxPros(int maxPros) {
            this.maxPros = maxPros;
        }
        public int getMaxCons() { return maxCons; }
        public void setMaxCons(int maxCons) {
            this.maxCons = maxCons;
        }
    }

    // Getters and setters
    public Moderation getModeration() { return moderation; }
    public void setModeration(Moderation moderation) {
        this.moderation = moderation;
    }
    public Voting getVoting() { return voting; }
    public void setVoting(Voting voting) {
        this.voting = voting;
    }
    public Limits getLimits() { return limits; }
    public void setLimits(Limits limits) {
        this.limits = limits;
    }
}
