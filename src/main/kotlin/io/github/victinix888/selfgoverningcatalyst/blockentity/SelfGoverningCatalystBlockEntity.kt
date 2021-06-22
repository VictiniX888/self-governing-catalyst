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
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import java.util.*

class SelfGoverningCatalystBlockEntity(pos: BlockPos?, state: BlockState?) : LootableContainerBlockEntity(SELF_GOVERNING_CATALYST_BLOCK_ENTITY, pos, state), ExtendedScreenHandlerFactory {

    private val initInventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    private var currentActiveSlot = 0

    private val uuid = UUID.fromString("8e08e7ee-fb54-4aa2-9019-0ab8b7e6d4a9")
    private lateinit var fakePlayer: FakePlayerEntity
    private var inventory = initInventory

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

        // I put these in the companion object because they are static functions as implemented in the Minecraft code
        fun serverTick(world: World, state: BlockState, blockEntity: SelfGoverningCatalystBlockEntity) {
            if (!blockEntity::fakePlayer.isInitialized) {
                blockEntity.initFakePlayer(world, state)
            }
                
            // determine whether the block is receiving a redstone signal
            val isTriggered by lazy { state.get(SelfGoverningCatalystBlock.TRIGGERED) ?: false }
            // determine whether the fakeplayer should act on current tick
            val shouldAct = (blockEntity.redstoneMode == RedstoneMode.IGNORE) || (blockEntity.redstoneMode == RedstoneMode.LOW && !isTriggered) || (blockEntity.redstoneMode == RedstoneMode.HIGH && isTriggered)
            if (shouldAct) {
                handleAct(blockEntity.fakePlayer, world, blockEntity)
            } else if (blockEntity.fakePlayer.isMining) {
                // cancel mining action if fakeplayer was mining
                blockEntity.fakePlayer.interruptMining()
            }
        }

        private fun handleAct(fakePlayer: FakePlayerEntity, world: World, blockEntity: SelfGoverningCatalystBlockEntity) {
            // give fakePlayer item from container
            val slotToSelect = getItemSlotToSelect(blockEntity.inventory, blockEntity)
            fakePlayer.inventory.selectedSlot = slotToSelect
            fakePlayer.playerTick()

            val lookingAtHitResult = getEntityLookingAt(fakePlayer, world)

            val itemToUse = fakePlayer.mainHandStack
            performAction(itemToUse, lookingAtHitResult, fakePlayer, world, blockEntity.mode)

            // eject any items that do not fit in the block's inventory
            fakePlayer.ejectItemsAfter(INVENTORY_SIZE)
        }

        private fun getItemSlotToSelect(inventory: DefaultedList<ItemStack>, blockEntity: SelfGoverningCatalystBlockEntity): Int {
            if (inventory[blockEntity.currentActiveSlot].isEmpty) {
                (inventory.indexOfFirst { !it.isEmpty }).let {
                    blockEntity.currentActiveSlot = if (it >= 0) it else 0
                }
            }

            return blockEntity.currentActiveSlot
        }

        private fun getEntityLookingAt(entity: LivingEntity, world: World): HitResult {
            val entityPos = entity.pos.add(0.0, entity.standingEyeHeight.toDouble(), 0.0)
            val blockHitResult = entity.raycast(3.0, 1.0F, true)

            return world.getOtherEntities(null, Box(entityPos, blockHitResult.pos))
                .minByOrNull { it.pos.distanceTo(entityPos) }
                ?.let {
                    EntityHitResult(it)
                } ?: blockHitResult
        }

        private fun performAction(itemToUse: ItemStack, lookingAtHitResult: HitResult, fakePlayer: FakePlayerEntity, world: World, mode: ClickMode) {
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
                    val lookingAtBlockState = world.getBlockState(lookingAtHitResult.blockPos)

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
                    val lookingAtBlockState = world.getBlockState(lookingAtHitResult.blockPos)

                    // carry out mining action
                    if (lookingAtBlockState?.isAir == false) {
                        fakePlayer.tickMining(lookingAtBlockState, lookingAtHitResult.blockPos)
                    }
                }
            }
        }
    }
    
    // Initializes the fakeplayer. Should be called on or before the first tick.
    // this.world is null in the init block so this must be done after construction
    private fun initFakePlayer(world: World, state: BlockState) {
        val direction = state.get(FacingBlock.FACING) ?: DEFAULT_DIRECTION
        fakePlayer = FakePlayerEntity(
            world.server,
            world as ServerWorld,
            GameProfile(uuid, "[SGC]"),
            initInventory,
            calcFakePlayerPos(direction),
            direction,
            aimDirection
        )
        inventory = fakePlayer.inventory.main
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
        inventory = list ?: DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    }

    override fun getContainerName(): Text {
        return TranslatableText("container.self_governing_catalyst")
    }

    override fun getInvStackList(): DefaultedList<ItemStack> {
        return inventory
    }

    override fun size(): Int {
        return INVENTORY_SIZE
    }

    override fun writeNbt(nbt: NbtCompound?): NbtCompound {
        Inventories.writeNbt(nbt, inventory)
        nbt?.putInt("mode", mode.ordinal)
        nbt?.putInt("aim", aimDirection.ordinal)
        nbt?.putInt("redstone", redstoneMode.ordinal)
        return super.writeNbt(nbt)
    }

    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, initInventory)
        
        // Initialize settings
        mode = ClickMode.values()[nbt?.getInt("mode") ?: DEFAULT_MODE.ordinal]
        aimDirection = AimDirection.values()[nbt?.getInt("aim") ?: DEFAULT_AIM.ordinal]
        redstoneMode = RedstoneMode.values()[nbt?.getInt("redstone") ?: DEFAULT_REDSTONE.ordinal]
    }

    override fun toUpdatePacket(): BlockEntityUpdateS2CPacket {
        return BlockEntityUpdateS2CPacket(pos, -1, toInitialChunkDataNbt())
    }

    override fun toInitialChunkDataNbt(): NbtCompound {
        return writeNbt(NbtCompound())
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeInt(mode.ordinal)
        buf.writeInt(aimDirection.ordinal)
        buf.writeInt(redstoneMode.ordinal)
    }
}