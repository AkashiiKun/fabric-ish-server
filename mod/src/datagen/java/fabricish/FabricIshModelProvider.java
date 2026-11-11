package fabricish;

import fabricish.block.FabricIshBlocks;
import fabricish.item.FabricIshItems;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

public class FabricIshModelProvider extends FabricModelProvider {
    public FabricIshModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        generator.createNonTemplateHorizontalBlock(FabricIshBlocks.TINY_POTATO);
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.generateFlatItem(FabricIshItems.CONCERN_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(FabricIshItems.FABRIC_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(FabricIshItems.TATER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
