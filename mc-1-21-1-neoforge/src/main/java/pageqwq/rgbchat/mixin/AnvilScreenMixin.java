package pageqwq.rgbchat.mixin;

import java.util.function.BiFunction;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pageqwq.rgbchat.RgbChatInput;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbInputFormatter;

/**
 * 铁砧改名输入框的 Modern UI 兼容：标记输入框并安装实时预览 formatter。
 *
 * <p>标记后 {@code EditBoxMixin} 把标签规范化为旧式 {@code §x} 色码（隐藏 + 光标精确），
 * formatter 为可见字符着色；发送时 {@code AnvilMenuMixin} 在 {@code setItemName} 前把色码
 * 解码回 {@code #RRGGBB}。
 *
 * <p>标记点选 {@code onNameChanged}（AnvilScreen 的 {@code init} 无 Mojmap 映射名，无法作为
 * mixin 目标）；首次编辑后整串规范化，此后保持。
 */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {

    @Shadow
    private EditBox name;

    @Inject(method = "onNameChanged", at = @At("HEAD"))
    private void rgbchat$markRenameBox(String text, CallbackInfo ci) {
        if (!RgbCompat.isModernUiLoaded() || RgbChatInput.isMarked(this.name)) {
            return;
        }
        RgbChatInput.mark(this.name);
        try {
            this.name.getClass()
                    .getMethod("setFormatter", BiFunction.class)
                    .invoke(this.name, RgbInputFormatter.instance());
        } catch (ReflectiveOperationException ignored) {
            // 无 setFormatter 的版本：无官方 Modern UI，忽略
        }
    }
}
