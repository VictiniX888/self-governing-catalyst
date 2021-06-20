package io.github.victinix888.selfgoverningcatalyst.blockentity

import com.mojang.authlib.GameProfile
import io.github.victinix888.selfgoverningcatalyst.SELF_GOVERNING_CATALYST_BLOCK_ENTITY
import io.github.victinix888.selfgoverningcatalyst.block.SelfGoverningCatalystBlock
import io.github.victinix888.selfgoverningcatalyst.block.SelfGoverningCatalystBlock.Companion.DEFAULT_DIRECTION
import io.github.victinix888.selfgoverningcatalyst.entity.AimDirection
import io.github.victinix888.selfgoverningcatalyst.entity.FakePlayerEntity
import io.github.victinix888.selfgoverningcatalyst.screen.SelfGoverningCatalystScreenHandler
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.FacingBlock
import net.minecraft.block.entity.LootableContainerBlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.network.ServerPlayerInteractionManager
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.Tickable
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.util.*

class SelfGoverningCatalystBlockEntity : LootableContainerBlockEntity(SELF_GOVERNING_CATALYST_BLOCK_ENTITY), Tickable, ExtendedScreenHandlerFactory {

    private var inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private var currentActiveSlot = 0

    private val uuid = UUID.fromString("8e08e7ee-fb54-4aa2-9019-0ab8b7e6d4a9")
    private lateinit var fakePlayer: FakePlayerEntity

    private lateinit var direction: Direction
    var mode = DEFAULT_MODE
    var aimDirection = DEFAULT_AIM
        set(value) {
            field = value
            if (this::fakePlayer.isInitialized) {
                fakePlayer.setAim(value)
            }
        }
    var redstoneMode = DEFAULT_REDSTONE

    companion object {
        const val INVENTORY_SIZE = 9
        private val DEFAULT_MODE =  ClickMode.RIGHT_CLICK
        private val DEFAULT_AIM = AimDirection.STRAIGHT
        private val DEFAULT_REDSTONE = RedstoneMode.IGNORE
    }

    override fun tick() {
        if (!this::fakePlayer.isInitialized) {
            // only create fakePlayer on server side
            world?.isClient?.let {
                if (!it) {
                    direction = world?.getBlockState(pos)?.get(FacingBlock.FACING) ?: DEFAULT_DIRECTION
                    fakePlayer = FakePlayerEntity(
                        world?.server,
                        world as ServerWorld,
                        GameProfile(uuid, "[SGC]"),
                        ServerPlayerInteractionManager(world as ServerWorld),
                        calcFakePlayerPos(direction),
                        direction,
                        aimDirection
                    )
                }
            }
        } else {
            world?.isClient?.let { client ->
                if (!client) {
                    // determine whether the block is receiving a redstone signal
                    val isTriggered by lazy { world?.getBlockState(pos)?.get(SelfGoverningCatalystBlock.TRIGGERED) ?: false }
                    // determine whether the fakeplayer should act on current tick
                    val shouldAct = (redstoneMode == RedstoneMode.IGNORE) || (redstoneMode == RedstoneMode.LOW && !isTriggered) || (redstoneMode == RedstoneMode.HIGH && isTriggered)
                    if (shouldAct) {
                        handleAct()
                    } else if (fakePlayer.isMining) {
                        // cancel mining action if fakeplayer was mining
                        fakePlayer.interruptMining()
                    }
                }
            }
        }
    }
    
    private fun handleAct() {
        val itemToUse = getItemToUse()
        // give fakePlayer item from container
        fakePlayer.setStackInHand(Hand.MAIN_HAND, itemToUse)
        fakePlayer.playerTick()

        val lookingAtHitResult = getEntityLookingAt(fakePlayer)
        
        performAction(itemToUse, lookingAtHitResult)
        
        // ensure that any changes to the itemstack in main hand is reflected in the block's inventory
        // (e.g. bucket left behind from drinking milk)
        val mainHandItemStack = fakePlayer.getStackInHand(Hand.MAIN_HAND)
        if (mainHandItemStack != inventory[currentActiveSlot]) {
            inventory[currentActiveSlot] = mainHandItemStack
        }
    }

    private fun performAction(itemToUse: ItemStack, lookingAtHitResult: HitResult) {
        if (mode == ClickMode.RIGHT_CLICK) {
            // cancel mining action if fakePlayer was mining on previous tick
            if (fakePlayer.isMining) {
                fakePlayer.interruptMining()
            }

            if (lookingAtHitResult.type == HitResult.Type.ENTITY) {
                lookingAtHitResult as EntityHitResult
                //obtain entity that the fakeplayer is looking at
                val lookingAtEntity = lookingAtHitResult.entity

                if (lookingAtEntity is LivingEntity) {
                    if (!itemToUse.isEmpty) {
                        // try to use item on entity
                        itemToUse.useOnEntity(fakePlayer, lookingAtEntity, Hand.MAIN_HAND).let { result ->
                            if (result.isAccepted) return
                        }
                    }
                }

                // try to interact with entity
                lookingAtEntity.interact(fakePlayer, Hand.MAIN_HAND).let { result ->
                    if (result.isAccepted) return
                }
                lookingAtEntity.interactAt(fakePlayer, lookingAtHitResult.pos, Hand.MAIN_HAND).let { result ->
                    if (result.isAccepted) return
                }

            } else if (lookingAtHitResult.type == HitResult.Type.BLOCK) {
                lookingAtHitResult as BlockHitResult
                // obtain block that the fakeplayer is looking at
                val lookingAtBlockState = world?.getBlockState(lookingAtHitResult.blockPos)

                if (lookingAtBlockState?.isAir == false) {
                    if (!itemToUse.isEmpty) {
                        // try to use item on block it is looking at
                        itemToUse.useOnBlock(ItemUsageContext(fakePlayer, Hand.MAIN_HAND, lookingAtHitResult)).let { result ->
                            if (result.isAccepted) return
                        }
                    }

                    // try to use block normally
                    lookingAtBlockState.onUse(world, fakePlayer, Hand.MAIN_HAND, lookingAtHitResult).let { result ->
                        if (result.isAccepted) return
                    }
                }
            }

            if (!itemToUse.isEmpty) {
                // try to use item normally
                itemToUse.use(world, fakePlayer, Hand.MAIN_HAND).let { typedResult ->
                    if (typedResult.result.isAccepted) {
                        fakePlayer.setStackInHand(Hand.MAIN_HAND, typedResult.value)
                        return
                    }
                }
            }
        } else {
            if (lookingAtHitResult.type == HitResult.Type.ENTITY) {
                // cancel mining action if fakePlayer was mining on previous tick
                if (fakePlayer.isMining) {
                    fakePlayer.interruptMining()
                }

                lookingAtHitResult as EntityHitResult
                //obtain entity that the fakeplayer is looking at
                val lookingAtEntity = lookingAtHitResult.entity

                fakePlayer.attack(lookingAtEntity)
            } else if (lookingAtHitResult.type == HitResult.Type.BLOCK) {
                lookingAtHitResult as BlockHitResult
                // obtain block that the fakeplayer is looking at
                val lookingAtBlockState = world?.getBlockState(lookingAtHitResult.blockPos)

                // carry out mining action
                if (lookingAtBlockState?.isAir == false) {
                    fakePlayer.tickMining(lookingAtBlockState, lookingAtHitResult.blockPos)
                }
            }
        }
    }

    private fun getItemToUse(): ItemStack {
        if (inventory[currentActiveSlot].isEmpty) {
            (inventory.indexOfFirst { !it.isEmpty }).let {
                currentActiveSlot = if (it >= 0) it else 0
            }
        }

        return inventory[currentActiveSlot]
    }

    private fun getEntityLookingAt(entity: LivingEntity): HitResult {
        val entityPos = entity.pos.add(0.0, entity.standingEyeHeight.toDouble(), 0.0)
        val blockHitResult = entity.raycast(3.0, 1.0F, true)

        return world?.getOtherEntities(null, Box(entityPos, blockHitResult.pos))?.minBy { it.pos.distanceTo(entityPos) }?.let {
            EntityHitResult(it)
        } ?: blockHitResult
    }

    private fun calcFakePlayerPos(direction: Direction): Vec3d {
        return when (direction) {
            Direction.SOUTH -> Vec3d(pos.x + 0.5, pos.y - 1.12, pos.z + 1.0)
            Direction.NORTH -> Vec3d(pos.x + 0.5, pos.y - 1.12, pos.z.toDouble())
            Direction.EAST -> Vec3d(pos.x + 1.0, pos.y - 1.12, pos.z + 0.5)
            Direction.WEST -> Vec3d(pos.x.toDouble(), pos.y - 1.12, pos.z + 0.5)
            Direction.UP -> Vec3d(pos.x + 0.5, pos.y - 0.62, pos.z + 0.5)
            Direction.DOWN -> Vec3d(pos.x + 0.5, pos.y - 1.62, pos.z + 0.5)
        }
    }

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?): ScreenHandler {
        return SelfGoverningCatalystScreenHandler(syncId, playerInventory, pos, mode, aimDirection, redstoneMode, this)
    }

    override fun setInvStackList(list: DefaultedList<ItemStack>?) {
        inventory = list
    }

    override fun getContainerName(): Text {
        return TranslatableText("container.self_governing_catalyst")
    }

    override fun getInvStackList(): DefaultedList<ItemStack> {
        return inventory
    }

    override fun size(): Int {
        return inventory.size
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        Inventories.toTag(tag, inventory)
        tag?.putInt("mode", mode.ordinal)
        tag?.putInt("aim", aimDirection.ordinal)
        tag?.putInt("redstone", redstoneMode.ordinal)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        super.fromTag(state, tag)
        Inventories.fromTag(tag, inventory)
        mode = ClickMode.values()[tag?.getInt("mode") ?: DEFAULT_MODE.ordinal]
        aimDirection = AimDirection.values()[tag?.getInt("aim") ?: DEFAULT_AIM.ordinal]
        redstoneMode = RedstoneMode.values()[tag?.getInt("redstone") ?: DEFAULT_REDSTONE.ordinal]
    }

    override fun toUpdatePacket(): BlockEntityUpdateS2CPacket? {
        return BlockEntityUpdateS2CPacket(pos, -1, toInitialChunkDataTag())
    }

    override fun toInitialChunkDataTag(): CompoundTag {
        return toTag(CompoundTag())
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeInt(mode.ordinal)
        buf.writeInt(aimDirection.ordinal)
        buf.writeInt(redstoneMode.ordinal)
    }
}