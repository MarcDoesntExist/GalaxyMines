package me.marcdoesntexist.galaxymines.multiplier;





public record PlayerBooster(BoosterType type, double multiplier, long expiresAt) {

    public boolean active(final long now) {
        return this.multiplier > 1.0D && this.expiresAt > now;
    }

    public int remainingSeconds(final long now) {
        if (this.expiresAt <= now) {
            return 0;
        }
        final long millis = this.expiresAt - now;
        final long seconds = millis / 1000L;
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }
}
