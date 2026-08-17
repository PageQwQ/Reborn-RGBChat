package pageqwq.rgbchat.mixin;

import java.util.function.Predicate;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pageqwq.rgbchat.RgbCompat;

/**
 * 告示牌编辑界面的宽度上限修正（所有模式）：{@code TextFieldHelper} 的校验谓词用
 * {@code font.width(text)} 判断是否还能输入，标签字符不应计入宽度。把谓词包一层、先去除标签
 * 再测量。
 *
 * <p>Modern UI 下编辑界面的逐字符着色渲染（{@code renderSignText} 重定向）因不同版本
 * {@code GuiGraphics.drawString} 返回值不同（1.21.9+ 改为 void），仅在本模块外的
 * 1.21.1/1.21.4 模块实现；本模块的编辑界面文本由世界内告示牌渲染（{@code SignTextMixin}）
 * 承担标签隐藏。
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
}
