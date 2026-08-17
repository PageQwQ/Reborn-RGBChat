package pageqwq.rgbchat.mixin;

import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbComponents;
import pageqwq.rgbchat.RgbConfig;

/**
 * 告示牌的 Modern UI 兼容入口：渲染行时把每行组件改写为逐字符样式组件。
 *
 * <p>仅作用于显示（{@code getRenderMessages} 的缓存结果），不修改告示牌数据。
 * 放置后的告示牌与编辑界面（编辑界面背后的世界内渲染）共用此路径。
 */
@Mixin(SignText.class)
abstract class SignTextMixin {

    private static final ThreadLocal<Boolean> RGBCHAT$CONVERTING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "getRenderMessages", at = @At("HEAD"), cancellable = true)
    private void rgbchat$convertLines(boolean front, Function<Component, FormattedCharSequence> transformer,
                                      CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (!RgbCompat.isModernUiLoaded() || RGBCHAT$CONVERTING.get()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.signs) {
            return;
        }
        RGBCHAT$CONVERTING.set(Boolean.TRUE);
        try {
            SignText self = (SignText) (Object) this;
            cir.setReturnValue(self.getRenderMessages(front,
                    component -> transformer.apply(RgbComponents.convert(component))));
        } finally {
            RGBCHAT$CONVERTING.set(Boolean.FALSE);
        }
    }
}
