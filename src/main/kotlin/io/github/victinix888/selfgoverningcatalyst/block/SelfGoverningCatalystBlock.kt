package io.github.victinix888.selfgoverningcatalyst.block

import io.github.victinix888.selfgoverningcatalyst.blockentity.SelfGoverningCatalystBlockEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World

class SelfGoverningCatalystBlock(blockSettings: FabricBlockSettings) : FacingBlock(blockSettings), BlockEntityProvider {

    companion object {
        val DEFAULT_DIRECTION = Direction.NORTH
    }

    init {
        defaultState = stateManager.defaultState.with(FACING, DEFAULT_DIRECTION)
    }

    private lateinit var direction: Direction

    override fun createBlockEntity(world: BlockView?): BlockEntity? {

        return SelfGoverningCatalystBlockEntity()
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>?) {
        builder?.add(FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext?): BlockState? {
        direction = ctx?.playerLookDirection?.opposite ?: DEFAULT_DIRECTION
        return defaultState.with(FACING, direction)
    }

    override fun createScreenHandlerFactory(state: BlockState?, world: World?, pos: BlockPos?): ExtendedScreenHandlerFactory? {
        return world?.getBlockEntity(pos) as? ExtendedScreenHandlerFactory
    }

    @Suppress("deprecation")
    override fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        if (world != null && player != null) {
            if (!world.isClient) {
                if (world.getBlockEntity(pos) is SelfGoverningCatalystBlockEntity) {
                    val factory = createScreenHandlerFactory(state, world, pos)
                    player.openHandledScreen(factory)
                }
            }
            return ActionResult.SUCCESS
        } else {
            return ActionResult.CONSUME
        }
    }

    override fun onStateReplaced(state: BlockState?, world: World?, pos: BlockPos?, newState: BlockState?, moved: Boolean) {
        if (state?.block != newState?.block) {
            val blockEntity = world?.getBlockEntity(pos)
            if (blockEntity is SelfGoverningCatalystBlockEntity) {
                ItemScatterer.spawn(world, pos, blockEntity)
            }

            super.onStateReplaced(state, world, pos, newState, moved)

        }
    }
}