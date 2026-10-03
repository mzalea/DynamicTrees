package com.dtteam.dynamictrees.model.modeldata;

import com.dtteam.dynamictrees.api.network.Connections;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.tree.family.Family;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Extension of {@link Connections} for storing and transferring model data to baked models.
 */
public class ModelConnections extends Connections {

    public static final ModelProperty<ModelConnections> CONNECTIONS_PROPERTY = new ModelProperty<>();

    private Direction ringOnly = null;
    private Family family = Family.NULL_FAMILY;
    private int soilDepth = -1;
    private int posHash = 0;

    public ModelConnections() {}

    public ModelConnections(Connections connections) {
        this.setAllRadii(connections.getAllRadii());
    }

    public ModelConnections(int[] radii) {
        super(radii);
    }

    public ModelConnections(Direction ringDir) {
        ringOnly = ringDir;
    }

    public ModelConnections setAllRadii(int[] radii) {
        return (ModelConnections) super.setAllRadii(radii);
    }

    public ModelConnections setFamily(Family family) {
        this.family = family;
        return this;
    }

    public ModelConnections setFamily(@Nullable BranchBlock branch) {
        if (branch != null) {
            this.family = branch.getFamily();
        }
        return this;
    }

    public Family getFamily() {
        return family;
    }

    /** Branch blocks between this one and the rooty soil (0 = sitting on it), or -1 when not known or further. */
    public int getSoilDepth() {
        return soilDepth;
    }

    /** A stable hash of the block position, for render-only variety. */
    public int getPosHash() {
        return posHash;
    }

    public ModelConnections setPlacement(int soilDepth, int posHash) {
        this.soilDepth = soilDepth;
        this.posHash = posHash;
        return this;
    }

    public Direction getRingOnly() {
        return ringOnly;
    }

    public void setForceRing(Direction ringSide) {
        ringOnly = ringSide;
    }

    public ModelData toModelData() {
        return ModelData.builder().with(CONNECTIONS_PROPERTY, this).build();
    }

    public ModelData toModelData(ModelData baseData) {
        return baseData.derive().with(CONNECTIONS_PROPERTY, this).build();
    }
}