package pageqwq.rgbchat.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pageqwq.rgbchat.RgbChatInput;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;

/**
 * 聊天输入框的 Modern UI 标签隐藏：把 {@code #RRGGBB} 标签规范化为旧式
 * {@code §x§R§R§G§G§B§B} 色码。
 *
 * <p>vanilla 分解器完整吞掉色码（14 字符 = 7 个 {@code §} 对），Modern UI 的输入框光标/选区
 * 数学（{@code MixinEditBox.onRenderWidget}）对 {@code §} 对做精确补偿，因此标签被隐藏且光标
 * 精确。{@code RgbInputFormatter} 负责为可见字符着色，发送时由 {@code ChatScreenMixin} 转回
 * 原始标签。
 *
 * <p>仅对 {@link RgbChatInput} 标记的聊天输入框生效；光标、选区与显示偏移通过
 * {@link RgbCompat#encodeInputTags(String, int[])} 的位置映射表同步。
 */
@Mixin(EditBox.class)
abstract class EditBoxMixin {

    @Shadow
    private String value;
    @Shadow
    private int cursorPos;
    @Shadow
    private int highlightPos;
    @Shadow
    private int displayPos;

    @Shadow
    protected abstract void onValueChange(String newText);

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void rgbchat$alignScroll(CallbackInfo ci) {
        if (!rgbchat$enabled()) {
            return;
        }
        // 长文本滚动时 displayPos 可能落在色码内部，把滚动位置对齐到色码边界，
        // 避免截断的色码在光标附近显示成大写十六进制残片。
        int aligned = RgbCompat.alignScrollPosition(this.value, this.displayPos);
        if (aligned != this.displayPos) {
            this.displayPos = aligned;
        }
    }

    @Inject(method = "setValue", at = @At("TAIL"))
    private void rgbchat$canonicalizeSetValue(String text, CallbackInfo ci) {        if (!rgbchat$enabled()) {
            return;
        }
        String raw = this.value;
        int[] map = new int[raw.length() + 1];
        String encoded = RgbCompat.encodeInputTags(raw, map);
        if (encoded != raw) {
            this.value = encoded;
            this.cursorPos = map[this.cursorPos];
            this.highlightPos = map[this.highlightPos];
            this.displayPos = map[this.displayPos];
        }
    }

    @Inject(method = "insertText", at = @At("TAIL"))
    private void rgbchat$canonicalizeInsertText(String text, CallbackInfo ci) {
        if (!rgbchat$enabled()) {
            return;
        }
        String raw = this.value;
        int[] map = new int[raw.length() + 1];
        String encoded = RgbCompat.encodeInputTags(raw, map);
        if (encoded != raw) {
            this.value = encoded;
            this.cursorPos = map[this.cursorPos];
            this.highlightPos = map[this.highlightPos];
            this.displayPos = map[this.displayPos];
            this.onValueChange(encoded);
        }
    }

    @Inject(method = "deleteChars", at = @At("TAIL"))
    private void rgbchat$canonicalizeDeleteChars(int numChars, CallbackInfo ci) {
        if (!rgbchat$enabled()) {
            return;
        }
        String raw = this.value;
        int[] map = new int[raw.length() + 1];
        String encoded = RgbCompat.encodeInputTags(raw, map);
        if (encoded != raw) {
            this.value = encoded;
            this.cursorPos = map[this.cursorPos];
            this.highlightPos = map[this.highlightPos];
            this.displayPos = map[this.displayPos];
            this.onValueChange(encoded);
        }
    }

    private boolean rgbchat$enabled() {
        if (!RgbCompat.isModernUiLoaded() || !RgbChatInput.isMarked(this)) {
            return false;
        }
        RgbConfig config = RgbConfig.get();
        return config.enabled && config.inputPreview;
    }
}
