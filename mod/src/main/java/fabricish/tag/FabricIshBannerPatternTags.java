package fabricish.tag;

import fabricish.FabricIshMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class FabricIshBannerPatternTags {
    public static final TagKey<BannerPattern> CONCERN_PATTERN_ITEM = create("pattern_item/concern");
    public static final TagKey<BannerPattern> FABRIC_PATTERN_ITEM = create("pattern_item/fabric");
    public static final TagKey<BannerPattern> TATER_PATTERN_ITEM = create("pattern_item/tater");

    private FabricIshBannerPatternTags() {
    }

    public static TagKey<BannerPattern> create(String path) {
        return TagKey.create(Registries.BANNER_PATTERN, FabricIshMod.id(path));
    }
}
