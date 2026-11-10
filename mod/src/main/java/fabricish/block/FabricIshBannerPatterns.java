package fabricish.block;

import fabricish.FabricIshMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BannerPattern;

public class FabricIshBannerPatterns {
    public static final ResourceKey<BannerPattern> CONCERN = create("concern");
    public static final ResourceKey<BannerPattern> FABRIC = create("fabric");
    public static final ResourceKey<BannerPattern> TATER = create("tater");

    private FabricIshBannerPatterns() {
    }

    public static ResourceKey<BannerPattern> create(String path) {
        return ResourceKey.create(Registries.BANNER_PATTERN, FabricIshMod.id(path));
    }

    public static void bootstrap(final BootstrapContext<BannerPattern> context) {
        register(context, CONCERN);
        register(context, FABRIC);
        register(context, TATER);
    }

    public static void register(final BootstrapContext<BannerPattern> context, final ResourceKey<BannerPattern> key) {
        context.register(key, new BannerPattern(key.identifier(), "block.minecraft.banner." + key.identifier().toShortLanguageKey()));
    }
}
