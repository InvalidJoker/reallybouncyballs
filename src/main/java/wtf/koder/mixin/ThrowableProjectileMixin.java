package wtf.koder.mixin;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.koder.BouncyBallProjectile;

@Mixin(ThrowableProjectile.class)
public class ThrowableProjectileMixin {
	@Redirect(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getHitResultOnMoveVector(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)Lnet/minecraft/world/phys/HitResult;"
			)
	)
	private HitResult reallybouncyballs$ignoreHitsWhileReturning(Entity entity, Predicate<Entity> filter) {
		if (entity instanceof BouncyBallProjectile ball && ball.isReturning()) {
			Vec3 target = entity.position().add(entity.getDeltaMovement());
			return BlockHitResult.miss(target, Direction.UP, BlockPos.containing(target));
		}

		return ProjectileUtil.getHitResultOnMoveVector(entity, filter);
	}
}
