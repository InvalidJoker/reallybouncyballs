package wtf.koder

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.HitResult

class BouncyBallProjectile : ThrowableItemProjectile {
	var bouncesLeft: Int = 0
	var isReturning: Boolean = false
	var isInitialised: Boolean = false

	constructor(type: EntityType<BouncyBallProjectile>, level: Level) : super(type, level)

	constructor(level: Level, shooter: LivingEntity, stack: ItemStack) :
		super(ReallyBouncyBalls.BOUNCY_BALL_PROJECTILE, shooter, level, stack)

	override fun getDefaultItem(): Item = ReallyBouncyBalls.BOUNCY_BALL

	override fun tick() {
		BallPhysics.handleTick(this)

		if (isRemoved) return
		super.tick()
	}

	override fun onHit(hitResult: HitResult) {
		if (BallPhysics.handleHit(this, hitResult)) return
		land()
	}

	private fun land() {
		val level = level()
		if (level !is ServerLevel) return

		spawnAtLocation(level, item.copyWithCount(1), 0.1f)
		discard()
	}

	override fun addAdditionalSaveData(output: ValueOutput) {
		super.addAdditionalSaveData(output)
		if (!isInitialised) return

		output.putInt("Bounces", bouncesLeft)
		output.putBoolean("Returning", isReturning)
	}

	override fun readAdditionalSaveData(input: ValueInput) {
		super.readAdditionalSaveData(input)

		val bounces = input.getIntOr("Bounces", -1)
		if (bounces >= 0) {
			bouncesLeft = bounces
			isInitialised = true
		}

		isReturning = input.getBooleanOr("Returning", false)
	}
}
