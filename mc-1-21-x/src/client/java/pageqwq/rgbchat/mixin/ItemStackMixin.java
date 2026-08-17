package pageqwq.rgbchat.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pageqwq.rgbchat.RgbCompat;
import pageqwq.rgbchat.RgbComponents;
import pageqwq.rgbchat.RgbConfig;

/**
 * 物品名的 Modern UI 兼容入口：把悬停名组件改写为逐字符样式组件。
 *
 * <p>纯显示转换：物品数据（NBT 中的自定义名）保持不变。铁砧改名的结果物品、物品栏、
 * 热键栏与提示框全部经由此入口获得颜色。
 *
 * <p>另外，改名会触发 vanilla 的稀有度提升（提示框边框变色），对含标签的彩色改名还原为
 * 基础稀有度（Modern UI 下提示框边框随稀有度着色，效果更明显）。
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void rgbchat$convertHoverName(CallbackInfoReturnable<Component> cir) {
        if (!RgbCompat.isModernUiLoaded()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.itemNames) {
            return;
        }
        Component converted = RgbComponents.convert(cir.getReturnValue());
        if (converted != cir.getReturnValue()) {
            cir.setReturnValue(converted);
        }
    }

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void rgbchat$noRarityBumpForTags(CallbackInfoReturnable<Rarity> cir) {
        if (!RgbCompat.isModernUiLoaded()) {
            return;
        }
        RgbConfig config = RgbConfig.get();
        if (!config.enabled || !config.itemNames) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        Component customName = self.get(DataComponents.CUSTOM_NAME);
        if (customName == null || RgbComponents.convert(customName) == customName) {
            return;
        }
        cir.setReturnValue(self.getOrDefault(DataComponents.RARITY, Rarity.COMMON));
    }
}
