package com.createhydro.hydro.block.entity;

import java.util.List;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.HydraulicAssemblyUnitBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.createhydro.hydro.registry.ModFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

// assembles any recipe that yields the filtered result. needs pressure + fluid, deliberately slow.
public class HydraulicAssemblyUnitBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    public static final int TANK_CAPACITY = 1500;
    public static final float ASSEMBLY_PU_COST = 600.0F;
    public static final int BASE_ASSEMBLY_DURATION = 80; // ~4s for a single-ingredient recipe
    public static final int PER_INGREDIENT_TICKS = 20;
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 9;
    private static final int RECIPE_SCAN_INTERVAL = 10;

    private final ItemStackHandler inputInv = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            inputsDirty = true;
            currentRecipe = null;
            setChanged();
        }
    };

    private final ItemStackHandler outputInv = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // insert goes to input, extract comes from output — keeps funnels from pulling raw ingredients back out
    private final IItemHandler externalItems = new AssemblyItemHandler(inputInv, outputInv);

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                updateFullState();
                sendData();
            }
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == ModFluids.HYDRO_FLUID.get()
                    || stack.getFluid() == ModFluids.HYDRO_FLUID_FLOWING.get();
        }
    };

    public FilteringBehaviour filtering;

    private boolean running = false;
    private int progress = 0;
    private int assemblyDuration = BASE_ASSEMBLY_DURATION;
    private boolean underpressured = false;
    private int syncCooldown = 0;

    private RecipeHolder<?> currentRecipe = null;
    private boolean inputsDirty = true;
    private int recipeScanCooldown = 0;

    public HydraulicAssemblyUnitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYDRAULIC_ASSEMBLY_UNIT.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        filtering = new FilteringBehaviour(this, new AssemblyFilterSlot())
                .withCallback(newFilter -> {
                    // selecting a different result invalidates the cached recipe
                    currentRecipe = null;
                    inputsDirty = true;
                });
        filtering.setLabel(Component.translatable("createhydraulics.gui.assembly_unit.filter"));
        behaviours.add(filtering);
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public IItemHandler getItemHandler() {
        return externalItems;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isUnderpressured() {
        return underpressured;
    }

    public void setFilter(ItemStack stack) {
        if (filtering == null) {
            return;
        }
        filtering.setFilter(stack.copyWithCount(1));
        currentRecipe = null;
        inputsDirty = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        serverTick();
    }

    private void serverTick() {
        absorbItemsFromAbove();

        float available = readAvailablePressure();
        RunState state = evaluate(available);

        boolean nowUnder = state == RunState.UNDERPRESSURED;
        if (nowUnder != underpressured) {
            underpressured = nowUnder;
            sendData();
        }

        if (state == RunState.READY) {
            if (!running) {
                startRunning();
            }
            progress++;
            // push progress to clients periodically so the goggles overlay stays accurate
            if (--syncCooldown <= 0) {
                syncCooldown = 10;
                sendData();
            }
            if (progress >= assemblyDuration) {
                assemble();
                progress = 0;
            }
        } else if (running) {
            stopRunning();
        }
    }

    private void startRunning() {
        running = true;
        progress = 0;
        assemblyDuration = computeDuration(currentRecipe);
        syncCooldown = 0;
        updateRunningState();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.35F, 0.6F);
        }
        sendData();
    }

    private void stopRunning() {
        running = false;
        progress = 0;
        updateRunningState();
        sendData();
    }

    private void absorbItemsFromAbove() {
        AABB box = new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.getItem().isEmpty());
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            ItemStack remainder = ItemHandlerHelper.insertItem(inputInv, stack.copy(), false);
            if (remainder.getCount() != stack.getCount()) {
                if (remainder.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(remainder);
                }
            }
        }
    }

    private float readAvailablePressure() {
        if (level.getBlockEntity(worldPosition.below()) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.UP)) {
            return pipe.getPressure();
        }
        return 0.0F;
    }

    private boolean hasPressureLine() {
        return level.getBlockEntity(worldPosition.below()) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.UP);
    }

    private enum RunState { READY, NO_FLUID, NO_RECIPE, OUTPUT_FULL, UNPOWERED, UNDERPRESSURED }

    private RunState evaluate(float available) {
        if (tank.getFluid().isEmpty()) {
            return RunState.NO_FLUID;
        }
        if (!hasPressureLine()) {
            return RunState.UNPOWERED;
        }
        if (available < ASSEMBLY_PU_COST) {
            return RunState.UNDERPRESSURED;
        }
        RecipeHolder<?> recipe = resolveRecipe();
        if (recipe == null) {
            return RunState.NO_RECIPE;
        }
        if (!hasOutputSpaceFor(recipe)) {
            return RunState.OUTPUT_FULL;
        }
        return RunState.READY;
    }

    private RecipeHolder<?> resolveRecipe() {
        ItemStack target = filtering != null ? filtering.getFilter() : ItemStack.EMPTY;
        if (target.isEmpty()) {
            currentRecipe = null;
            return null;
        }
        if (currentRecipe != null && matchesFilter(currentRecipe, target) && consumptionFor(currentRecipe) != null) {
            return currentRecipe;
        }
        currentRecipe = null;
        if (recipeScanCooldown > 0) {
            recipeScanCooldown--;
            if (!inputsDirty) {
                return null;
            }
        }
        recipeScanCooldown = RECIPE_SCAN_INTERVAL;
        inputsDirty = false;
        currentRecipe = scanForRecipe(target);
        return currentRecipe;
    }

    private RecipeHolder<?> scanForRecipe(ItemStack target) {
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            if (!matchesFilter(holder, target)) {
                continue;
            }
            if (consumptionFor(holder) != null) {
                return holder;
            }
        }
        return null;
    }

    private boolean matchesFilter(RecipeHolder<?> holder, ItemStack target) {
        Recipe<?> recipe = holder.value();
        ItemStack result;
        try {
            result = recipe.getResultItem(level.registryAccess());
        } catch (Exception ignored) {
            return false;
        }
        return result != null && !result.isEmpty() && ItemStack.isSameItem(result, target);
    }

    private int[] consumptionFor(RecipeHolder<?> holder) {
        List<Ingredient> ingredients = holder.value().getIngredients();
        if (ingredients.isEmpty()) {
            return null; // nothing to consume = free craft, skip
        }

        int[] remaining = new int[INPUT_SLOTS];
        int[] consume = new int[INPUT_SLOTS];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            remaining[i] = inputInv.getStackInSlot(i).getCount();
        }

        boolean anyReal = false;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                continue; // shaped recipes pad empty grid cells
            }
            anyReal = true;
            int matchSlot = -1;
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (remaining[i] <= 0) {
                    continue;
                }
                if (ingredient.test(inputInv.getStackInSlot(i))) {
                    matchSlot = i;
                    break;
                }
            }
            if (matchSlot < 0) {
                return null; // an ingredient could not be satisfied
            }
            remaining[matchSlot]--;
            consume[matchSlot]++;
        }
        return anyReal ? consume : null;
    }

    private int computeDuration(RecipeHolder<?> recipe) {
        if (recipe == null) {
            return BASE_ASSEMBLY_DURATION;
        }
        int[] consume = consumptionFor(recipe);
        if (consume == null) {
            return BASE_ASSEMBLY_DURATION;
        }
        int distinctSlots = 0;
        for (int c : consume) {
            if (c > 0) distinctSlots++;
        }
        return BASE_ASSEMBLY_DURATION + Math.max(0, distinctSlots - 1) * PER_INGREDIENT_TICKS;
    }

    private boolean hasOutputSpaceFor(RecipeHolder<?> holder) {
        ItemStack result = holder.value().getResultItem(level.registryAccess());
        if (result.isEmpty()) {
            return false;
        }
        // simulate=true never mutates the handler
        return ItemHandlerHelper.insertItem(outputInv, result.copy(), true).isEmpty();
    }

    private void assemble() {
        if (currentRecipe == null) {
            return;
        }
        int[] consume = consumptionFor(currentRecipe);
        if (consume == null) {
            return;
        }
        ItemStack result = currentRecipe.value().getResultItem(level.registryAccess()).copy();
        if (result.isEmpty()) {
            return;
        }
        // verify space one more time before committing
        if (!ItemHandlerHelper.insertItem(outputInv, result.copy(), true).isEmpty()) {
            return;
        }
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (consume[i] > 0) {
                inputInv.extractItem(i, consume[i], false);
            }
        }
        ItemHandlerHelper.insertItem(outputInv, result, false);
        currentRecipe = null;
        inputsDirty = true;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
        setChanged();
        sendData();
    }

    private void updateFullState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        boolean shouldBeFull = !tank.getFluid().isEmpty();
        if (state.hasProperty(HydraulicAssemblyUnitBlock.FULL)
                && state.getValue(HydraulicAssemblyUnitBlock.FULL) != shouldBeFull) {
            BlockState updated = state.setValue(HydraulicAssemblyUnitBlock.FULL, shouldBeFull);
            if (!shouldBeFull) {
                updated = updated.setValue(HydraulicAssemblyUnitBlock.RUNNING, false);
            }
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    private void updateRunningState() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        // only show the "on" texture while the tank is full (running without fluid shouldn't happen, but guard it)
        boolean shouldRun = running && !tank.getFluid().isEmpty();
        if (state.hasProperty(HydraulicAssemblyUnitBlock.RUNNING)
                && state.getValue(HydraulicAssemblyUnitBlock.RUNNING) != shouldRun) {
            level.setBlock(worldPosition, state.setValue(HydraulicAssemblyUnitBlock.RUNNING, shouldRun),
                    Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("gui.goggles.assembly_unit").forGoggles(tooltip);

        float available = level != null ? readAvailablePressure() : 0.0F;
        ItemStack target = filtering != null ? filtering.getFilter() : ItemStack.EMPTY;

        if (underpressured) {
            statusLine(tooltip, ChatFormatting.RED, "underpressured");
        } else if (running) {
            statusLine(tooltip, ChatFormatting.GREEN, "assembling");
        } else if (tank.getFluid().isEmpty()) {
            statusLine(tooltip, ChatFormatting.GRAY, "no_fluid");
        } else if (target.isEmpty()) {
            statusLine(tooltip, ChatFormatting.GRAY, "no_filter");
        } else {
            statusLine(tooltip, ChatFormatting.GRAY, "idle");
        }

        if (!target.isEmpty()) {
            lang().add(lang().translate("gui.assembly_unit.target").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(target.getHoverName().getString()).style(ChatFormatting.WHITE))
                    .forGoggles(tooltip, 1);
        }

        if (running) {
            int percent = assemblyDuration > 0 ? Math.min(100, (int) (100L * progress / assemblyDuration)) : 0;
            lang().add(lang().translate("gui.assembly_unit.progress").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(percent + "%").style(ChatFormatting.GOLD))
                    .forGoggles(tooltip, 1);
        }

        lang().add(lang().translate("gui.assembly_unit.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(available)).style(ChatFormatting.GOLD))
                .add(lang().text(ChatFormatting.DARK_GRAY, " / " + LangNumberFormat.format(ASSEMBLY_PU_COST) + " PU"))
                .forGoggles(tooltip, 1);

        lang().add(lang().translate("gui.assembly_unit.fluid").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(tank.getFluidAmount())).style(ChatFormatting.AQUA))
                .add(lang().text(ChatFormatting.DARK_GRAY, " / " + LangNumberFormat.format(TANK_CAPACITY) + " mB"))
                .forGoggles(tooltip, 1);
        return true;
    }

    private static void statusLine(List<Component> tooltip, ChatFormatting color, String key) {
        lang().add(lang().text(color, "■ "))
                .add(lang().translate("gui.assembly_unit." + key).style(ChatFormatting.GRAY))
                .forGoggles(tooltip, 1);
    }

    private static LangBuilder lang() {
        return new LangBuilder(CreateHydraulics.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.put("Input", inputInv.serializeNBT(registries));
        tag.put("Output", outputInv.serializeNBT(registries));
        tag.putBoolean("Running", running);
        tag.putInt("Progress", progress);
        tag.putInt("AssemblyDuration", assemblyDuration);
        tag.putBoolean("Underpressured", underpressured);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        inputInv.deserializeNBT(registries, tag.getCompound("Input"));
        outputInv.deserializeNBT(registries, tag.getCompound("Output"));
        running = tag.getBoolean("Running");
        progress = tag.getInt("Progress");
        assemblyDuration = tag.contains("AssemblyDuration") ? tag.getInt("AssemblyDuration") : BASE_ASSEMBLY_DURATION;
        underpressured = tag.getBoolean("Underpressured");
    }

    @Override
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide) {
            return;
        }
        Vec3 c = VecHelper.getCenterOf(worldPosition);
        dropAll(inputInv, c);
        dropAll(outputInv, c);
    }

    private void dropAll(ItemStackHandler inv, Vec3 pos) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy()));
            }
        }
    }

    private static class AssemblyFilterSlot extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            // 16.05 places the box just outside the face so it isn't occluded by model geometry
            return VecHelper.voxelSpace(8, 8, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }
    }

    private static final class AssemblyItemHandler implements IItemHandler {
        private final ItemStackHandler input;
        private final ItemStackHandler output;

        AssemblyItemHandler(ItemStackHandler input, ItemStackHandler output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public int getSlots() {
            return input.getSlots() + output.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot < input.getSlots()
                    ? input.getStackInSlot(slot)
                    : output.getStackInSlot(slot - input.getSlots());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < input.getSlots()) {
                return input.insertItem(slot, stack, simulate);
            }
            return stack; // never insert into the output buffer
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < input.getSlots()) {
                return ItemStack.EMPTY; // never extract raw ingredients
            }
            return output.extractItem(slot - input.getSlots(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < input.getSlots()
                    ? input.getSlotLimit(slot)
                    : output.getSlotLimit(slot - input.getSlots());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < input.getSlots() && input.isItemValid(slot, stack);
        }
    }
}
