package com.randowand;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = RandoWand.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue WAND_DURABILITY = BUILDER
            .comment("Maximum durability (uses) for the RandoWand. Set to 0 or lower for unbreakable.")
            .defineInRange("wandDurability", 1000, -1, 32767);

    private static final ForgeConfigSpec.IntValue WAND_MAX_STACK_SIZE = BUILDER
            .comment("Maximum stack size for the RandoWand item.")
            .defineInRange("wandMaxStackSize", 1, 1, 64);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int wandDurability;
    public static int wandMaxStackSize;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        wandDurability = WAND_DURABILITY.get();
        wandMaxStackSize = WAND_MAX_STACK_SIZE.get();
    }
}