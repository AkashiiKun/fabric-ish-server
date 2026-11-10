package fabricish;

import fabricish.item.FabricIshItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

public class FabricIshRecipeProvider extends FabricRecipeProvider {
    public FabricIshRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override
            public void buildRecipes() {
                this.shapeless(RecipeCategory.MISC, FabricIshItems.CONCERN_BANNER_PATTERN)
                    .requires(Items.PAPER)
                    .requires(Items.SOUL_SAND)
                    .unlockedBy(getHasName(Items.SOUL_SAND), this.has(Items.SOUL_SAND))
                    .save(this.output);
                this.shapeless(RecipeCategory.MISC, FabricIshItems.FABRIC_BANNER_PATTERN)
                    .requires(Items.PAPER)
                    .requires(Items.LOOM)
                    .unlockedBy(getHasName(Items.LOOM), this.has(Items.LOOM))
                    .save(this.output);
                this.shapeless(RecipeCategory.MISC, FabricIshItems.TATER_BANNER_PATTERN)
                    .requires(Items.PAPER)
                    .requires(Items.POTATO)
                    .unlockedBy(getHasName(Items.POTATO), this.has(Items.POTATO))
                    .save(this.output);

            }
        };
    }
}
