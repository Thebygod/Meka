package mekanism.common.attachments.containers;

import java.util.List;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.inventory.IMekanismInventory;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class AttachedInventorySlots extends AttachedContainers<IInventorySlot> implements IMekanismInventory {

    public AttachedInventorySlots(List<IInventorySlot> slots) {
        super(slots);
    }

    @Override
    public List<IInventorySlot> getInventorySlots(@Nullable Direction side) {
        return containers;
    }
}