package pageqwq.rgbchat.mixin;

import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pageqwq.rgbchat.RenderContext;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;
import pageqwq.rgbchat.RgbParser;
import pageqwq.rgbchat.RgbStyles;
import pageqwq.rgbchat.StyledChar;

/**
 * 普通（非 Modern UI）渲染路径的标签着色入口。
 *
 * <p>Modern UI 加载时直接放行：其布局引擎（{@code TextLayoutEngine} / {@code
 * ModernStringSplitter} / {@code MixinEditBox}）按原始字符串索引布局与光标位置，任何在此处的
 * 文本改写都会破坏下标映射（AIOOBE 崩溃或光标/换行错位）。Modern UI 下的着色由
 * {@code ChatComponentMixin} / {@code SignTextMixin} / {@code ItemStackMixin} 在组件层完成，
 * 输入框由 {@code EditBoxMixin} 做字符串级规范化。
 */
@Mixin(StringDecomposer.class)
abstract class StringDecomposerMixin {

    @Inject(
            method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;"
                    + "Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void rgbchat$applyRgbTags(String text, int start, Style style, Style resetStyle,
                                             FormattedCharSink sink, CallbackInfoReturnable<Boolean> cir) {
        if (RgbCompat.isModernUiLoaded()) {
            return;
        }
        if (!RgbParser.containsTag(text)) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.allows(RenderContext.current())) {
            return;
        }
        List<StyledChar> chars = RgbParser.parse(text, config.gradient);
        for (StyledChar sc : chars) {
            if (sc.index < start) {
                continue;
            }
            if (!sink.accept(sc.index, RgbStyles.apply(style, sc.format), sc.codePoint)) {
                cir.setReturnValue(false);
                return;
            }
        }
        cir.setReturnValue(true);
    }
}
