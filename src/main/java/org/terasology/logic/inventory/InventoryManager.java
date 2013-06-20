/*
 * Copyright 2013 Moving Blocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.logic.inventory;

import org.terasology.entitySystem.EntityRef;

/**
 * @author Immortius
 */
public interface InventoryManager {

    /**
     * Moves all items from one inventory to another, as far as possible
     * @param fromInventory
     * @param toInventory
     */
    void moveAll(EntityRef fromInventory, EntityRef toInventory);

    /**
     * @param inventoryEntity
     * @param item
     * @return Whether the given item can be added to the inventory
     */
    boolean canTakeItem(EntityRef inventoryEntity, EntityRef item);

    /**
     * @param inventoryEntity
     * @param item
     * @return Whether the item was fully consumed in being added to the inventory
     */
    boolean giveItem(EntityRef inventoryEntity, EntityRef item);

    /**
     * Removes an item from the inventory (but doesn't destroy it)
     *
     * @param inventoryEntity
     * @param item
     */
    void removeItem(EntityRef inventoryEntity, EntityRef item);

    /**
     * Removes a number of an item from the inventory. If the count equals or exceeds actual stack size of the item,
     * the entire stack is removed. Otherwise a new item is created of the correct stack size, and the existing item's
     * stack size is decremented to match.
     *
     * @param inventoryEntity
     * @param item
     */
    EntityRef removeItem(EntityRef inventoryEntity, EntityRef item, int stackCount);

    /**
     * Removes an item from the inventory and destroys it
     *
     * @param inventoryEntity
     * @param item
     */
    void destroyItem(EntityRef inventoryEntity, EntityRef item);

    /**
     * @param itemA
     * @param itemB
     * @return Whether the two items can be merged (ignoring stack size limits)
     */
    boolean canStackTogether(EntityRef itemA, EntityRef itemB);

    /**
     * @param item
     * @return The size of the stack of items represented by the given item. 0 if the entity is not an item.
     */
    int getStackSize(EntityRef item);

    /**
     *  This version of setStackSize will destroy the item if newStackSize is <=0
     * @param inventoryEntity
     * @param item
     * @param newStackSize
     */
    void setStackSize(EntityRef inventoryEntity, EntityRef item, int newStackSize);
}
