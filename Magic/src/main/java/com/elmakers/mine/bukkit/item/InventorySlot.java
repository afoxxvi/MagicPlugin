package com.elmakers.mine.bukkit.item;

import javax.annotation.Nullable;

import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.utility.CompatibilityUtils;

public enum InventorySlot {
    HELMET(39), CHESTPLATE(38), LEGGINGS(37), BOOTS(36),

    MAIN_HAND(), OFF_HAND(), FREE(),

    // This is here for armor stands
    RIGHT_ARM();

    private final int slot;

    InventorySlot() {
        this(-1);
    }

    InventorySlot(int slot) {
        this.slot = slot;
    }

    public static InventorySlot getArmorSlot(int slot) {
        slot = Math.max(Math.min(slot, 3), 0);
        switch (slot) {
            case 0: return BOOTS;
            case 1: return LEGGINGS;
            case 2: return CHESTPLATE;
            case 3: return HELMET;
        }
        return HELMET;
    }

    public int getArmorSlot() {
        return slot - 36;
    }

    public boolean isArmorSlot() {
        return slot != -1;
    }

    public int getSlot() {
        return slot;
    }

    public int getSlot(Mage mage) {
        if (slot != -1 || !mage.isPlayer()) {
            return slot;
        }
        switch (this) {
            case MAIN_HAND:
                return mage.getPlayer().getInventory().getHeldItemSlot();
            case OFF_HAND:
                return 40;
            case FREE:
                Inventory inventory = mage.getInventory();
                for (int i = 0; i < inventory.getSize(); i++) {
                    if (CompatibilityUtils.isEmpty(inventory.getItem(i))) {
                        return i;
                    }
                }
            break;
            default:
                return -1;
        }
        return -1;
    }

    public boolean setItem(EntityEquipment equitment, ItemStack itemStack) {
        switch (this) {
            case HELMET:
                equitment.setHelmet(itemStack);
                break;
            case CHESTPLATE:
                equitment.setChestplate(itemStack);
                break;
            case LEGGINGS:
                equitment.setLeggings(itemStack);
                break;
            case BOOTS:
                equitment.setBoots(itemStack);
                break;
            case MAIN_HAND:
                equitment.setItemInMainHand(itemStack);
                break;
            case OFF_HAND:
                equitment.setItemInOffHand(itemStack);
                break;
            case RIGHT_ARM:
                equitment.setItemInOffHand(itemStack);
                break;
            default: return false;
        }
        return true;
    }

    @Nullable
    public ItemStack getItem(EntityEquipment equipment) {
        switch (this) {
            case HELMET:
                return equipment.getHelmet();
            case CHESTPLATE:
                return equipment.getChestplate();
            case LEGGINGS:
                return equipment.getLeggings();
            case BOOTS:
                return equipment.getBoots();
            case MAIN_HAND:
                return equipment.getItemInMainHand();
            case OFF_HAND:
            case RIGHT_ARM:
                return equipment.getItemInOffHand();
            default: return null;
        }
    }
}
