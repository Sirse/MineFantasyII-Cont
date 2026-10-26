package minefantasy.mf2.gametest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizons.horizonqa.structure.HybridStructureLoader;
import com.gtnewhorizons.horizonqa.structure.HybridStructureTemplate;
import com.gtnewhorizons.horizonqa.structure.StructurePlacer;

/**
 * Builds every structure template in a row east of the spawn of a {@link ShowcaseWorld}, each behind a sign naming it.
 * The templates are the ones the game tests stand on, so what the showcase shows is what the tests check.
 */
public final class Showcase {

    private static final Logger LOG = LogManager.getLogger("MF2 showcase");

    static final String[] TEMPLATES = { Templates.BIG_FURNACE, Templates.BIG_FURNACE_WORKING, Templates.CRUCIBLE_STONE,
            Templates.CRUCIBLE_COATED, Templates.CRUCIBLE_NO_WALLS, Templates.CRUCIBLE_ENDER, Templates.BLAST_FURNACE,
            Templates.COGWORK_FRAME, Templates.COGWORK_STATION };

    /** Air between two structures. */
    private static final int GAP = 3;
    /** How high above the ground a rebuild clears. */
    private static final int CLEAR_HEIGHT = 12;
    /** The distance from the spawn to the row, and from the signs to the structures. */
    private static final int OFFSET = 4, SIGN_DISTANCE = 2;
    private static final int SIGN_WIDTH = 15;
    /** The menagerie lies south of both the structures and the storage, past the longer of the two. */
    private static final int MENAGERIE_WEST = 20, MENAGERIE_SOUTH = Math.max(16, Storage.DEPTH) + GAP;

    private Showcase() {}

    /**
     * Builds the showcase into a new showcase world once. The world keeps that it was built, so nothing taken from it
     * brings a rebuild over what the player made there; only the command rebuilds.
     */
    public static void buildIfNew(WorldServer world) {
        if (!ShowcaseWorld.is(world)) return;
        Built built = (Built) world.perWorldStorage.loadData(Built.class, Built.NAME);
        if (built != null) return;
        Result result = build(world);
        // A showcase built only in part is not marked built, so the next start tries again
        if (!result.complete) {
            LOG.warn("The showcase was built only in part: {}", result.summary);
            return;
        }
        built = new Built(Built.NAME);
        world.perWorldStorage.setData(Built.NAME, built);
        built.markDirty();
    }

    /** What a build put up, for the chat, and whether it put up all it meant to. */
    public static final class Result {

        final String summary;
        final boolean complete;

        Result(String summary, boolean complete) {
            this.summary = summary;
            this.complete = complete;
        }
    }

    /** Saved with the world once the showcase is first built. */
    public static final class Built extends WorldSavedData {

        static final String NAME = "mf2showcase";

        public Built(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {}

        @Override
        public void writeToNBT(NBTTagCompound nbt) {}
    }

    /** Builds every district anew, with a still noon and no rain. */
    public static Result build(WorldServer world) {
        rebuilt.clear();
        ChunkCoordinates spawn = world.getSpawnPoint();
        int structures = buildStructures(world);
        int blocks = BlockCatalog.build(world, spawn.posX + OFFSET, spawn.posZ - OFFSET);
        int items = Storage.build(world, spawn.posX - OFFSET, spawn.posZ - OFFSET);
        Menagerie.Count creatures = Menagerie.build(world, spawn.posX - MENAGERIE_WEST, spawn.posZ + MENAGERIE_SOUTH);
        stillNoon(world);
        sweepTicks = SWEEP_TICKS;
        sweep(world);
        boolean complete = structures == TEMPLATES.length && creatures.shown == creatures.meant;
        String summary = "Built " + structures
                + " of "
                + TEMPLATES.length
                + " structures, "
                + blocks
                + " blocks, "
                + items
                + " items and "
                + creatures.shown
                + " of "
                + creatures.meant
                + " creatures"
                + (complete ? "." : "; see the log for what is missing.");
        return new Result(summary, complete);
    }

    /** The areas the last build cleared, west, north, east and south edges each. */
    private static final List<int[]> rebuilt = new ArrayList<>();
    /** Ticks left to sweep up what a rebuild shook loose: blocks that cannot stay fall as items a moment later. */
    private static final int SWEEP_TICKS = 40;
    private static int sweepTicks;

    /** Takes away the items lying in the areas just rebuilt, and nowhere else, while the sweep lasts. */
    static void sweep(net.minecraft.world.World world) {
        if (sweepTicks <= 0) return;
        sweepTicks--;
        for (Object entity : world.loadedEntityList) {
            if (!(entity instanceof EntityItem)) continue;
            EntityItem item = (EntityItem) entity;
            for (int[] area : rebuilt) {
                if (item.posX >= area[0] && item.posX < area[2] + 1
                        && item.posZ >= area[1]
                        && item.posZ < area[3] + 1) {
                    item.setDead();
                    break;
                }
            }
        }
    }

    /** Loads the chunks of an area, with what was saved in them, before it is rebuilt. */
    static void load(WorldServer world, int x1, int z1, int x2, int z2) {
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) {
            for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
                world.theChunkProviderServer.loadChunk(cx, cz);
            }
        }
    }

    /** Clears the row south-east of the spawn and builds every template into it. */
    private static int buildStructures(WorldServer world) {
        HybridStructureTemplate[] templates = new HybridStructureTemplate[TEMPLATES.length];
        int length = 0, depth = 0;
        for (int i = 0; i < TEMPLATES.length; i++) {
            templates[i] = load(TEMPLATES[i]);
            if (templates[i] == null) continue;
            length += templates[i].getSizeX() + GAP;
            depth = Math.max(depth, templates[i].getSizeZ());
        }
        ChunkCoordinates start = start(world);
        clear(world, start.posX - GAP, start.posZ - SIGN_DISTANCE - 1, start.posX + length, start.posZ + depth + 1);

        int x = start.posX, built = 0;
        for (int i = 0; i < templates.length; i++) {
            if (templates[i] == null) continue;
            StructurePlacer.place(templates[i], world, x, start.posY, start.posZ);
            sign(world, x, start.posY, start.posZ - SIGN_DISTANCE, TEMPLATES[i]);
            x += templates[i].getSizeX() + GAP;
            built++;
        }
        return built;
    }

    private static ChunkCoordinates start(WorldServer world) {
        ChunkCoordinates spawn = world.getSpawnPoint();
        return new ChunkCoordinates(spawn.posX + OFFSET, ShowcaseWorld.GROUND, spawn.posZ + OFFSET);
    }

    private static HybridStructureTemplate load(String path) {
        try {
            return HybridStructureLoader.load("minefantasy2:" + path);
        } catch (IOException | RuntimeException e) {
            LOG.error("Cannot load template {}", path, e);
            return null;
        }
    }

    static void clear(WorldServer world, int x1, int z1, int x2, int z2) {
        load(world, x1, z1, x2, z2);
        rebuilt.add(new int[] { x1, z1, x2, z2 });
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                world.setBlock(x, ShowcaseWorld.GROUND - 1, z, Blocks.grass, 0, 2);
                for (int y = ShowcaseWorld.GROUND; y < ShowcaseWorld.GROUND + CLEAR_HEIGHT; y++) {
                    if (!world.isAirBlock(x, y, z)) {
                        // Without its tile the block has no contents to spill
                        world.removeTileEntity(x, y, z);
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                    }
                }
            }
        }
    }

    /**
     * A sign facing north, toward the player walking up from the spawn, with the text split at {@code /} and wrapped to
     * the sign's width.
     */
    static void sign(WorldServer world, int x, int y, int z, String text) {
        sign(world, x, y, z, 8, text);
    }

    /** A sign turned by {@code rotation}, in sixteenths clockwise from facing south. */
    static void sign(WorldServer world, int x, int y, int z, int rotation, String text) {
        world.setBlock(x, y, z, Blocks.standing_sign, rotation, 2);
        if (world.getTileEntity(x, y, z) instanceof TileEntitySign) {
            TileEntitySign sign = (TileEntitySign) world.getTileEntity(x, y, z);
            int line = 0;
            for (String part : text.split("/")) {
                for (int from = 0; from < part.length() && line < sign.signText.length; from += SIGN_WIDTH) {
                    sign.signText[line++] = part.substring(from, Math.min(part.length(), from + SIGN_WIDTH));
                }
            }
            sign.markDirty();
            world.markBlockForUpdate(x, y, z);
        }
    }

    private static void stillNoon(WorldServer world) {
        GameRules rules = world.getGameRules();
        rules.setOrCreateGameRule("doDaylightCycle", "false");
        rules.setOrCreateGameRule("doMobSpawning", "false");
        world.setWorldTime(6000L);
        WorldInfo info = world.getWorldInfo();
        info.setRaining(false);
        info.setThundering(false);
        info.setRainTime(Integer.MAX_VALUE);
        info.setThunderTime(Integer.MAX_VALUE);
    }

    /** {@code /mf2showcase}: rebuilds the showcase, in a showcase world only. */
    public static final class Command extends CommandBase {

        @Override
        public String getCommandName() {
            return ShowcaseWorld.NAME;
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/" + ShowcaseWorld.NAME;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 0) throw new WrongUsageException(getCommandUsage(sender));
            if (!(sender.getEntityWorld() instanceof WorldServer) || !ShowcaseWorld.is(sender.getEntityWorld())) {
                sender.addChatMessage(new ChatComponentText("The showcase is built in the MF2 Test World only."));
                return;
            }
            sender.addChatMessage(new ChatComponentText(build((WorldServer) sender.getEntityWorld()).summary));
        }
    }
}
