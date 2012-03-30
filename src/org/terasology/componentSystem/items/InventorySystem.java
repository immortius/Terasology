package org.terasology.componentSystem.items;

import org.terasology.components.InventoryComponent;
import org.terasology.components.ItemComponent;
import org.terasology.entitySystem.ComponentSystem;
import org.terasology.entitySystem.EntityRef;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class InventorySystem implements ComponentSystem {

    public static final byte MAX_STACK = (byte)99;

    public void initialise() {

    }

    /**
     * Adds an item to an inventory. If the item stacks it may be destroyed or partially moved (stack count diminished).
     * @param inventoryEntity
     * @param itemEntity
     * @return Whether the item was successfully added to the container in full
     */
    public boolean addItem(EntityRef inventoryEntity, EntityRef itemEntity) {
        InventoryComponent inventory = inventoryEntity.getComponent(InventoryComponent.class);
        ItemComponent item = itemEntity.getComponent(ItemComponent.class);
        if (inventory == null || item == null)
            return false;
        
        int intitalCount = item.stackCount;

        // First check for existing stacks
        for (EntityRef itemStack : inventory.itemSlots) {
            if (itemStack != null) {
                ItemComponent stackComp = itemStack.getComponent(ItemComponent.class);
                if (item.stackId.equals(stackComp.stackId)) {
                    int stackSpace = MAX_STACK - stackComp.stackCount;
                    int amountToTransfer = Math.min(stackSpace, item.stackCount);
                    stackComp.stackCount += amountToTransfer;
                    item.stackCount -= amountToTransfer;
                    itemStack.saveComponent(stackComp);

                    if (item.stackCount == 0) {
                        itemEntity.destroy();
                        inventoryEntity.saveComponent(inventory);
                        return true;
                    }
                }
            }
        }

        // Then free spaces
        int freeSlot = inventory.itemSlots.indexOf(null);
        if (freeSlot != -1) {
            inventory.itemSlots.set(freeSlot, itemEntity);
            item.container = inventoryEntity;
            itemEntity.saveComponent(item);
            inventoryEntity.saveComponent(inventory);
            return true;
        }
        if (intitalCount != item.stackCount) {
            itemEntity.saveComponent(item);
            inventoryEntity.saveComponent(inventory);
        }
        return false;
    }
}
