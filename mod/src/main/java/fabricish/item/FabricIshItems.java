package fabricish.item;

import fabricish.tag.FabricIshBannerPatternTags;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Function;

public class FabricIshItems {
    public static final Item CONCERN_BANNER_PATTERN = register(FabricIshItemIds.CONCERN_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.CONCERN_PATTERN_ITEM)));
    public static final Item FABRIC_BANNER_PATTERN = register(FabricIshItemIds.FABRIC_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.FABRIC_PATTERN_ITEM)));
    public static final Item TATER_BANNER_PATTERN = register(FabricIshItemIds.TATER_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.TATER_PATTERN_ITEM)));

    private FabricIshItems() {
    }

    public static Item register(ResourceKey<Item> itemKey, Item.Properties properties) {
        return register(itemKey, Item::new, properties);
    }

    public static Item register(ResourceKey<Item> itemKey, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
        Item item = itemFactory.apply(properties.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
            output.insertAfter(Items.MOJANG_BANNER_PATTERN, CONCERN_BANNER_PATTERN, FABRIC_BANNER_PATTERN, TATER_BANNER_PATTERN)
        );
    }
}
