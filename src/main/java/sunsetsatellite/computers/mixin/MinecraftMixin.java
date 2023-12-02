package sunsetsatellite.computers.mixin;

import dan200.shared.ItemDisk;
import net.minecraft.client.Minecraft;
import net.minecraft.core.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sunsetsatellite.computers.Computers;

@Mixin(value = Minecraft.class, remap = false)
public abstract class MinecraftMixin {
	@Shadow
	public World theWorld;

	@Inject(
		method = "startGame",
		at = @At("TAIL")
	)
	public void start(CallbackInfo ci){
		Computers.instance.load();
	}

	@Inject(
		method = "runTick",
		at = @At("TAIL")
	)
	public void tick(CallbackInfo ci){
		ItemDisk.loadLabelsIfWorldChanged(theWorld);
		Computers.m_tickCount++;
	}

	@Shadow
	private static Minecraft theMinecraft;

	@Inject(method = "getMinecraft(Ljava/lang/Class;)Lnet/minecraft/client/Minecraft;", at = @At("HEAD"), cancellable = true)
	private static void returnMinecraft(Class<?> caller, CallbackInfoReturnable<Minecraft> cir){
		cir.setReturnValue(theMinecraft);
	}

}
