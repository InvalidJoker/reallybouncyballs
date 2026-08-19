package wtf.koder

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import org.slf4j.LoggerFactory

object ReallyBouncyBalls : ModInitializer {
	const val MOD_ID: String = "reallybouncyballs"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	private const val BALL_ENCHANTMENT_VALUE = 10
	private const val BALL_STACK_SIZE = 16

	// loyality side effects I discovered...
	private val POINTLESS_ON_BALLS: List<ResourceKey<Enchantment>> = listOf(
		Enchantments.RIPTIDE,
		Enchantments.CHANNELING,
		Enchantments.IMPALING,
	)

	private val BOUNCY_BALL_KEY: ResourceKey<Item> =
		ResourceKey.create(Registries.ITEM, id("bouncy_ball"))

	private val BOUNCY_BALL_PROJECTILE_KEY: ResourceKey<EntityType<*>> =
		ResourceKey.create(Registries.ENTITY_TYPE, id("bouncy_ball"))

	val BOUNCY_BALL: Item = Registry.register(
		BuiltInRegistries.ITEM,
		BOUNCY_BALL_KEY,
		BouncyBallItem(
			Item.Properties()
				.setId(BOUNCY_BALL_KEY)
				.stacksTo(BALL_STACK_SIZE)
				.enchantable(BALL_ENCHANTMENT_VALUE)
		)
	)

	val BOUNCY_BALL_PROJECTILE: EntityType<BouncyBallProjectile> = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		BOUNCY_BALL_PROJECTILE_KEY,
		EntityType.Builder
			.of({ type, level -> BouncyBallProjectile(type, level) }, MobCategory.MISC)
			.sized(0.25f, 0.25f)
			.clientTrackingRange(4)
			.updateInterval(10)
			.build(BOUNCY_BALL_PROJECTILE_KEY)
	)

	override fun onInitialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { output ->
			output.insertAfter(Items.ENDER_PEARL, BOUNCY_BALL)
		}

		EnchantmentEvents.ALLOW_ENCHANTING.register { enchantment, target, _ ->
			if (target.item === BOUNCY_BALL && POINTLESS_ON_BALLS.any { enchantment.`is`(it) }) {
				TriState.FALSE
			} else {
				TriState.DEFAULT
			}
		}

		LOGGER.info("ReallyBouncyBalls loaded - go throw a bouncy ball.")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
