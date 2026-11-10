package fabricish.item;

import fabricish.FabricIshMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class FabricIshItemIds {
    public static final ResourceKey<Item> CONCERN_BANNER_PATTERN = create("concern_banner_pattern");
    public static final ResourceKey<Item> FABRIC_BANNER_PATTERN = create("fabric_banner_pattern");
    public static final ResourceKey<Item> TATER_BANNER_PATTERN = create("tater_banner_pattern");

    private FabricIshItemIds() {
    }

    public static ResourceKey<Item> create(String path) {
        return ResourceKey.create(Registries.ITEM, FabricIshMod.id(path));
    }
}
