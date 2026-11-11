package fabricish;

import fabricish.item.FabricIshItemIds;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;

import java.util.concurrent.CompletableFuture;

public class FabricIshItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public FabricIshItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        builder(ItemTags.LOOM_PATTERNS)
            .add(FabricIshItemIds.FABRIC_BANNER_PATTERN)
            .add(FabricIshItemIds.TATER_BANNER_PATTERN)
            .add(FabricIshItemIds.CONCERN_BANNER_PATTERN);
    }
}
