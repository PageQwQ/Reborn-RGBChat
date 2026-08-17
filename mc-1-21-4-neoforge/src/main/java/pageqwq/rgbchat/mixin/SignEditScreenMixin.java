package pageqwq.rgbchat.mixin;

import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;
import pageqwq.rgbchat.RgbInputFormatter;

/**
 * 告示牌编辑界面的标签隐藏（Modern UI）与宽度上限修正（所有模式）。
 *
 * <p>宽度上限：{@code TextFieldHelper} 的校验谓词用 {@code font.width(text)} 判断是否还能输入，
 * 在 Modern UI 下标签字符会计入宽度。把谓词包一层、先去除标签再测量，两种模式都正确。

 * <p>渲染：Modern UI 下编辑界面的文字由 {@code renderSignText} 以原始字符串绘制（绕过组件层），
 * 把绘制与宽度测量重定向到 {@link RgbInputFormatter} 的逐字符样式序列——标签隐藏、逐字符着色，
 * 且绘制宽度与测量宽度一致，光标/选区精确。vanilla 模式走原有 StringDecomposer 路径不受影响。
 */
@Mixin(AbstractSignEditScreen.class)
abstract class SignEditScreenMixin {

    @ModifyArg(
            method = "init",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/TextFieldHelper;"
                    + "<init>(Ljava/util/function/Supplier;Ljava/util/function/Consumer;"
                    + "Ljava/util/function/Supplier;Ljava/util/function/Consumer;"
                    + "Ljava/util/function/Predicate;)V"),
            index = 4)
    private Predicate<String> rgbchat$stripTagsForLimit(Predicate<String> original) {
        return s -> original.test(RgbCompat.stripTags(s));
    }

    @Redirect(
            method = "renderSignText",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"))
    private int rgbchat$widthVisible(Font font, String text) {
        if (!rgbchat$enabled()) {
            return font.width(text);
        }
        return font.width(RgbInputFormatter.format(text, 0));
    }

    @Redirect(
            method = "renderSignText",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;"
                            + "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int rgbchat$drawVisible(GuiGraphics graphics, Font font, String text,
                                    int x, int y, int color, boolean shadow) {
        if (!rgbchat$enabled()) {
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        return graphics.drawString(font, RgbInputFormatter.format(text, 0), x, y, color, shadow);
    }

    private boolean rgbchat$enabled() {
        if (!RgbCompat.isModernUiLoaded()) {
            return false;
        }
        RgbConfig config = RgbConfig.get();
        return config.enabled && config.signs;
    }
}
