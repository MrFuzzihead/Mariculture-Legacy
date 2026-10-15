package mariculture.core.handlers;

import java.util.HashMap;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import mariculture.api.core.Environment.Salinity;
import mariculture.api.core.IEnvironmentHandler;

public class EnvironmentHandler implements IEnvironmentHandler {

    public static final HashMap<Integer, BiomeData> environments = new HashMap();

    @Override
    public void addEnvironment(BiomeGenBase biome, Salinity salinity, int temperature) {
        environments.put(biome.biomeID, new BiomeData(temperature, salinity));
    }

    @Override
    public Salinity getSalinity(World world, int x, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        BiomeData data = environments.get(biome.biomeID);
        return data == null ? Salinity.FRESH : data.salinity;
    }

    @Override
    public int getBiomeTemperature(World world, int x, int y, int z) {
        int temperature = 10;
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        BiomeData data = environments.get(biome.biomeID);
        if (data != null) {
            temperature = data.temperature;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.NETHER)) {
            temperature = 100;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.DESERT)) {
            temperature = 50;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.FROZEN)) {
            temperature = -30;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.SWAMP)) {
            temperature = 8;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.JUNGLE)) {
            temperature = 25;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.FOREST)) {
            temperature = 10;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.PLAINS)) {
            temperature = 10;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.WASTELAND)) {
            temperature = 45;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.END)) {
            temperature = -10;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.WATER)) {
            temperature = 3;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.MAGICAL)) {
            temperature = 20;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.BEACH)) {
            temperature = 15;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.MOUNTAIN)) {
            temperature = -10;
        } else if (BiomeDictionary.isBiomeOfType(biome, Type.HILLS)) {
            temperature = -2;
        }

        return temperature;
    }

    @Override
    public int getTemperature(World world, int x, int y, int z) {
        return calculateTemperature(
            getBiomeTemperature(world, x, y, z),
            getSunBrightness(world, 1F),
            world.getRainStrength(1),
            y);
    }

    /** Altitude band (inclusive) that keeps the biome's base temperature. */
    private static final int PLATEAU_BOTTOM = 60;
    private static final int PLATEAU_TOP = 68;
    /** Temperature change per block outside the plateau (about 1 degree per 8.9 blocks). */
    private static final double LAPSE_RATE = 0.11255D;
    /**
     * Explicit rain cooling at full rain strength. This is intentionally small because
     * {@link #getSunBrightness(World, float)} already reduces the sun contribution while it rains; the point of
     * this term is that it must actually be applied instead of being truncated to zero by an {@code (int)} cast.
     */
    private static final double RAIN_COOLING = 0.15D;

    /**
     * World-free version of {@link #getTemperature(World, int, int, int)}, used for testing the profile. The
     * calculation stays in {@code double} and is rounded once at the end: the old code stored the result in an
     * {@code int} through every step and truncated the altitude and rain terms toward zero, which collapsed the
     * rain term to 0 and added a step at the altitude boundaries.
     */
    static int calculateTemperature(int biomeTemperature, float sunBrightness, float rainStrength, int y) {
        return (int) Math.round(calculateTemperatureRaw(biomeTemperature, sunBrightness, rainStrength, y));
    }

    static double calculateTemperatureRaw(int biomeTemperature, float sunBrightness, float rainStrength, int y) {
        double temperature = biomeTemperature - 3;
        temperature += sunBrightness * 9 - 2;
        temperature += getAltitudeOffset(y);
        temperature -= rainStrength * RAIN_COOLING;
        return temperature;
    }

    /**
     * Altitude adjustment: 0 inside the surface band, cooling with height above it and with depth below it. This
     * is the intended shape of the two altitude branches; the previous code had the constants swapped
     * ({@code y > 60} / {@code y < 68}), so the surface band never existed and the value stepped at its edge.
     */
    static double getAltitudeOffset(int y) {
        if (y > PLATEAU_TOP) return (PLATEAU_TOP - y) * LAPSE_RATE;
        if (y < PLATEAU_BOTTOM) return -(PLATEAU_BOTTOM - y) * LAPSE_RATE;
        return 0.0D;
    }

    // Copied from world
    public float getSunBrightness(World world, float par1) {
        float f1 = world.getCelestialAngle(par1);
        float f2 = 1.0F - (MathHelper.cos(f1 * (float) Math.PI * 2.0F) * 2.0F + 0.2F);

        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        if (f2 > 1.0F) {
            f2 = 1.0F;
        }

        f2 = 1.0F - f2;
        f2 = (float) (f2 * (1.0D - world.getRainStrength(par1) * 5.0F / 16.0D));
        f2 = (float) (f2 * (1.0D - world.getWeightedThunderStrength(par1) * 5.0F / 16.0D));
        return f2 * 0.8F + 0.2F;
    }

    private static class BiomeData {

        private int temperature;
        private Salinity salinity;

        private BiomeData(int temperature, Salinity salinity) {
            this.temperature = temperature;
            this.salinity = salinity;
        }
    }

    @Override
    public boolean matches(Salinity biomeSalinity, int biomeTemp, Salinity checkSaltBase, int checkSaltVariation,
        int checkTempBase, int checkTempVariation) {
        int minSalt = Math.max(0, checkSaltBase.ordinal() - checkSaltVariation);
        int maxSalt = Math.min(2, checkSaltBase.ordinal() + checkSaltVariation);
        int minTemp = checkTempBase - checkTempVariation;
        int maxTemp = checkTempBase + checkTempVariation;

        if (biomeTemp >= minTemp && biomeTemp <= maxTemp) {
            if (biomeSalinity.ordinal() >= minSalt && biomeSalinity.ordinal() <= maxSalt) {
                return true;
            }
        }

        return false;
    }
}
