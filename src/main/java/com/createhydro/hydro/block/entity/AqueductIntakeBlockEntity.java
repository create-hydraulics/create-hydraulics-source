package com.createhydro.hydro.block.entity;

import java.util.List;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.AqueductIntakeBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// submerged pressure source: pushes OUTPUT_PRESSURE PU up into the pipe above while surrounded by water.
public class AqueductIntakeBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final float OUTPUT_PRESSURE = 100.0F;

    public AqueductIntakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AQUEDUCT_INTAKE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AqueductIntakeBlockEntity intake) {
        boolean submerged = isSubmerged(level, pos, state);

        // only update the blockstate on a real change to avoid unnecessary chunk dirty marks
        if (state.getValue(AqueductIntakeBlock.ACTIVE) != submerged) {
            level.setBlock(pos, state.setValue(AqueductIntakeBlock.ACTIVE, submerged), Block.UPDATE_ALL);
        }

        if (!submerged) {
            return;
        }

        // output is top-face only; respect the pipe's own connection check before flooding
        BlockPos outputPos = pos.above();
        if (level.getBlockEntity(outputPos) instanceof HardenedIronPipeBlockEntity pipe
                && !pipe.isFlowBlocked() && pipe.connectsOnSide(Direction.DOWN)) {
            HardenedIronPipeBlockEntity.floodFromSource(level, outputPos, OUTPUT_PRESSURE);
        }
    }

    // submerged = waterlogged, or all five non-top neighbours are water
    private static boolean isSubmerged(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(AqueductIntakeBlock.WATERLOGGED)) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) {
                continue; // top face is the output, not part of the submersion check
            }
            if (!level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean active = getBlockState().hasProperty(AqueductIntakeBlock.ACTIVE)
                && getBlockState().getValue(AqueductIntakeBlock.ACTIVE);

        lang().translate("gui.goggles.aqueduct_intake").forGoggles(tooltip);

        if (active) {
            lang().add(dot(ChatFormatting.GREEN))
                    .add(lang().translate("gui.aqueduct_intake.active").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
            lang().add(lang().translate("gui.aqueduct_intake.output").style(ChatFormatting.GRAY))
                    .space()
                    .add(lang().text(LangNumberFormat.format(OUTPUT_PRESSURE)).style(ChatFormatting.GOLD))
                    .add(lang().text(ChatFormatting.DARK_GRAY, " PU/s"))
                    .forGoggles(tooltip, 1);
        } else {
            lang().add(dot(ChatFormatting.RED))
                    .add(lang().translate("gui.aqueduct_intake.inactive").style(ChatFormatting.GRAY))
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    private static LangBuilder dot(ChatFormatting color) {
        return lang().text(color, "■ ");
    }

    private static LangBuilder lang() {
        return new LangBuilder(CreateHydraulics.MODID);
    }
}
