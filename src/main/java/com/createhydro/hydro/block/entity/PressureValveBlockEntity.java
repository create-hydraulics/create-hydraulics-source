package com.createhydro.hydro.block.entity;

import com.createhydro.hydro.block.PressureValveBlock;
import com.createhydro.hydro.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Pressure Valve.
 *
 * <p>Inherits all pressure logic from {@link HardenedIronPipeBlockEntity}. The only addition is
 * {@link #isFlowBlocked()}: when the valve is powered by a redstone signal it becomes an impassable wall
 * &ndash; pressure does not cross it in either direction, so a side with no input of its own stays empty.
 * (Powered &rarr; closed: pressure arriving on the east face cannot reach the west side.)</p>
 */
public class PressureValveBlockEntity extends HardenedIronPipeBlockEntity {

    public PressureValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRESSURE_VALVE.get(), pos, state);
    }

    // ------------------------------------------------------------------------------------------------
    // Valve gate
    // ------------------------------------------------------------------------------------------------

    /**
     * Returns {@code true} when the valve is powered (redstone signal present) and therefore sealing off
     * all flow. The parent's server tick respects this for pressure in both directions.
     */
    @Override
    public boolean isFlowBlocked() {
        BlockState s = getBlockState();
        return s.hasProperty(PressureValveBlock.POWERED) && s.getValue(PressureValveBlock.POWERED);
    }
}
