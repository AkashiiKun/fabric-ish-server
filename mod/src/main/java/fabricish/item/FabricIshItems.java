package fabricish.item;

import fabricish.block.FabricIshBlockItemIds;
import fabricish.block.FabricIshBlocks;
import fabricish.tag.FabricIshBannerPatternTags;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;
import java.util.function.Function;

public class FabricIshItems {
    public static final Item CONCERN_BANNER_PATTERN = register(FabricIshItemIds.CONCERN_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.CONCERN_PATTERN_ITEM)));
    public static final Item FABRIC_BANNER_PATTERN = register(FabricIshItemIds.FABRIC_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.FABRIC_PATTERN_ITEM)));
    public static final Item TATER_BANNER_PATTERN = register(FabricIshItemIds.TATER_BANNER_PATTERN, new Item.Properties().stacksTo(1).delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, (context) -> context.getOrThrow(FabricIshBannerPatternTags.TATER_PATTERN_ITEM)));

    public static final Item TINY_POTATO = registerBlock(FabricIshBlockItemIds.TINY_POTATO, FabricIshBlocks.TINY_POTATO, BlockItem::new, new Item.Properties().equippableUnswappable(EquipmentSlot.HEAD));

    private FabricIshItems() {
    }

    /// Registers an Item from the given BlockItem
    private static Item registerBlock(
        final BlockItemId id, final Block block, final BiFunction<Block, Item.Properties, Item> itemFactory, final Item.Properties properties
    ) {
        return register(id.item(), p -> itemFactory.apply(block, p), properties.useBlockDescriptionPrefix().requiredFeatures(block.requiredFeatures()));
    }

    public static Item register(ResourceKey<Item> itemKey, Item.Properties properties) {
        return register(itemKey, Item::new, properties);
    }

    public static Item register(ResourceKey<Item> itemKey, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
        Item item = itemFactory.apply(properties.setId(itemKey));

        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
            output.insertAfter(Items.MOJANG_BANNER_PATTERN, CONCERN_BANNER_PATTERN, FABRIC_BANNER_PATTERN, TATER_BANNER_PATTERN)
        );
    }
}
