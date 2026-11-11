package fabricish;

import fabricish.block.FabricIshBannerPatterns;
import fabricish.tag.FabricIshBannerPatternTags;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.concurrent.CompletableFuture;

public class FabricIshBannerPatternTagProvider extends FabricTagsProvider<BannerPattern> {
    public FabricIshBannerPatternTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, Registries.BANNER_PATTERN, registryLookupFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.builder(FabricIshBannerPatternTags.FABRIC_PATTERN_ITEM)
            .add(FabricIshBannerPatterns.FABRIC);

        this.builder(FabricIshBannerPatternTags.TATER_PATTERN_ITEM)
            .add(FabricIshBannerPatterns.TATER);

        this.builder(FabricIshBannerPatternTags.CONCERN_PATTERN_ITEM)
            .add(FabricIshBannerPatterns.CONCERN);

    }
}
