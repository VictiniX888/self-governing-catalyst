package io.github.victinix888.selfgoverningcatalyst.screen

import io.github.victinix888.selfgoverningcatalyst.SELF_GOVERNING_CATALYST_SCREEN_HANDLER
import io.github.victinix888.selfgoverningcatalyst.blockentity.ClickMode
import io.github.victinix888.selfgoverningcatalyst.blockentity.RedstoneMode
import io.github.victinix888.selfgoverningcatalyst.entity.AimDirection
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos

class SelfGoverningCatalystScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory?,
    val blockPos: BlockPos,
    var clickMode: ClickMode,
    var aimDirection: AimDirection,
    var redstoneMode: RedstoneMode,
    private val inventory: Inventory = SimpleInventory(INVENTORY_SIZE)
) : ScreenHandler(SELF_GOVERNING_CATALYST_SCREEN_HANDLER, syncId) {

    companion object {
        const val INVENTORY_SIZE = 9
    }

    init {
        checkSize(inventory, INVENTORY_SIZE)
        inventory.onOpen(playerInventory?.player)

        // set block inventory slots
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                addSlot(Slot(inventory, j + i * 3, 116 + j * 18, 17 + i * 18))
            }
        }

        // set player inventory slots
        for (i in 0 until 3) {
            for (j in 0 until 9) {
                addSlot(Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 92 + i * 18))
            }
        }

        for (i in 0 until 9) {
            addSlot(Slot(playerInventory, i, 8 + i * 18, 150))
        }
    }

    override fun canUse(player: PlayerEntity?): Boolean {
        return inventory.canPlayerUse(player)
    }

    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        var newStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot != null && slot.hasStack()) {
            val originalStack = slot.stack
            newStack = originalStack.copy()
            if (index < INVENTORY_SIZE) {
                if (!insertItem(originalStack, INVENTORY_SIZE, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(originalStack, 0, INVENTORY_SIZE, false)) {
                return ItemStack.EMPTY
            }

            if (originalStack.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }

            if (originalStack.count == newStack.count) {
                return ItemStack.EMPTY
            }

            slot.onTakeItem(player, originalStack)
        }

        return newStack
    }
}