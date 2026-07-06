package com.createhydro.hydro.registry;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.recipe.HydraulicFistRecipe;
import com.createhydro.hydro.recipe.HydraulicPressRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe types and serializers added by Create: Hydraulics. */
public final class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CreateHydraulics.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CreateHydraulics.MODID);

    /** The Hydraulic Press recipe type — {@code createhydraulics:pressing}. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<HydraulicPressRecipe>> HYDRAULIC_PRESSING =
            RECIPE_TYPES.register("pressing", id -> new RecipeType<>() {
                @Override
                public String toString() {
                    return id.toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HydraulicPressRecipe>> HYDRAULIC_PRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("pressing", HydraulicPressRecipe.Serializer::new);

    /** The Hydraulic Fist recipe type — {@code createhydraulics:fisting}. */
    public static final DeferredHolder<RecipeType<?>, RecipeType<HydraulicFistRecipe>> HYDRAULIC_FISTING =
            RECIPE_TYPES.register("fisting", id -> new RecipeType<>() {
                @Override
                public String toString() {
                    return id.toString();
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HydraulicFistRecipe>> HYDRAULIC_FISTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("fisting", HydraulicFistRecipe.Serializer::new);

    private ModRecipes() {}

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
