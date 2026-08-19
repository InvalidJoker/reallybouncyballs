package wtf.koder

import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

class BouncyBallItem(properties: Properties) : Item(properties) {
	override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
		val stack = player.getItemInHand(hand)

		level.playSound(
			null, player.x, player.y, player.z,
			SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
			0.5f, 0.4f / (level.random.nextFloat() * 0.4f + 0.8f)
		)

		if (level is ServerLevel) {
			Projectile.spawnProjectileFromRotation(
				{ serverLevel, shooter, thrown -> BouncyBallProjectile(serverLevel, shooter, thrown) },
				level, stack, player, 0.0f, THROW_POWER, 1.0f
			)
		}

		player.awardStat(Stats.ITEM_USED.get(this))
		stack.consume(1, player)
		return InteractionResult.SUCCESS
	}

	companion object {
		private const val THROW_POWER = 1.5f // copied from EP
	}
}
