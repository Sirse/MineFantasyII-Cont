package minefantasy.mf2.integration.minetweaker;

import java.util.Arrays;
import java.util.Map;

import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.rpg.RPGElements;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.integration.minetweaker.helpers.ScriptNames;
import minetweaker.MineTweakerAPI;
import minetweaker.MineTweakerImplementationAPI;
import minetweaker.api.player.IPlayer;
import minetweaker.api.server.ICommandFunction;

public class MTCommands implements ICommandFunction {

    @Override
    public void execute(String[] arguments, IPlayer player) {
        if (arguments.length < 1) {
            if (player != null) {
                player.sendChat("Specify materials, skills, tools or designs");
            }
            return;
        }

        if (arguments[0].equals("tools") || arguments[0].equals("designs")) {
            MineTweakerAPI.logCommand(
                    arguments[0].equals("tools")
                            ? "MineFantasy built-in tool types: " + ScriptNames.TOOLS
                                    + "; grid recipes also accept hands; mods may add tool types"
                            : ScriptNames.designs());
            if (player != null) {
                player.sendChat("Names generated; see minetweaker.log in your minecraft dir");
            }
            return;
        }

        if (arguments[0].equals("materials")) {
            MineTweakerAPI.logCommand("MineFantasy materials list:");
            for (Map.Entry<String, CustomMaterial> entry : CustomMaterial.materialList.entrySet()) {
                CustomMaterial material = entry.getValue();
                MineTweakerAPI.logCommand(
                        String.format(
                                "    %s (type: %s, color (RGB): %s, tier: %d, hardness: %.2f, durability: %.2f, flexibility: %.2f, sharpness: %.2f, resistance: %.2f, density: %.2f)",
                                material.name,
                                material.type,
                                Arrays.toString(material.colourRGB),
                                material.tier,
                                material.hardness,
                                material.durability,
                                material.flexibility,
                                material.sharpness,
                                material.resistance,
                                material.density));
            }

            if (player != null) {
                player.sendChat(
                        MineTweakerImplementationAPI.platform
                                .getMessage("Materials list generated; see minetweaker.log in your minecraft dir"));
            }
        }
        if (arguments[0].equals("skills")) {
            MineTweakerAPI.logCommand("MineFantasy skills list:");

            for (Skill skill : RPGElements.skillsList) {
                MineTweakerAPI.logCommand(
                        String.format("Skill %s registered as %s", skill.getDisplayName(), skill.skillName));
            }

            if (player != null) {
                player.sendChat(
                        MineTweakerImplementationAPI.platform
                                .getMessage("Skills list generated; see minetweaker.log in your minecraft dir"));
            }
        }
    }
}
