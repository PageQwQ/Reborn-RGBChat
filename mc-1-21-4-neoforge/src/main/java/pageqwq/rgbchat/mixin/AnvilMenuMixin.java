package pageqwq.rgbchat.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;

/**
 * 铁砧改名发送前解码：输入框中的标签被规范化为旧式 {@code §x} 色码，发送前转回
 * {@code #RRGGBB}（1.19+ 服务端会剥离玩家消息中的 {@code §}，且物品名需要以标签形式存储）。
 *
 * <p>解码幂等：递归调用输入已无 {@code §x} 色码，直接放行。服务端收到的是原始标签串。
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {

    @Inject(method = "setItemName", at = @At("HEAD"), cancellable = true)
    private void rgbchat$decodeRename(String name, CallbackInfoReturnable<Boolean> cir) {
        if (!RgbCompat.isModernUiLoaded()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.inputPreview) {
            return;
        }
        String decoded = RgbCompat.decodeInputHex(name);
        if (decoded != name) {
            cir.setReturnValue(((AnvilMenu) (Object) this).setItemName(decoded));
        }
    }
}
