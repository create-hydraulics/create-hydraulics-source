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
 * A "fisting" recipe for the Hydraulic Fist: one item input &rarr; one item output, where raw mechanical
 * force is applied to the input.
 *
 * <p>Mirrors {@link HydraulicPressRecipe} in shape (a single ingredient in, a single item out) but is its own
 * recipe type ({@code createhydraulics:fisting}) so the Fist and the Press keep distinct recipe pools. The Fist
 * uses these recipes <b>two</b> ways:</p>
 * <ul>
 *   <li><b>On belts &amp; depots</b> &ndash; the item passing under (or resting on) the target block is matched
 *       as a {@link SingleRecipeInput} and {@link #assemble assembled} into the result, exactly like the Press
 *       does with a depot.</li>
 *   <li><b>On blocks in the world</b> &ndash; the Fist looks up the targeted block <i>as an item</i>
 *       ({@code block.asItem()}); if a fisting recipe matches and its result is itself a block item, the block is
 *       transformed in place (e.g. Stone&rarr;Cobblestone). One recipe therefore drives both the in-world crush
 *       and the on-belt crush, so authors only define the transformation once.</li>
 * </ul>
 *
 * <p>Recipes live in JSON at {@code data/createhydraulics/recipe/fisting/*.json} with
 * {@code "type": "createhydraulics:fisting"}.</p>
 */
public class HydraulicFistRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient input;
    private final ItemStack result;
    /** Cosmetic-only on this machine for now; kept so recipes can carry a duration like Create's do. */
    private final int processingTime;

    public HydraulicFistRecipe(Ingredient input, ItemStack result, int processingTime) {
        this.input = input;
        this.result = result;
        this.processingTime = processingTime;
    }

    /** The single ingredient this recipe crushes. */
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
        return ModRecipes.HYDRAULIC_FISTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.HYDRAULIC_FISTING.get();
    }

    /**
     * (De)serializer for {@link HydraulicFistRecipe}: a single {@code ingredient} and a single {@code result}
     * item (plus an optional {@code processing_time}), identical in shape to the pressing serializer.
     */
    public static class Serializer implements RecipeSerializer<HydraulicFistRecipe> {

        public static final MapCodec<HydraulicFistRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(
                        Ingredient.CODEC.fieldOf("ingredient").forGetter(HydraulicFistRecipe::getInput),
                        ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                        net.minecraft.util.ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_time", 100)
                                .forGetter(HydraulicFistRecipe::getProcessingTime))
                .apply(instance, HydraulicFistRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, HydraulicFistRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, HydraulicFistRecipe::getInput,
                        ItemStack.STREAM_CODEC, r -> r.result,
                        ByteBufCodecs.VAR_INT, HydraulicFistRecipe::getProcessingTime,
                        HydraulicFistRecipe::new);

        @Override
        public MapCodec<HydraulicFistRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HydraulicFistRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
