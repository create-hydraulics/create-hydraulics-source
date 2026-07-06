package com.createhydro.hydro.block.entity;

import java.util.List;

import com.createhydro.hydro.CreateHydraulics;
import com.createhydro.hydro.block.FlowGaugeBlock;
import com.createhydro.hydro.registry.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

// pipe segment that also reads its pressure and maps it to a needle angle for the dial.
public class FlowGaugeBlockEntity extends HardenedIronPipeBlockEntity implements IHaveGoggleInformation {

    public static final float NEEDLE_MIN_DEGREES = -90.0F;
    // stops just shy of vertical (-10 instead of +90) so the needle stays inside the dial at full pressure
    public static final float NEEDLE_MAX_DEGREES = -10.0F;

    public FlowGaugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_GAUGE.get(), pos, state);
    }

    // gauge connects only along its pass-through axis; the dial face and perpendicular sides are sealed
    @Override
    public boolean connectsOnSide(Direction dir) {
        BlockState s = getBlockState();
        return s.hasProperty(FlowGaugeBlock.FACING)
                && dir.getAxis() == s.getValue(FlowGaugeBlock.FACING).getAxis();
    }

    public float getPressureFraction() {
        return Mth.clamp(getPressure() / MAX_PRESSURE, 0.0F, 1.0F);
    }

    public float getNeedleAngleDegrees() {
        return Mth.lerp(getPressureFraction(), NEEDLE_MIN_DEGREES, NEEDLE_MAX_DEGREES);
    }

    // client-only; eased so pressure sync steps show as a smooth sweep rather than a snap
    private float renderAngle = Float.NaN;

    public float getRenderNeedleAngle() {
        float target = getNeedleAngleDegrees();
        if (Float.isNaN(renderAngle)) {
            renderAngle = target;
        } else {
            renderAngle += (target - renderAngle) * 0.2F;
            if (Math.abs(target - renderAngle) < 0.05F) {
                renderAngle = target;
            }
        }
        return renderAngle;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        float pressure = getPressure();
        float fraction = getPressureFraction();
        ChatFormatting valueColor = colorFor(fraction);

        lang().translate("gui.goggles.flow_gauge").forGoggles(tooltip);

        lang().add(lang().translate("gui.flow_gauge.pressure").style(ChatFormatting.GRAY))
                .space()
                .add(lang().text(LangNumberFormat.format(pressure)).style(valueColor))
                .add(lang().text(ChatFormatting.DARK_GRAY, " / " + LangNumberFormat.format(MAX_PRESSURE) + " PU"))
                .forGoggles(tooltip, 1);
        return true;
    }

    private static ChatFormatting colorFor(float fraction) {
        if (fraction <= 0.0F) {
            return ChatFormatting.GRAY;
        }
        if (fraction < 0.34F) {
            return ChatFormatting.AQUA;
        }
        if (fraction < 0.67F) {
            return ChatFormatting.GREEN;
        }
        return ChatFormatting.GOLD;
    }

    private static LangBuilder lang() {
        return new LangBuilder(CreateHydraulics.MODID);
    }
}
