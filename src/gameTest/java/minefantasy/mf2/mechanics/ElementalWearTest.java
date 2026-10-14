package minefantasy.mf2.mechanics;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.Random;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.TestPos;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.entity.EntityDragonBreath;
import minefantasy.mf2.entity.mob.DragonBreath;
import minefantasy.mf2.gametest.Overrides;

/**
 * Fire and acid wear only what has durability, through its own damage handling: an unbreakable item or one without
 * durability stays whole, and a stack is not lost to it.
 */
@GameTestHolder("minefantasy2")
public class ElementalWearTest {

    private ElementalWearTest() {}

    private static EntityZombie wearer(GameTestHelper helper) {
        World world = helper.getWorld();
        TestPos at = helper.absolute(2, 2, 2);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(at.x() + 0.5D, at.y(), at.z() + 0.5D);
        world.spawnEntityInWorld(zombie);
        return zombie;
    }

    private static ItemStack unbreakable(ItemStack stack) {
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean("Unbreakable", true);
        return stack;
    }

    private static void burn(EntityZombie zombie) {
        zombie.hurtResistantTime = 0;
        zombie.attackEntityFrom(DamageSource.inFire, 4F);
    }

    /** A breath of acid that always eats into the equipment. */
    private static void acid(GameTestHelper helper, EntityZombie zombie) {
        World world = helper.getWorld();
        EntityDragonBreath shot = new EntityDragonBreath(world, new EntityPig(world), 0, 0, 1, 0F);
        shot.rand = new Random() {

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        zombie.hurtResistantTime = 0;
        DragonBreath.poison.onHitEntity(zombie, shot);
    }

    private static Overrides wear(boolean fire, boolean acid, String... immune) {
        java.util.Set<String> before = ElementalWear.immune;
        return new Overrides().set(() -> ElementalWear.fire, v -> ElementalWear.fire = v, fire)
                .set(() -> ElementalWear.acid, v -> ElementalWear.acid = v, acid)
                .run(() -> ElementalWear.setImmune(immune), () -> ElementalWear.immune = before);
    }

    @GameTest
    public static void fireWearsArmourButNotWhatCannotWear(GameTestHelper helper) {
        EntityZombie zombie = wearer(helper);
        try (Overrides overrides = wear(true, true)) {
            zombie.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
            zombie.setCurrentItemOrArmor(3, unbreakable(new ItemStack(Items.iron_chestplate)));
            zombie.setCurrentItemOrArmor(2, new ItemStack(Blocks.stone, 5));
            burn(zombie);
            assertTrue("fire did not wear the helmet", zombie.getEquipmentInSlot(4).getItemDamage() > 0);
            assertNotNull("fire destroyed unbreakable armour", zombie.getEquipmentInSlot(3));
            assertEquals("fire wore unbreakable armour", 0, zombie.getEquipmentInSlot(3).getItemDamage());
            assertNotNull("fire destroyed an item without durability", zombie.getEquipmentInSlot(2));
            assertEquals("fire took from a stack", 5, zombie.getEquipmentInSlot(2).stackSize);
        } finally {
            zombie.setDead();
        }
        helper.succeed();
    }

    @GameTest
    public static void acidWearsWhatHasDurabilityAndLeavesTheRest(GameTestHelper helper) {
        EntityZombie zombie = wearer(helper);
        try (Overrides overrides = wear(true, true)) {
            zombie.setCurrentItemOrArmor(0, new ItemStack(Blocks.stone, 16));
            zombie.setCurrentItemOrArmor(4, unbreakable(new ItemStack(Items.iron_helmet)));
            zombie.setCurrentItemOrArmor(3, new ItemStack(Items.iron_chestplate));
            acid(helper, zombie);
            assertNotNull("acid destroyed a held stack", zombie.getEquipmentInSlot(0));
            assertEquals("acid took from a held stack", 16, zombie.getEquipmentInSlot(0).stackSize);
            assertNotNull("acid destroyed unbreakable armour", zombie.getEquipmentInSlot(4));
            assertTrue("acid did not wear the chestplate", zombie.getEquipmentInSlot(3).getItemDamage() > 0);
        } finally {
            zombie.setDead();
        }
        helper.succeed();
    }

    @GameTest
    public static void acidWearsTheHeldItemAndBreaksOnlyWhatIsWornOut(GameTestHelper helper) {
        EntityZombie zombie = wearer(helper);
        try (Overrides overrides = wear(true, true)) {
            zombie.setCurrentItemOrArmor(0, new ItemStack(Items.iron_sword));
            ItemStack worn = new ItemStack(Items.leather_boots);
            worn.setItemDamage(worn.getMaxDamage() - 1);
            zombie.setCurrentItemOrArmor(1, worn);
            acid(helper, zombie);
            assertTrue("acid did not wear the sword", zombie.getEquipmentInSlot(0).getItemDamage() > 0);
            assertNull("worn-out boots did not break", zombie.getEquipmentInSlot(1));
        } finally {
            zombie.setDead();
        }
        helper.succeed();
    }

    @GameTest
    public static void configTurnsWearOffOrSparesItems(GameTestHelper helper) {
        EntityZombie zombie = wearer(helper);
        try {
            try (Overrides overrides = wear(false, false)) {
                zombie.setCurrentItemOrArmor(4, new ItemStack(Items.iron_helmet));
                burn(zombie);
                acid(helper, zombie);
                assertEquals("wear turned off still wore", 0, zombie.getEquipmentInSlot(4).getItemDamage());
            }
            try (Overrides overrides = wear(true, true, "minecraft:iron_helmet")) {
                burn(zombie);
                acid(helper, zombie);
                assertEquals("an immune item was worn", 0, zombie.getEquipmentInSlot(4).getItemDamage());
            }
        } finally {
            zombie.setDead();
        }
        helper.succeed();
    }
}
