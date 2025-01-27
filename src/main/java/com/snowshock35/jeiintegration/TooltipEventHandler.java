/*
 * MIT License
 *
 * Copyright (c) 2020 SnowShock35
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.snowshock35.jeiintegration;

import static net.minecraftforge.common.ForgeHooks.getBurnTime;

import com.mojang.blaze3d.platform.InputConstants;
import com.snowshock35.jeiintegration.config.Config;
import com.snowshock35.jeiintegration.config.Config.Mode;
import java.text.DecimalFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

public class TooltipEventHandler {

    private static final DecimalFormat DECIMALFORMAT = new DecimalFormat("#.##");

    static {
        DECIMALFORMAT.setGroupingUsed(true);
        DECIMALFORMAT.setGroupingSize(3);
    }

    private static boolean isDebugMode() {
        return Minecraft.getInstance().options.advancedItemTooltips;
    }

    private static boolean isShiftKeyDown() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void registerTooltip(ItemTooltipEvent e, Component tooltip, Mode mode) {
        boolean isEnabled = switch (mode) {
            case DISABLED -> false;
            case ENABLED -> true;
            case ON_SHIFT -> isShiftKeyDown();
            case ON_DEBUG -> isDebugMode();
            case ON_SHIFT_AND_DEBUG -> isShiftKeyDown() && isDebugMode();
        };
        if (isEnabled) {
            e.getToolTip().add(tooltip);
        }
    }

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent e) {
        ItemStack itemStack = e.getItemStack();
        Item item = itemStack.getItem();
        Player player = e.getEntity();

        // If item stack empty do nothing
        if (itemStack.isEmpty()) {
            return;
        }

        // Tooltip - Burn Time
        int burnTime = 0;
        try {
            burnTime = getBurnTime(itemStack, RecipeType.SMELTING);
        } catch (Exception ex) {
            JEIIntegration.LOGGER.warn("):\n\nSomething went wrong!");
        }

        if (burnTime > 0) {
            Component burnTooltip = Component.translatable("tooltip.jeiintegration.burnTime")
                    .append(" " + DECIMALFORMAT.format(burnTime) + " ")
                    .append(Component.translatable("tooltip.jeiintegration.burnTime.suffix"))
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, burnTooltip, Config.CLIENT.burnTimeTooltipMode.get());
        }

        // Tooltip - Durability
        int maxDamage = itemStack.getMaxDamage();
        int currentDamage = maxDamage - itemStack.getDamageValue();
        if (maxDamage > 0) {
            Component durabilityTooltip = Component.translatable("tooltip.jeiintegration.durability")
                    .append(" " + currentDamage + "/" + maxDamage)
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, durabilityTooltip, Config.CLIENT.durabilityTooltipMode.get());
        }

        // Tooltip - Enchantability
        int enchantability = itemStack.getEnchantmentValue();
        if (enchantability > 0) {
            Component enchantabilityTooltip = Component.translatable("tooltip.jeiintegration.enchantability")
                    .append(" " + enchantability)
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, enchantabilityTooltip, Config.CLIENT.enchantabilityTooltipMode.get());
        }

        // Tooltip - Hunger / Saturation
        FoodProperties foodProperties = item.getFoodProperties(itemStack, player);
        if (item.isEdible() && foodProperties != null) {
            int healVal = foodProperties.getNutrition();
            float satVal = healVal * (foodProperties.getSaturationModifier() * 2);

            Component foodTooltip = Component.translatable("tooltip.jeiintegration.hunger")
                    .append(" " + healVal + " ")
                    .append(Component.translatable("tooltip.jeiintegration.saturation"))
                    .append(" " + DECIMALFORMAT.format(satVal))
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, foodTooltip, Config.CLIENT.foodTooltipMode.get());
        }

        // Tooltip - NBT Data
        CompoundTag nbtData = item.getShareTag(itemStack);
        if (nbtData != null) {
            Component nbtTooltip = Component.translatable("tooltip.jeiintegration.nbtTagData")
                    .append(" " + nbtData)
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, nbtTooltip, Config.CLIENT.nbtTooltipMode.get());
        }

        // Tooltip - Registry Name
        Component registryTooltip = Component.translatable("tooltip.jeiintegration.registryName")
                .append(" " + ForgeRegistries.ITEMS.getKey(item))
                .withStyle(ChatFormatting.DARK_GRAY);

        registerTooltip(e, registryTooltip, Config.CLIENT.registryNameTooltipMode.get());


        // Tooltip - Max Stack Size
        int stackSize = itemStack.getMaxStackSize();
        if (stackSize > 0) {
            Component stackSizeTooltip = Component.translatable("tooltip.jeiintegration.maxStackSize")
                    .append(" " + stackSize)
                    .withStyle(ChatFormatting.DARK_GRAY);

            registerTooltip(e, stackSizeTooltip, Config.CLIENT.maxStackSizeTooltipMode.get());
        }

        // Tooltip - Tags
        if (itemStack.getTags().findAny().isPresent()) {
            Mode mode = Config.CLIENT.tagsTooltipMode.get();

            Component tagsTooltip = Component.translatable("tooltip.jeiintegration.tags")
                    .withStyle(ChatFormatting.DARK_GRAY);
            registerTooltip(e, tagsTooltip, mode);

            itemStack.getTags()
                .map(TagKey::location)
                .sorted((rl1, rl2) -> {
                    String namespaceRl1 = rl1.getNamespace();
                    String namespaceRl2 = rl2.getNamespace();
                    if (namespaceRl1.equals("minecraft") && !namespaceRl2.equals("minecraft")) return -1;
                    if (namespaceRl2.equals("minecraft") && !namespaceRl1.equals("minecraft")) return 1;
                    return rl1.toString().compareTo(rl2.toString());
                })
                .map(tag -> Component.literal("    " + tag))
                .map(tag -> tag.withStyle(ChatFormatting.DARK_GRAY))
                .forEachOrdered(tooltip -> registerTooltip(e, tooltip, mode));
        }

        // Tooltip - Translation Key
        Component translationKeyTooltip = Component.translatable("tooltip.jeiintegration.translationKey")
                .append(Component.literal(" " + itemStack.getDescriptionId()))
                .withStyle(ChatFormatting.DARK_GRAY);

        registerTooltip(e, translationKeyTooltip, Config.CLIENT.translationKeyTooltipMode.get());
    }
}
