package me.marcdoesntexist.galaxymines.luckyblocks;






public record LuckyBlockFeedback(String sound, String particle, int cooldownSeconds) {
    public static final LuckyBlockFeedback NONE = new LuckyBlockFeedback("", "", 0);

    public LuckyBlockFeedback {
        sound = sound == null ? "" : sound.trim();
        particle = particle == null ? "" : particle.trim();
        cooldownSeconds = Math.max(0, Math.min(3600, cooldownSeconds));
    }

    public boolean hasSound() {
        return !this.sound.isBlank();
    }

    public boolean hasParticle() {
        return !this.particle.isBlank();
    }

    public boolean hasCooldown() {
        return this.cooldownSeconds > 0;
    }
}
