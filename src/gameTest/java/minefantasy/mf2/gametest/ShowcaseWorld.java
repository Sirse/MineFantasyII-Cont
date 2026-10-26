package minefantasy.mf2.gametest;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.biome.WorldChunkManagerHell;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;

/**
 * A flat world for looking at MineFantasy's machines: bedrock, two dirt and grass, no villages. {@link Showcase} builds
 * the machines into it and only into it.
 */
public final class ShowcaseWorld extends WorldType {

    public static final String NAME = "mf2showcase";
    /** The flat preset: bedrock, two dirt, grass, in plains. */
    private static final String LAYERS = "2;7,2x3,2;1;";
    /** The height of the first block above the grass. */
    public static final int GROUND = 4;

    public static ShowcaseWorld type;

    private ShowcaseWorld() {
        super(NAME);
    }

    public static void register() {
        type = new ShowcaseWorld();
    }

    public static boolean is(World world) {
        return world.provider.dimensionId == 0 && world.getWorldInfo().getTerrainType() == type;
    }

    /** The menu translates this as a key and, finding none, shows it as it is in every language. */
    @Override
    public String getTranslateName() {
        return "MF2 Test World";
    }

    @Override
    public WorldChunkManager getChunkManager(World world) {
        return new WorldChunkManagerHell(BiomeGenBase.plains, 0.5F);
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String generatorOptions) {
        return new ChunkProviderFlat(world, world.getSeed(), false, LAYERS);
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return GROUND;
    }

    @Override
    public double getHorizon(World world) {
        return 0.0D;
    }

    @Override
    public boolean hasVoidParticles(boolean flag) {
        return false;
    }

    @Override
    public double voidFadeMagnitude() {
        return 1.0D;
    }
}
