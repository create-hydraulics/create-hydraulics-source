package com.createhydro.hydro.recipe;

import com.createhydro.hydro.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A pressing recipe for the Hydraulic Press: one item input → one item output.
 *
 * <p>This is a deliberately small, self-contained recipe modelled on Create's
 * {@code PressingRecipe} (one ingredient in, an item out) but without Create's heavier
 * {@code ProcessingRecipe} machinery &ndash; we don't need rollable outputs, fluids, heat, or
 * sequenced-assembly support here. The input is matched against the item sitting on the
 * {@code DepotBlock} directly below the press (a {@link SingleRecipeInput}, exactly like Create's
 * press), and {@link #assemble} yields the result that replaces it.</p>
 *
 * <p>Recipes live in JSON at {@code data/createhydraulics/recipe/pressing/*.json} with
 * {@code "type": "createhydraulics:pressing"}.</p>
 */
public class HydraulicPressRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient input;
    private final ItemStack result;
    /** Cosmetic-only on this machine for now; kept so recipes can carry a duration like Create's do. */
    private final int processingTime;

    public HydraulicPressRecipe(Ingredient input, ItemStack result, int processingTime) {
        this.input = input;
        this.result = result;
        this.processingTime = processingTime;
    }

    /** The single ingredient this recipe presses. */
    public Ingredient getInput() {
        return input;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    @Override
    public boolean matches(SingleRecipeInput inv, Level level) {
        if (inv.isEmpty()) {
            return false;
        }
        return input.test(inv.getItem(0));
    }

    @Override
    public ItemStack assemble(SingleRecipeInput inv, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    /** Machine recipe — keep it out of the vanilla recipe book. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HYDRAULIC_PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HYDRAULIC_PRESSING.get();
    }

    /**
     * (De)serializer for {@link HydraulicPressRecipe}. The JSON shape mirrors Create's pressing recipe
     * closely: a single {@code ingredient} and a single {@code result} item (plus an optional
     * {@code processing_time}).
     */
    public static class Serializer implements RecipeSerializer<HydraulicPressRecipe> {

        public static final MapCodec<HydraulicPressRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(
                        Ingredient.CODEC.fieldOf("ingredient").forGetter(HydraulicPressRecipe::getInput),
                        ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                        net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_time", 100)
                                .forGetter(HydraulicPressRecipe::getProcessingTime))
                .apply(instance, HydraulicPressRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, HydraulicPressRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, HydraulicPressRecipe::getInput,
                        ItemStack.STREAM_CODEC, r -> r.result,
                        ByteBufCodecs.VAR_INT, HydraulicPressRecipe::getProcessingTime,
                        HydraulicPressRecipe::new);

        @Override
        public MapCodec<HydraulicPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HydraulicPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
