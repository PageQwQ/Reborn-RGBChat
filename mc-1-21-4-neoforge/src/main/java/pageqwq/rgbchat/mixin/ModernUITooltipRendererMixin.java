package pageqwq.rgbchat.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbConfig;

/**
 * Modern UI 提示框边框取色（渐变改名）。
 *
 * <p>Modern UI 的 {@code TooltipRenderer.computeWorkingColor} 从悬停名的逐字符颜色收集边框色：
 * 长渐变名会收集到超过 4 种颜色而进入彩虹频谱模式。此 Mixin 在名字含渐变标签时直接取渐变
 * 停靠点的多色值作为四角边框色，跳过其默认逻辑（@Pseudo + require=0：Modern UI 缺失或版本
 * 不匹配时整体跳过）。
 */
@Pseudo
@Mixin(targets = "icyllis.modernui.mc.TooltipRenderer", remap = false)
abstract class ModernUITooltipRendererMixin {

    @Shadow(remap = false)
    private static int[] sStrokeColor;

    @Shadow(remap = false)
    private int[] mWorkStrokeColor;

    @Shadow(remap = false)
    private boolean mUseSpectrum;

    @Inject(method = "computeWorkingColor", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void rgbchat$gradientBorder(ItemStack item, CallbackInfo ci) {
        if (!RgbCompat.isModernUiLoaded()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.itemNames || item == null || item.isEmpty()) {
            return;
        }
        Component customName = item.get(DataComponents.CUSTOM_NAME);
        if (customName == null) {
            return;
        }
        int[] stops = RgbCompat.extractGradientStops(customName.getString());
        if (stops == null) {
            return;
        }
        int[] corners = rgbchat$corners(stops);
        this.mWorkStrokeColor[0] = sStrokeColor[0] & 0xFF000000 | corners[0];
        this.mWorkStrokeColor[1] = sStrokeColor[1] & 0xFF000000 | corners[1];
        this.mWorkStrokeColor[2] = sStrokeColor[2] & 0xFF000000 | corners[2];
        this.mWorkStrokeColor[3] = sStrokeColor[3] & 0xFF000000 | corners[3];
        this.mUseSpectrum = false;
        ci.cancel();
    }

    /** 停靠点 → 四角颜色：1 个铺满；2 个左右渐变；3 个补插值；4+ 均匀采样 4 个。 */
    private static int[] rgbchat$corners(int[] stops) {
        int[] c = new int[4];
        if (stops.length == 1) {
            c[0] = c[1] = c[2] = c[3] = stops[0];
        } else if (stops.length == 2) {
            c[0] = c[1] = stops[0];
            c[2] = c[3] = stops[1];
        } else if (stops.length == 3) {
            c[0] = stops[0];
            c[1] = stops[1];
            c[2] = stops[2];
            c[3] = stops[1];
        } else {
            for (int k = 0; k < 4; k++) {
                c[k] = stops[Math.round((float) k / 3.0f * (stops.length - 1))];
            }
        }
        return c;
    }
}
