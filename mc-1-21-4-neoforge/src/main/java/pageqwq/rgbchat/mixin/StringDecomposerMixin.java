package pageqwq.rgbchat.mixin;

import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RenderContext;
import pageqwq.rgbchat.RgbConfig;
import pageqwq.rgbchat.RgbParser;
import pageqwq.rgbchat.RgbStyles;
import pageqwq.rgbchat.StyledChar;

/**
 * Single hook covering every formatted-text render path: all String and
 * FormattedText overloads of {@code StringDecomposer.iterateFormatted}
 * funnel into this five-argument method. Strings containing RGB tags are
 * decomposed by the shared parser and fed to the sink with RGB styles;
 * everything else is left to vanilla untouched.
 */
@Mixin(StringDecomposer.class)
public abstract class StringDecomposerMixin {

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
