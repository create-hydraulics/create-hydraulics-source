package com.createhydro.hydro.compat.jei;

import com.createhydro.hydro.recipe.HydraulicFistRecipe;
import com.createhydro.hydro.registry.ModBlocks;
import com.createhydro.hydro.registry.ModRecipes;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI category for the Hydraulic Fist's {@code createhydraulics:fisting} recipes (one item in &rarr; one item
 * out). Identical in shape to {@link HydraulicPressingCategory}: the recipe type is bridged from the vanilla
 * {@link ModRecipes#HYDRAULIC_FISTING} type with {@link RecipeType#createFromVanilla}, so JEI works in terms of
 * {@link RecipeHolder}s and picks up each recipe's registry id for free.
 *
 * <p>The layout is deliberately simple &ndash; an input slot, an arrow, and an output slot &ndash; drawn with
 * JEI's built-in slot/arrow sprites, so no bespoke background texture is needed.</p>
 */
public class HydraulicFistingCategory implements IRecipeCategory<RecipeHolder<HydraulicFistRecipe>> {

    /** JEI recipe type, bridged from our vanilla recipe type so recipes carry their ids into JEI. */
    public static final RecipeType<RecipeHolder<HydraulicFistRecipe>> RECIPE_TYPE =
            RecipeType.createFromVanilla(ModRecipes.HYDRAULIC_FISTING.get());

    private static final int WIDTH = 80;
    private static final int HEIGHT = 26;
    private static final int INPUT_X = 4;
    private static final int OUTPUT_X = WIDTH - 18 - 4; // 58 — leaves a 4px right margin
    private static final int SLOT_Y = 4;
    private static final int ARROW_X = 28;
    private static final int ARROW_Y = 5;

    private final IDrawable icon;
    private final IDrawableStatic slot;
    private final IDrawableStatic arrow;

    public HydraulicFistingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.HYDRAULIC_FIST.get()));
        this.slot = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<HydraulicFistRecipe>> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("createhydraulics.gui.jei.fisting");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<HydraulicFistRecipe> holder, IFocusGroup focuses) {
        HydraulicFistRecipe recipe = holder.value();
        builder.addInputSlot(INPUT_X, SLOT_Y)
                .addIngredients(recipe.getInput())
                .setBackground(slot, -1, -1);
        // getResultItem ignores its provider (it returns the stored stack), so EMPTY is a safe, non-null arg.
        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.getResultItem(RegistryAccess.EMPTY))
                .setBackground(slot, -1, -1);
    }

    @Override
    public void draw(RecipeHolder<HydraulicFistRecipe> holder, IRecipeSlotsView slotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        arrow.draw(graphics, ARROW_X, ARROW_Y);
    }
}
