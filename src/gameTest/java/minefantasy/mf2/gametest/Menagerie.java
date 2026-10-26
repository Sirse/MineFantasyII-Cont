package minefantasy.mf2.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import minefantasy.mf2.entity.EntityCogwork;
import minefantasy.mf2.entity.mob.DragonBreed;
import minefantasy.mf2.entity.mob.EntityDragon;
import minefantasy.mf2.entity.mob.EntityHound;
import minefantasy.mf2.entity.mob.EntityMinotaur;
import minefantasy.mf2.entity.mob.MinotaurBreed;

/**
 * South of the spawn: every MineFantasy creature over a sign naming it, the dragons in a grid of age by breed and the
 * minotaurs by kind and rank. They are held where they stand, facing the spawn, never hunting or despawning: a place to
 * look at the models. Beside them an arena of young dragons left to their own ways, only kept from flying off, for how
 * they move and fight.
 */
public final class Menagerie {

    private static final Logger LOG = LogManager.getLogger("MF2 showcase");
    /** Marks a held showcase creature, and where it is held. */
    private static final String ANCHOR = "MF2ShowcaseAnchor";
    /** Marks an arena dragon, and the middle of its arena. */
    private static final String ARENA = "MF2ShowcaseArena";
    /** Room for the largest dragon between two cells of the grid, and between the other rows. */
    private static final int DRAGON_STEP = 14, STEP = 5;
    /** The arena's half width, and how far a dragon may fly from its middle before it is brought back. */
    private static final int ARENA_HALF = 16, ARENA_REACH = 40;
    /** A sign facing north, toward the player walking up from the spawn. */
    private static final int FACING_NORTH = 8;
    private static final int DRAGON_AGES = 5, MINOTAUR_SPECIES = 3, MINOTAUR_RANKS = 4;
    /** How far a held creature may stray before it is pulled back. */
    private static final double DRIFT = 0.5D;

    private Menagerie() {}

    /** What a rebuild put up: the creatures shown, and those it meant to show. */
    static final class Count {

        int shown, meant;
    }

    /**
     * Clears the district south of {@code (x, z)}, its creatures with it, and fills it anew. The district is loaded
     * first, so creatures saved in a chunk that was not loaded are cleared too rather than coming back beside the new.
     */
    static Count build(WorldServer world, int x, int z) {
        List<List<Exhibit>> rows = new ArrayList<>();
        rows.addAll(dragons(world));
        int dragonRows = rows.size();
        for (int species = 0; species < MINOTAUR_SPECIES; species++) {
            rows.add(minotaurs(world, species));
        }
        List<Exhibit> others = new ArrayList<>();
        others.add(new Exhibit(new EntityHound(world), "Hound"));
        others.add(new Exhibit(new EntityCogwork(world), "Cogwork/suit"));
        rows.add(others);

        int width = 0;
        for (List<Exhibit> row : rows) width = Math.max(width, row.size() * DRAGON_STEP);
        int depth = dragonRows * DRAGON_STEP + (rows.size() - dragonRows) * STEP * 2;
        int arenaX = x + width + ARENA_HALF + 4, arenaZ = z + ARENA_HALF;
        int x2 = arenaX + ARENA_HALF, z2 = z + Math.max(depth, ARENA_HALF * 2);

        Showcase.load(world, x - 1, z - 1, x2, z2);
        for (Object entity : new ArrayList<>(world.loadedEntityList)) {
            Entity e = (Entity) entity;
            if ((e.getEntityData().hasKey(ANCHOR) || e.getEntityData().hasKey(ARENA)) && e.posX >= x - 1
                    && e.posX <= x2 + 1
                    && e.posZ >= z - 1
                    && e.posZ <= z2 + 1) {
                e.setDead();
            }
        }
        Showcase.clear(world, x - 1, z - 1, x2, z2);

        Count count = new Count();
        int y = ShowcaseWorld.GROUND, rowZ = z + 1;
        for (int r = 0; r < rows.size(); r++) {
            int step = r < dragonRows ? DRAGON_STEP : STEP;
            for (int i = 0; i < rows.get(r).size(); i++) {
                Exhibit exhibit = rows.get(r).get(i);
                int at = x + step / 2 + i * step;
                Showcase.sign(world, at, y, rowZ, FACING_NORTH, exhibit.name);
                count.meant++;
                if (hold(world, exhibit.creature, at + 0.5D, y, rowZ + step / 2 + 0.5D)) count.shown++;
            }
            rowZ += r < dragonRows ? DRAGON_STEP : STEP * 2;
        }
        Showcase.sign(world, arenaX, y, z, FACING_NORTH, "Arena/young dragons/as they are");
        for (int breed = 0; breed < 4; breed++) {
            EntityDragon dragon = new EntityDragon(world);
            dragon.setDragon(0);
            dragon.setBreed(breed);
            count.meant++;
            if (free(world, dragon, arenaX, arenaZ, breed)) count.shown++;
        }
        return count;
    }

    /** The dragons, a row for each age and a column for each breed of it. */
    private static List<List<Exhibit>> dragons(World world) {
        List<List<Exhibit>> rows = new ArrayList<>();
        for (int age = 0; age < DRAGON_AGES; age++) {
            List<Exhibit> row = new ArrayList<>();
            DragonBreed last = null;
            for (int breed = 0;; breed++) {
                DragonBreed type;
                try {
                    type = DragonBreed.getBreed(age, breed);
                } catch (RuntimeException e) {
                    break; // no dragon of this age
                }
                // Past the last breed the lookup repeats it
                if (type == last) break;
                last = type;
                EntityDragon dragon = new EntityDragon(world);
                dragon.setDragon(age);
                dragon.setBreed(breed);
                row.add(new Exhibit(dragon, type.name + "/" + type.breedName));
            }
            if (!row.isEmpty()) rows.add(row);
        }
        return rows;
    }

    private static List<Exhibit> minotaurs(World world, int species) {
        List<Exhibit> row = new ArrayList<>();
        MinotaurBreed last = null;
        for (int rank = 0; rank < MINOTAUR_RANKS; rank++) {
            MinotaurBreed type = MinotaurBreed.getBreed(species, rank);
            if (type == last) break;
            last = type;
            EntityMinotaur minotaur = new EntityMinotaur(world);
            minotaur.setMob(species, rank);
            row.add(new Exhibit(minotaur, type.name + "/" + type.tex));
        }
        return row;
    }

    /** Puts up a creature held where it stands, facing north toward the spawn. */
    private static boolean hold(WorldServer world, EntityLivingBase creature, double x, double y, double z) {
        creature.setLocationAndAngles(x, y, z, 180.0F, 0.0F);
        creature.rotationYawHead = creature.renderYawOffset = 180.0F;
        NBTTagCompound anchor = new NBTTagCompound();
        anchor.setDouble("X", x);
        anchor.setDouble("Y", y);
        anchor.setDouble("Z", z);
        creature.getEntityData().setTag(ANCHOR, anchor);
        // Calmed before it is in the world, so no goal of its own has begun
        calm(creature, x, y, z);
        return spawn(world, creature);
    }

    /** Lets an arena dragon go, beside the middle of its arena. */
    private static boolean free(WorldServer world, EntityDragon dragon, int x, int z, int place) {
        dragon.setLocationAndAngles(
                x + (place % 2) * 8 - 4 + 0.5D,
                ShowcaseWorld.GROUND,
                z + (place / 2) * 8 - 4 + 0.5D,
                0,
                0);
        NBTTagCompound arena = new NBTTagCompound();
        arena.setDouble("X", x + 0.5D);
        arena.setDouble("Z", z + 0.5D);
        dragon.getEntityData().setTag(ARENA, arena);
        return spawn(world, dragon);
    }

    private static boolean spawn(WorldServer world, EntityLivingBase creature) {
        if (creature instanceof EntityLiving) {
            ((EntityLiving) creature).func_110163_bv(); // never despawns
        }
        if (world.spawnEntityInWorld(creature)) return true;
        LOG.warn("The showcase could not put up {}", creature.getCommandSenderName());
        return false;
    }

    /**
     * Holds every showcase creature where it stands and brings the arena's dragons back when they fly off. Public, as
     * the event bus calls it from a generated class.
     */
    public static final class Keeper {

        private static boolean warned;

        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.world.isRemote || !ShowcaseWorld.is(event.world)) return;
            if (event.phase == TickEvent.Phase.START) {
                // A dragon on Peaceful sends itself away within the tick: the showcase stays at Easy at least
                if (event.world.difficultySetting == EnumDifficulty.PEACEFUL) {
                    event.world.difficultySetting = EnumDifficulty.EASY;
                    if (!warned) {
                        warned = true;
                        LOG.info("The showcase world is kept at Easy: on Peaceful its dragons would leave");
                    }
                }
                return;
            }
            Showcase.sweep(event.world);
            for (Object entity : event.world.loadedEntityList) {
                if (!(entity instanceof EntityLivingBase)) continue;
                EntityLivingBase creature = (EntityLivingBase) entity;
                if (creature.getEntityData().hasKey(ANCHOR)) {
                    keep(creature, creature.getEntityData().getCompoundTag(ANCHOR));
                } else if (creature.getEntityData().hasKey(ARENA)) {
                    NBTTagCompound arena = creature.getEntityData().getCompoundTag(ARENA);
                    double x = arena.getDouble("X"), z = arena.getDouble("Z");
                    double dx = creature.posX - x, dz = creature.posZ - z;
                    // Brought back too before it climbs to where a bored dragon leaves for good
                    if (dx * dx + dz * dz > ARENA_REACH * ARENA_REACH
                            || creature.posY > ShowcaseWorld.GROUND + ARENA_REACH) {
                        creature.setPositionAndUpdate(x, ShowcaseWorld.GROUND + 8, z);
                    }
                }
            }
        }

        private static void keep(EntityLivingBase creature, NBTTagCompound anchor) {
            double x = anchor.getDouble("X"), y = anchor.getDouble("Y"), z = anchor.getDouble("Z");
            calm(creature, x, y, z);
            // Pulled back only when it has drifted, so the client is not sent a jump every tick
            if (creature.getDistanceSq(x, y, z) > DRIFT * DRIFT) {
                creature.setPositionAndUpdate(x, y, z);
            }
            creature.motionX = creature.motionZ = 0.0D;
            if (creature.motionY > 0.0D) creature.motionY = 0.0D;
            creature.fallDistance = 0.0F;
        }
    }

    /**
     * Takes away what moves a creature, so it stands and breathes instead of being dragged back every tick: its goals,
     * its target, and a dragon's course, flight and fire.
     */
    private static void calm(EntityLivingBase creature, double x, double y, double z) {
        creature.setRevengeTarget(null);
        if (creature instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) creature;
            living.setAttackTarget(null);
            // A loaded creature gets its goals again from its constructor; removed this way a running one is stopped
            dropGoals(living.tasks);
            dropGoals(living.targetTasks);
            living.getNavigator().clearPathEntity();
        }
        if (creature instanceof EntityCreature) ((EntityCreature) creature).setTarget(null);
        if (creature instanceof EntityDragon) {
            EntityDragon dragon = (EntityDragon) creature;
            dragon.disengage(100);
            dragon.setTerrestrial(true);
            dragon.waypointX = x;
            dragon.waypointY = y;
            dragon.waypointZ = z;
            dragon.courseChangeCooldown = 100;
            dragon.fireBreathCooldown = 200;
        }
    }

    private static void dropGoals(EntityAITasks tasks) {
        if (tasks.taskEntries.isEmpty()) return;
        for (Object entry : new ArrayList<>(tasks.taskEntries)) {
            tasks.removeTask(((EntityAITasks.EntityAITaskEntry) entry).action);
        }
    }

    private static final class Exhibit {

        final EntityLivingBase creature;
        final String name;

        Exhibit(EntityLivingBase creature, String name) {
            this.creature = creature;
            this.name = name;
        }
    }
}
