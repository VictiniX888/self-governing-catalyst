package io.github.victinix888.selfgoverningcatalyst.network

import io.github.victinix888.selfgoverningcatalyst.entity.FakePlayerEntity
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.ClientConnection
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.text.Text

class FakeNetworkHandler(server: MinecraftServer?,
                         connection: ClientConnection?,
                         fakePlayer: FakePlayerEntity
) : ServerPlayNetworkHandler(server, connection, fakePlayer) {

    override fun tick() {}

    override fun onCloseHandledScreen(packet: CloseHandledScreenC2SPacket?) {}

    override fun onSignUpdate(packet: UpdateSignC2SPacket?) {}

    override fun onJigsawUpdate(packet: UpdateJigsawC2SPacket?) {}

    override fun onGameMessage(packet: ChatMessageC2SPacket?) {}

    override fun onStructureBlockUpdate(packet: UpdateStructureBlockC2SPacket?) {}

    override fun onKeepAlive(packet: KeepAliveC2SPacket?) {}

    override fun onCustomPayload(packet: CustomPayloadC2SPacket?) {}

    override fun onDisconnected(reason: Text?) {}

    override fun onHandSwing(packet: HandSwingC2SPacket?) {}

    override fun onQueryEntityNbt(packet: QueryEntityNbtC2SPacket?) {}

    override fun onUpdateDifficulty(packet: UpdateDifficultyC2SPacket?) {}

    override fun onPlayerAbilities(packet: UpdatePlayerAbilitiesC2SPacket?) {}

    override fun onClientSettings(packet: ClientSettingsC2SPacket?) {}

    override fun onClickSlot(packet: ClickSlotC2SPacket?) {}

    override fun onRecipeBookData(packet: RecipeBookDataC2SPacket?) {}

    override fun onUpdateSelectedSlot(packet: UpdateSelectedSlotC2SPacket?) {}

    override fun onAdvancementTab(packet: AdvancementTabC2SPacket?) {}

    override fun onBookUpdate(packet: BookUpdateC2SPacket?) {}

    override fun onRequestCommandCompletions(packet: RequestCommandCompletionsC2SPacket?) {}

    override fun onPlayerInteractBlock(packet: PlayerInteractBlockC2SPacket?) {}

    override fun onPlayerAction(packet: PlayerActionC2SPacket?) {}

    override fun onUpdateCommandBlock(packet: UpdateCommandBlockC2SPacket?) {}

    override fun onConfirmScreenAction(packet: ConfirmScreenActionC2SPacket?) {}

    override fun onPickFromInventory(packet: PickFromInventoryC2SPacket?) {}

    override fun onVehicleMove(packet: VehicleMoveC2SPacket?) {}

    override fun onTeleportConfirm(packet: TeleportConfirmC2SPacket?) {}

    override fun onPlayerMove(packet: PlayerMoveC2SPacket?) {}

    override fun onClientCommand(packet: ClientCommandC2SPacket?) {}

    override fun onSpectatorTeleport(packet: SpectatorTeleportC2SPacket?) {}

    override fun onButtonClick(packet: ButtonClickC2SPacket?) {}

    override fun onUpdateCommandBlockMinecart(packet: UpdateCommandBlockMinecartC2SPacket?) {}

    override fun onMerchantTradeSelect(packet: SelectMerchantTradeC2SPacket?) {}

    override fun onUpdateDifficultyLock(packet: UpdateDifficultyLockC2SPacket?) {}

    override fun onCraftRequest(packet: CraftRequestC2SPacket?) {}

    override fun sendPacket(packet: Packet<*>?) {}

    override fun sendPacket(packet: Packet<*>?, listener: GenericFutureListener<out Future<in Void>>?) {}

    override fun onPlayerInput(packet: PlayerInputC2SPacket?) {}

    override fun onClientStatus(packet: ClientStatusC2SPacket?) {}

    override fun onRenameItem(packet: RenameItemC2SPacket?) {}

    override fun onJigsawGenerating(packet: JigsawGeneratingC2SPacket?) {}

    override fun disconnect(reason: Text?) {}

    override fun onQueryBlockNbt(packet: QueryBlockNbtC2SPacket?) {}

    override fun requestTeleport(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {}

    override fun onPlayerInteractItem(packet: PlayerInteractItemC2SPacket?) {}

    override fun syncWithPlayerPosition() {}

    override fun onUpdateBeacon(packet: UpdateBeaconC2SPacket?) {}

    override fun teleportRequest(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, set: MutableSet<PlayerPositionLookS2CPacket.Flag>?) {}

    override fun onResourcePackStatus(packet: ResourcePackStatusC2SPacket?) {}

    override fun onPlayerInteractEntity(packet: PlayerInteractEntityC2SPacket?) {}

    override fun onCreativeInventoryAction(packet: CreativeInventoryActionC2SPacket?) {}

    override fun onBoatPaddleState(packet: BoatPaddleStateC2SPacket?) {}

    override fun onRecipeCategoryOptions(packet: RecipeCategoryOptionsC2SPacket?) {}
}