package mekanism.common.lib.inventory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import mekanism.api.text.EnumColor;
import mekanism.common.Mekanism;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.content.transporter.TransporterManager;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.util.StackUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TransitRequest {

    private final TransitResponse EMPTY = new TransitResponse(ItemStack.EMPTY, null);

    public static SimpleTransitRequest simple(ItemStack stack) {
        return new SimpleTransitRequest(stack);
    }

    public static TransitRequest anyItem(IItemHandler inventory, int amount) {
        return definedItem(inventory, amount, Finder.ANY);
    }

    public static TransitRequest definedItem(IItemHandler inventory, int amount, Finder finder) {
        return definedItem(inventory, 1, amount, finder);
    }

    public static TransitRequest definedItem(IItemHandler inventory, int min, int max, Finder finder) {
        HandlerTransitRequest ret = new HandlerTransitRequest(inventory);
        if (inventory == null) {
            return ret;
        }
        // count backwards- we start from the bottom of the inventory and go back for consistency
        for (int i = inventory.getSlots() - 1; i >= 0; i--) {
            ItemStack stack = inventory.extractItem(i, max, true);

            if (!stack.isEmpty() && finder.modifies(stack)) {
                HashedItem hashed = HashedItem.raw(stack);
                int toUse = Math.min(stack.getCount(), max - ret.getCount(hashed));
                if (toUse == 0) {
                    continue; // continue if we don't need any more of this item type
                }
                ret.addItem(StackUtils.size(stack, toUse), i);
            }
        }
        // remove items that we don't have enough of
        ret.getItemMap().entrySet().removeIf(entry -> entry.getValue().getTotalCount() < min);
        return ret;
    }

    public abstract Collection<? extends ItemData> getItemData();

    @NotNull
    public TransitResponse eject(BlockEntity outputter, BlockPos targetPos, @Nullable BlockEntity target, Direction side, int min,
          Function<TileEntityLogisticalTransporterBase, EnumColor> outputColor) {
        if (isEmpty()) {//Short circuit if our request is empty
            return getEmptyResponse();
        }
        if (target instanceof TileEntityLogisticalTransporterBase transporter) {
            return transporter.getTransmitter().insert(outputter, this, outputColor.apply(transporter), true, min);
        }
        return addToInventory(outputter.getLevel(), targetPos, target, side, min);
    }

    @NotNull
    public TransitResponse addToInventory(Level level, BlockPos pos, @Nullable IItemHandler inventory, int min, boolean force) {
        if (isEmpty()) {//Short circuit if our request is empty
            return getEmptyResponse();
        } else if (force && WorldUtils.getTileEntity(level, pos) instanceof TileEntityLogisticalSorter sorter) {
            return sorter.sendHome(this);
        }
        return addToInventory(inventory, min);
    }

    @NotNull
    public TransitResponse addToInventory(Level level, BlockPos pos, @Nullable BlockEntity tile, Direction side, int min) {
        if (isEmpty()) {//Short circuit if our request is empty
            return getEmptyResponse();
        }
        IItemHandler inventory = Capabilities.ITEM.getCapabilityIfLoaded(level, pos, null, tile, side.getOpposite());
        return addToInventory(inventory, min);
    }

    @NotNull
    private TransitResponse addToInventory(@Nullable IItemHandler inventory, int min) {
        if (inventory == null) {
            return getEmptyResponse();
        }
        int slots = inventory.getSlots();
        if (slots == 0) {
            //If the inventory has no slots just exit early with the result that we can't send any items
            return getEmptyResponse();
        }
        if (min > 1) {
            //If we have a minimum amount of items we are trying to send, we need to start by simulating
            // to see if we actually have enough room to send the minimum amount of our item. We can
            // skip this step if we don't have a minimum amount being sent, as then whatever we are
            // able to insert will be "good enough"
            TransitResponse response = TransporterManager.getPredictedInsert(inventory, this);
            if (response.isEmpty() || response.getSendingAmount() < min) {
                //If we aren't able to send any items or are only able to send less than we have room for
                // return that we aren't able to insert the requested amount
                return getEmptyResponse();
            }
            // otherwise, continue on to actually sending items to the inventory
        }
        for (ItemData data : getItemData()) {
            ItemStack origInsert = StackUtils.size(data.getStack(), data.getTotalCount());
            ItemStack toInsert = origInsert.copy();
            for (int i = 0; i < slots; i++) {
                // Do insert, this will handle validating the item is valid for the inventory
                toInsert = inventory.insertItem(i, toInsert, false);
                // If empty, end
                if (toInsert.isEmpty()) {
                    return createResponse(origInsert, data);
                }
            }
            if (TransporterManager.didEmit(origInsert, toInsert)) {
                return createResponse(TransporterManager.getToUse(origInsert, toInsert), data);
            }
        }
        return getEmptyResponse();
    }

    public boolean isEmpty() {
        return getItemData().isEmpty();
    }

    @NotNull
    public TransitResponse createResponse(ItemStack inserted, ItemData data) {
        return new TransitResponse(inserted, data);
    }

    public TransitResponse createSimpleResponse() {
        ItemData data = getItemData().stream().findFirst().orElse(null);
        return data == null ? getEmptyResponse() : createResponse(data.itemType.createStack(data.totalCount), data);
    }

    @NotNull
    public TransitResponse getEmptyResponse() {
        return EMPTY;
    }

    public static class TransitResponse {

        private final ItemStack inserted;
        private final ItemData slotData;

        public TransitResponse(@NotNull ItemStack inserted, ItemData slotData) {
            this.inserted = inserted;
            this.slotData = slotData;
        }

        public int getSendingAmount() {
            return inserted.getCount();
        }

        public ItemData getSlotData() {
            return slotData;
        }

        public ItemStack getStack() {
            return inserted;
        }

        public boolean isEmpty() {
            return inserted.isEmpty() || slotData.getTotalCount() == 0;
        }

        public ItemStack getRejected() {
            if (isEmpty()) {
                return ItemStack.EMPTY;
            }
            return slotData.getItemType().createStack(slotData.getTotalCount() - getSendingAmount());
        }

        public ItemStack use(int amount) {
            return slotData.use(amount);
        }

        public ItemStack useAll() {
            return use(getSendingAmount());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TransitResponse other = (TransitResponse) o;
            return (inserted == other.inserted || ItemStack.matches(inserted, other.inserted)) && slotData.equals(other.slotData);
        }

        @Override
        public int hashCode() {
            int code = 1;
            code = 31 * code + inserted.getItem().hashCode();
            code = 31 * code + inserted.getCount();
            if (inserted.hasTag()) {
                code = 31 * code + inserted.getTag().hashCode();
            }
            code = 31 * code + slotData.hashCode();
            return code;
        }
    }

    public static class ItemData {

        private final HashedItem itemType;
        protected int totalCount;

        public ItemData(HashedItem itemType) {
            this.itemType = itemType;
        }

        public HashedItem getItemType() {
            return itemType;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public ItemStack getStack() {
            return getItemType().createStack(getTotalCount());
        }

        public ItemStack use(int amount) {
            Mekanism.logger.error("Can't 'use' with this type of TransitResponse: {}", getClass().getName());
            return ItemStack.EMPTY;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ItemData itemData = (ItemData) o;
            return totalCount == itemData.totalCount && itemType.equals(itemData.itemType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemType, totalCount);
        }
    }

    public static class SimpleTransitRequest extends TransitRequest {

        private final List<ItemData> slotData;

        protected SimpleTransitRequest(ItemStack stack) {
            slotData = Collections.singletonList(new SimpleItemData(stack));
        }

        @Override
        public Collection<ItemData> getItemData() {
            return slotData;
        }

        public static class SimpleItemData extends ItemData {

            public SimpleItemData(ItemStack stack) {
                //TODO: Can this use raw to avoid a copy? My intuition says yes as I don't think the item data stays around when the stack can mutate
                // but this definitely needs more thought
                super(HashedItem.create(stack));
                totalCount = stack.getCount();
            }
        }
    }
}
