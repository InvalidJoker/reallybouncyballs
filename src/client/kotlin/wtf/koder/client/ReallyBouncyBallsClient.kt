package wtf.koder.client

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import wtf.koder.ReallyBouncyBalls

object ReallyBouncyBallsClient : ClientModInitializer {
	override fun onInitializeClient() {
		EntityRenderers.register(ReallyBouncyBalls.BOUNCY_BALL_PROJECTILE) { context ->
			ThrownItemRenderer(context)
		}
	}
}
