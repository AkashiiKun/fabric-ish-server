package fabricish.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

public class FabricIshBlocks {
    public static final Block TINY_POTATO = register(
        FabricIshBlockItemIds.TINY_POTATO,
        TinyPotatoBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.RAW_IRON)
            .instabreak()
            .noOcclusion()
            .sound(SoundType.CROP)
    );

    /// Registers a Block and an item from a given BlockItemId
    private static Block registerWithItem(BlockItemId id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
        // Create the block instance
        Block block = register(id, blockFactory, properties);

        // Create the block item instance
        BlockItem blockItem = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);

        return block;
    }

    /// Registers a Block from a given BlockItemId
    private static Block register(BlockItemId id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
        return register(id.block(), blockFactory, properties);
    }

    /// Registers a Block from a given ResourceKey
    private static Block register(ResourceKey<Block> id, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
        // Create the block instance
        Block block = blockFactory.apply(properties.setId(id));

        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    public static void initialize() {
    }
}
