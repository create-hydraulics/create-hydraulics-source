package com.createhydro.hydro.compat.jei;

import java.util.List;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.recipe.HydraulicFistRecipe;
import com.createhydro.hydro.recipe.HydraulicPressRecipe;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI integration for Create: Hydraulics.
 *
 * <p>Plain crafting recipes (pipe, valve, gauge, intakes, press) are shown by JEI automatically. This plugin
 * adds the one thing JEI cannot infer on its own: the Hydraulic Press's custom {@code createhydraulics:pressing}
 * recipes, surfaced in their own {@link HydraulicPressingCategory} with the press registered as the catalyst
 * (so looking up the press shows what it can make, and looking up a result shows the press).</p>
 *
 * <p>This class only loads when JEI is installed (JEI discovers it by the {@link JeiPlugin} annotation); the JEI
 * API is a compile-time-only dependency, so the mod runs fine without JEI present.</p>
 */
@JeiPlugin
public class CreateHydraulicsJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateHydraulics.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new HydraulicPressingCategory(guiHelper),
                new HydraulicFistingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return; // recipes live on the connected level's recipe manager; nothing to add before joining a world
        }
        List<RecipeHolder<HydraulicPressRecipe>> pressing =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.HYDRAULIC_PRESSING.get());
        registration.addRecipes(HydraulicPressingCategory.RECIPE_TYPE, pressing);

        List<RecipeHolder<HydraulicFistRecipe>> fisting =
                level.getRecipeManager().getAllRecipesFor(ModRecipes.HYDRAULIC_FISTING.get());
        registration.addRecipes(HydraulicFistingCategory.RECIPE_TYPE, fisting);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.HYDRAULIC_PRESS.get(), HydraulicPressingCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(ModBlocks.HYDRAULIC_FIST.get(), HydraulicFistingCategory.RECIPE_TYPE);
    }
}
