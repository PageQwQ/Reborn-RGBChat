package pageqwq.rgbchat.mixin;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbComponents;
import pageqwq.rgbchat.RgbConfig;

/**
 * 聊天 HUD 的 Modern UI 兼容入口。
 *
 * <p>在消息进入 {@code ComponentSplitter} 行拆分之前，把 {@code #RRGGBB} 标签改写为逐字符
 * {@code TextColor} 样式组件。Modern UI 的布局引擎按组件样式逐字形渲染 RGB，且行拆分
 * （{@code ModernStringSplitter.computeLineBreaks}）逐片段统计可见字符，与布局侧一致。
 *
 * <p>两个 addMessage 重载都拦截；递归调用由线程局部守卫放行，转换后的组件不再包含有效标签，
 * {@link RgbComponents#convert} 也会对无变化文本返回原实例（引用相等）。
 */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {

    private static final ThreadLocal<Boolean> RGBCHAT$CONVERTING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rgbchat$onAddMessage1(Component message, CallbackInfo ci) {
        rgbchat$convertAndRequeue(message, null, null, ci);
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;"
                    + "Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rgbchat$onAddMessage3(Component message, MessageSignature signature,
                                       GuiMessageTag tag, CallbackInfo ci) {
        rgbchat$convertAndRequeue(message, signature, tag, ci);
    }

    private void rgbchat$convertAndRequeue(Component message, MessageSignature signature,
                                           GuiMessageTag tag, CallbackInfo ci) {
        if (!RgbCompat.isModernUiLoaded() || RGBCHAT$CONVERTING.get()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.chat) {
            return;
        }
        Component converted = RgbComponents.convert(message);
        if (converted == message) {
            return;
        }
        RGBCHAT$CONVERTING.set(Boolean.TRUE);
        try {
            ChatComponent self = (ChatComponent) (Object) this;
            if (signature == null) {
                self.addMessage(converted);
            } else {
                self.addMessage(converted, signature, tag);
            }
        } finally {
            RGBCHAT$CONVERTING.set(Boolean.FALSE);
        }
        ci.cancel();
    }
}
