package pageqwq.rgbchat.mixin;

import java.util.function.BiFunction;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pageqwq.rgbchat.RgbChatInput;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;
import pageqwq.rgbchat.RgbInputFormatter;

/**
 * 聊天输入框的 Modern UI 兼容配套：标记输入框、安装实时预览 formatter、发送前解码。
 *
 * <p>输入框内的标签被 {@code EditBoxMixin} 规范化为旧式 {@code §x} 色码（隐藏 + 光标精确），
 * 但 1.19+ 服务端会剥离玩家消息中的 {@code §}，因此在 {@code handleChatInput} 发送前把色码
 * 解码回 {@code #RRGGBB} 标签。
 *
 * <p>{@code setFormatter} 在 1.21.9+ 被移除（且无官方 Modern UI），用反射安装以便同一份源码
 * 兼容全版本，缺失时安全跳过。
 */
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {

    @Shadow
    @Final
    private EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void rgbchat$markChatInput(CallbackInfo ci) {
        if (!RgbCompat.isModernUiLoaded()) {
            return;
        }
        RgbChatInput.mark(this.input);
        try {
            this.input.getClass()
                    .getMethod("setFormatter", BiFunction.class)
                    .invoke(this.input, RgbInputFormatter.instance());
        } catch (ReflectiveOperationException ignored) {
            // 无 setFormatter 的版本（1.21.9+）：无官方 Modern UI，忽略
        }
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void rgbchat$decodeOnSend(String input, boolean addToHistory, CallbackInfo ci) {
        if (!RgbCompat.isModernUiLoaded() || !RgbChatInput.isMarked(this.input)) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.inputPreview) {
            return;
        }
        String decoded = RgbCompat.decodeInputHex(input);
        if (decoded != input) {
            ((ChatScreen) (Object) this).handleChatInput(decoded, addToHistory);
            ci.cancel();
        }
    }
}
