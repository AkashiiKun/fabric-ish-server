package fabricish;

import fabricish.block.FabricIshBannerPatterns;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class DataGenEntrypoint implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator dataGenerator) {
        FabricDataGenerator.Pack pack = dataGenerator.createPack();

        pack.addProvider(FabricIshBannerPatternProvider::new);
        pack.addProvider(FabricIshBannerPatternTagProvider::new);
        pack.addProvider(FabricIshBlockLootTableProvider::new);
        pack.addProvider(FabricIshModelProvider::new);
        pack.addProvider(FabricIshItemTagProvider::new);
        pack.addProvider(FabricIshRecipeProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.BANNER_PATTERN, FabricIshBannerPatterns::bootstrap);
    }
}
