package wtf.koder

import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.ARGB
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object BallPhysics {
	// base bounces
	private const val BASE_BOUNCES = 3
	// per ench level
	private const val BOUNCES_PER_LEVEL = 2


	private const val BASE_RESTITUTION = 0.62
	private const val RESTITUTION_PER_LEVEL = 0.09
	private const val TANGENT_FRICTION = 0.94
	private const val MIN_BOUNCE_SPEED = 0.14
	private const val SLIME_BOOST = 1.7

	// default color (when I give it to me in creative)
	private const val DEFAULT_BALL_COLOR = 0xE8E8E8

	private const val RETURN_PICKUP_DISTANCE = 1.5
	private const val RETURN_BASE_SPEED = 0.12
	private const val RETURN_SPEED_PER_LEVEL = 0.08
	private const val RETURN_MAX_SPEED = 2.0

	val BOUNCINESS: ResourceKey<Enchantment> =
		ResourceKey.create(Registries.ENCHANTMENT, ReallyBouncyBalls.id("bounciness"))

	fun handleHit(ball: BouncyBallProjectile, hit: HitResult): Boolean {
		if (ball.isReturning) return true

		initialise(ball)

		return when (hit) {
			is BlockHitResult -> bounceOffBlock(ball, hit)
			is EntityHitResult -> bounceOffEntity(ball, hit)
			else -> false
		}
	}

	fun handleTick(ball: BouncyBallProjectile) {
		if (!ball.isReturning) return
		val level = ball.level() as? ServerLevel ?: return

		val stack = ball.item.copyWithCount(1)
		val owner = ball.owner
		if (owner == null || !owner.isAlive || (owner is ServerPlayer && owner.isSpectator)) {
			ball.spawnAtLocation(level, stack, 0.1f)
			ball.discard()
			return
		}

		val toOwner = owner.eyePosition.subtract(ball.position())
		if (toOwner.length() < RETURN_PICKUP_DISTANCE) {
			if (owner is Player && owner.inventory.add(stack)) {
				level.playSound(null, owner.x, owner.y, owner.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.8f)
			} else {
				ball.spawnAtLocation(level, stack, 0.1f)
			}
			ball.discard()
			return
		}

		ball.setNoGravity(true)
		val pull = RETURN_BASE_SPEED + RETURN_SPEED_PER_LEVEL * loyaltyLevel(ball)
		var movement = ball.deltaMovement.scale(0.9).add(toOwner.normalize().scale(pull))
		if (movement.length() > RETURN_MAX_SPEED) {
			movement = movement.normalize().scale(RETURN_MAX_SPEED)
		}
		ball.setDeltaMovement(movement)

		level.sendParticles(
			DustParticleOptions(ballColor(ball.item), 0.8f),
			ball.x, ball.y, ball.z, 1, 0.05, 0.05, 0.05, 0.0
		)
	}

	fun bouncinessLevel(stack: ItemStack): Int = enchantmentLevel(stack, BOUNCINESS)

	private fun loyaltyLevel(ball: BouncyBallProjectile): Int {
		val level = ball.level()
		return if (level is ServerLevel) {
			EnchantmentHelper.getTridentReturnToOwnerAcceleration(level, ball.item, ball)
		} else {
			enchantmentLevel(ball.item, Enchantments.LOYALTY)
		}
	}

	private fun enchantmentLevel(stack: ItemStack, enchantment: ResourceKey<Enchantment>): Int {
		val enchantments = stack.get(DataComponents.ENCHANTMENTS) ?: return 0
		for (entry in enchantments.entrySet()) {
			if (entry.key.`is`(enchantment)) return entry.intValue
		}
		return 0
	}

	private fun initialise(ball: BouncyBallProjectile) {
		if (ball.isInitialised) return
		ball.isInitialised = true
		ball.bouncesLeft = BASE_BOUNCES + BOUNCES_PER_LEVEL * bouncinessLevel(ball.item)
	}

	private fun bounceOffBlock(ball: BouncyBallProjectile, hit: BlockHitResult): Boolean {
		val blockState = ball.level().getBlockState(hit.blockPos)
		if (blockState.`is`(Blocks.HONEY_BLOCK) || blockState.`is`(Blocks.SOUL_SAND)) {
			return stopBouncing(ball)
		}

		var restitution = restitution(ball)
		if (blockState.`is`(Blocks.SLIME_BLOCK)) restitution *= SLIME_BOOST

		return reflect(ball, hit.direction.unitVec3, restitution, hit.location)
	}

	private fun bounceOffEntity(ball: BouncyBallProjectile, hit: EntityHitResult): Boolean {
		val normal = ball.position().subtract(hit.entity.boundingBox.center).normalize()
		if (normal.lengthSqr() < 1.0e-4) return false
		return reflect(ball, normal, restitution(ball), hit.location)
	}

	private fun restitution(ball: BouncyBallProjectile): Double =
		BASE_RESTITUTION + RESTITUTION_PER_LEVEL * bouncinessLevel(ball.item)

	private fun reflect(
		ball: BouncyBallProjectile,
		normal: Vec3,
		restitution: Double,
		contact: Vec3,
	): Boolean {
		if (ball.bouncesLeft <= 0) return stopBouncing(ball)

		val velocity = ball.deltaMovement
		val intoSurface = normal.scale(velocity.dot(normal))
		val bounced = velocity.subtract(intoSurface).scale(TANGENT_FRICTION)
			.subtract(intoSurface.scale(restitution))
		if (bounced.length() < MIN_BOUNCE_SPEED) return stopBouncing(ball)

		ball.bouncesLeft -= 1
		ball.setDeltaMovement(bounced)
		ball.setPos(ball.position().add(normal.scale(0.06)))
		playBounceEffects(ball, contact, ball.bouncesLeft)
		return true
	}

	private fun stopBouncing(ball: BouncyBallProjectile): Boolean {
		ball.bouncesLeft = 0
		if (loyaltyLevel(ball) <= 0) return false

		ball.isReturning = true
		ball.setNoGravity(true)
		ball.playSound(SoundEvents.TRIDENT_RETURN, 0.6f, 1.7f)
		return true
	}

	private fun playBounceEffects(ball: BouncyBallProjectile, contact: Vec3, bouncesLeft: Int) {
		val level = ball.level() as? ServerLevel ?: return
		level.sendParticles(
			DustParticleOptions(ballColor(ball.item), 1.0f),
			contact.x, contact.y, contact.z, 6, 0.12, 0.12, 0.12, 0.0
		)
		level.playSound(
			null, contact.x, contact.y, contact.z,
			SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL,
			0.5f, 0.8f + 0.1f * minOf(bouncesLeft, 8)
		)
	}

	private fun ballColor(stack: ItemStack): Int =
		DyedItemColor.getOrDefault(stack, ARGB.opaque(DEFAULT_BALL_COLOR))
}
