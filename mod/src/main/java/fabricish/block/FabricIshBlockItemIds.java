package fabricish.block;

import fabricish.FabricIshMod;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

public class FabricIshBlockItemIds {
    public static final BlockItemId TINY_POTATO = create("tiny_potato");

    private static BlockItemId create(String name) {
        Identifier id = FabricIshMod.id(name);
        return BlockItemId.create(id, id);
    }
}
