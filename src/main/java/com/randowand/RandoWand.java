package com.randowand;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod("randowand")
public class RandoWand {
    public static final String MOD_ID = "randowand";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<Item> RANDO_WAND = ITEMS.register("randowand",
            () -> new ModItem(new Item.Properties().stacksTo(1).durability(1000)));

    public static final RegistryObject<CreativeModeTab> RANDO_WAND_TAB = CREATIVE_MODE_TABS.register("randowand_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.randowand"))
                    .icon(() -> RANDO_WAND.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(RANDO_WAND.get());
                    }).build());

    public RandoWand(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RANDO_WAND.get());
        }
    }
}