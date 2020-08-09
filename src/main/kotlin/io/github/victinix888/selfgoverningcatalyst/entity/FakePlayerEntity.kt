package io.github.victinix888.selfgoverningcatalyst.entity

import com.mojang.authlib.GameProfile
import io.github.victinix888.selfgoverningcatalyst.network.FakeNetworkHandler
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.network.ClientConnection
import net.minecraft.network.NetworkSide
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.network.ServerPlayerInteractionManager
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import net.minecraft.world.World
import java.util.*

class FakePlayerEntity(
        server: MinecraftServer?,
        world: ServerWorld?,
        profile: GameProfile,
        interactionManager: ServerPlayerInteractionManager,
        pos: Vec3d,
        private val direction: Direction,
        aim: AimDirection
) : ServerPlayerEntity(server, world, profile, interactionManager) {

    var isMining = false
    private var miningTime = -1
    private lateinit var miningPos: BlockPos
    private var miningBlockState: BlockState? = null
    private var blockBreakProgress = -1

    init {
        setPos(pos.x, pos.y, pos.z)
        when (direction) {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> setRotation(direction.asRotation(), aim.pitch)
            Direction.UP -> setRotation(direction.asRotation(), -90F)
            Direction.DOWN -> setRotation(direction.asRotation(), 90F)
        }
        interactionManager.setGameMode(GameMode.SURVIVAL, GameMode.SURVIVAL)
        networkHandler = FakeNetworkHandler(world?.server, ClientConnection(NetworkSide.SERVERBOUND), this)
    }

    // code mostly taken from ServerPlayerInteractionManager::update
    fun tickMining(blockState: BlockState, blockPos: BlockPos) {
        if (isMining) {
            if (blockState.isAir) {
                resetMining()
            } else {
                if ((miningBlockState != null && blockState != miningBlockState) || blockPos != miningPos) {
                    // cancel and restart mining process if block being mined is changed
                    interruptMining()
                    beginMining(blockState, blockPos)
                } else {
                    miningTime++
                    continueMining(blockState, blockPos)
                }
            }
        } else {
            beginMining(blockState, blockPos)
        }
    }

    fun interruptMining() {
        resetMining()
    }

    private fun resetMining() {
        world.setBlockBreakingInfo(this.entityId, miningPos, -1)
        blockBreakProgress = -1
        miningTime = -1
        isMining = false
        miningBlockState = null
    }

    private fun finishMining(blockPos: BlockPos) {
        interactionManager.tryBreakBlock(blockPos)
        resetMining()
    }

    // code mostly taken from ServerPlayerInteractionManager::continueMining
    private fun continueMining(blockState: BlockState, blockPos: BlockPos) {
        val currentBreakProgress = blockState.calcBlockBreakingDelta(this, world, blockPos) * (miningTime + 1)
        if (currentBreakProgress >= 1F) {
            finishMining(blockPos)
        } else {
            val currentBreakProgressInt = (currentBreakProgress * 10F).toInt()
            if (currentBreakProgressInt != blockBreakProgress) {
                world.setBlockBreakingInfo(this.entityId, blockPos, currentBreakProgressInt)
                blockBreakProgress = currentBreakProgressInt
            }
        }
    }

    // code mostly taken from ServerPlayerInteractionManager::processBlockBreakingAction
    private fun beginMining(blockState: BlockState, blockPos: BlockPos) {
        if (!isMining) {
            if (!blockState.isAir) {
                blockState.onBlockBreakStart(world, blockPos, this)
                val currentBreakProgress = blockState.calcBlockBreakingDelta(this, world, blockPos)

                if (currentBreakProgress >= 1F) {
                    finishMining(blockPos)
                } else {
                    isMining = true
                    miningPos = blockPos.toImmutable()
                    miningBlockState = blockState
                    val currentBreakProgressInt = (currentBreakProgress * 10F).toInt()
                    world.setBlockBreakingInfo(this.entityId, blockPos, currentBreakProgressInt)
                    blockBreakProgress = currentBreakProgressInt
                }
            }
        }
    }

    fun setAim(aimDirection: AimDirection) {
        pitch = when (direction) {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST -> aimDirection.pitch
            Direction.UP -> -90F
            Direction.DOWN -> 90F
        }
    }

    //override fun playerTick() {}
    //override fun tick() {}       don't override since attribute modifiers due to equipped tools/weapons is applied here
    override fun eatFood(world: World?, stack: ItemStack?): ItemStack { return stack ?: ItemStack.EMPTY }
    override fun playSound(sound: SoundEvent?, volume: Float, pitch: Float) {}
    override fun playSound(event: SoundEvent?, category: SoundCategory?, volume: Float, pitch: Float) {}
    override fun openHandledScreen(factory: NamedScreenHandlerFactory?): OptionalInt { return OptionalInt.empty() }
    override fun openHandledScreen(handler: ScreenHandler?) {}
}