package mekanism.common.lib.inventory;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import mekanism.common.Mekanism;
import mekanism.common.util.InventoryUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

//TODO: Do we want to rename this to HandlerTransitRequest?
public class TileTransitRequest extends TransitRequest {

    private final IItemHandler handler;
    private final Map<HashedItem, TileItemData> itemMap = new LinkedHashMap<>();

    public TileTransitRequest(IItemHandler handler) {
        this.handler = handler;
    }

    public void addItem(ItemStack stack, int slot) {
        HashedItem hashed = HashedItem.create(stack);
        itemMap.computeIfAbsent(hashed, TileItemData::new).addSlot(slot, stack);
    }

    public int getCount(HashedItem itemType) {
        ItemData data = itemMap.get(itemType);
        return data == null ? 0 : data.getTotalCount();
    }

    protected IItemHandler getHandler() {
        return handler;
    }

    public Map<HashedItem, TileItemData> getItemMap() {
        return itemMap;
    }

    @Override
    public Collection<TileItemData> getItemData() {
        return itemMap.values();
    }

    public class TileItemData extends ItemData {

        private final Int2IntMap slotMap = new Int2IntOpenHashMap();

        public TileItemData(HashedItem itemType) {
            super(itemType);
        }

        public void addSlot(int id, ItemStack stack) {
            slotMap.put(id, stack.getCount());
            totalCount += stack.getCount();
        }

        @Override
        public ItemStack use(int amount) {
            IItemHandler handler = getHandler();
            if (handler != null && !slotMap.isEmpty()) {
                HashedItem itemType = getItemType();
                ItemStack itemStack = itemType.getInternalStack();
                ObjectIterator<Int2IntMap.Entry> iterator = slotMap.int2IntEntrySet().iterator();
                while (iterator.hasNext()) {
                    Int2IntMap.Entry entry = iterator.next();
                    int slot = entry.getIntKey();
                    int currentCount = entry.getIntValue();
                    int toUse = Math.min(amount, currentCount);
                    ItemStack ret = handler.extractItem(slot, toUse, false);
                    boolean stackable = InventoryUtils.areItemsStackable(itemStack, ret);
                    if (!stackable || ret.getCount() != toUse) { // be loud if an InvStack's prediction doesn't line up
                        Mekanism.logger.warn("An inventory's returned content {} does not line up with TileTransitRequest's prediction.", stackable ? "count" : "type");
                        Mekanism.logger.warn("TileTransitRequest item: {}, toUse: {}, ret: {}, slot: {}", itemStack, toUse, ret, slot);
                        //TODO: Do we want to keep track of the position and side so we can log it here like we used to?
                        Mekanism.logger.warn("ItemHandler: {}", handler.getClass().getName());
                    }
                    amount -= toUse;
                    totalCount -= toUse;
                    if (totalCount == 0) {
                        itemMap.remove(itemType);
                    }
                    currentCount = currentCount - toUse;
                    if (currentCount == 0) {
                        //If we removed all items from this slot, remove the slot
                        iterator.remove();
                    } else {
                        // otherwise, update the amount in it
                        entry.setValue(currentCount);
                    }
                    if (amount == 0) {
                        break;
                    }
                }
            }
            return getStack();
        }
    }
}