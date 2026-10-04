package com.dtteam.dynamictrees.systems.growthlogic;

import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import com.dtteam.dynamictrees.utility.CoordUtils;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * New Haven broadleaf growth: a few long limbs instead of a ball. Once a branch has turned out of the trunk it keeps
 * going the way it is heading ({@code limb_reach}) and bends upward as it goes ({@code arch}), and never grows down;
 * each tree leans its crown toward one side picked from its root position ({@code lean}), so neighbours differ.
 */
public class SpreadingLogic extends GrowthLogicKit {

    public static final ConfigurationProperty<Integer> LIMB_REACH = ConfigurationProperty.integer("limb_reach");
    public static final ConfigurationProperty<Integer> ARCH = ConfigurationProperty.integer("arch");
    public static final ConfigurationProperty<Integer> LEAN = ConfigurationProperty.integer("lean");

    public SpreadingLogic(final ResourceLocation registryName) {
        super(registryName);
    }

    @Override
    protected GrowthLogicKitConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration()
                .with(LIMB_REACH, 3)
                .with(ARCH, 1)
                .with(LEAN, 2);
    }

    @Override
    protected void registerProperties() {
        this.register(LIMB_REACH, ARCH, LEAN);
    }

    @Override
    public int[] populateDirectionProbabilityMap(GrowthLogicKitConfiguration configuration, DirectionManipulationContext context) {
        final int[] probMap = super.populateDirectionProbabilityMap(configuration, context);
        final Direction lean = CoordUtils.HORIZONTALS[Math.floorMod(CoordUtils.coordHashCode(context.signal().rootPos, 2), 4)];
        final Direction dir = context.signal().dir;

        if (context.signal().isInTrunk()) {
            // More limbs leave the trunk on the leaning side.
            probMap[lean.ordinal()] += configuration.get(LEAN);
        } else {
            probMap[Direction.DOWN.ordinal()] = 0;
            if (dir.getAxis().isHorizontal()) {
                probMap[dir.ordinal()] += configuration.get(LIMB_REACH);
                // Limbs start level and bend up the further they reach.
                int out = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());
                if (out >= 2) probMap[Direction.UP.ordinal()] += configuration.get(ARCH);
            }
            probMap[lean.ordinal()] += 1;
        }
        return probMap;
    }
}
