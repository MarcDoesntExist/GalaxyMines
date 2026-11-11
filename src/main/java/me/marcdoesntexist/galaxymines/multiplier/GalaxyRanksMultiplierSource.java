package me.marcdoesntexist.galaxymines.multiplier;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;










public final class GalaxyRanksMultiplierSource implements MultiplierSource {
    public static final String ID = "galaxyranks";
    private static final String API_CLASS = "me.marcdoesntexist.galaxyranks.api.GalaxyRanksAPI";

    private final Method multiplier;
    private final Logger logger;
    private boolean failureReported;

    private GalaxyRanksMultiplierSource(final Method multiplier, final Logger logger) {
        this.multiplier = multiplier;
        this.logger = logger != null ? logger : Logger.getLogger("GalaxyMines");
    }




    public static GalaxyRanksMultiplierSource resolve(final Logger logger) {
        return resolve(logger, API_CLASS);
    }





    static GalaxyRanksMultiplierSource resolve(final Logger logger, final String apiClass) {
        try {
            final Class<?> api = Class.forName(apiClass);
            final Method method = api.getMethod("multiplier", Player.class);
            return new GalaxyRanksMultiplierSource(method, logger);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError
                 | RuntimeException ex) {
            return null;
        }
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public double dropMultiplier(final Player player) {
        return this.read(player);
    }

    @Override
    public double sellMultiplier(final Player player) {
        return this.read(player);
    }

    private double read(final Player player) {
        if (player == null || !player.isOnline()) {
            return 1.0D;
        }
        try {
            final Object value = this.multiplier.invoke(null, player);
            if (value instanceof Number number) {
                final double multiplier = number.doubleValue();
                return Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
            }
            return 1.0D;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (!this.failureReported) {
                this.failureReported = true;
                this.logger.log(Level.WARNING, "The GalaxyRanks multiplier could not be read; "
                        + "rank multipliers are treated as 1.0 until the next reload.", ex);
            }
            return 1.0D;
        }
    }
}
