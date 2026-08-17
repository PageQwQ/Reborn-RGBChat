package pageqwq.rgbchat;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 标记聊天输入框实例（Modern UI 兼容用）。
 *
 * <p>仅被标记的 {@code EditBox} 参与输入框标签规范化，避免污染搜索框、告示牌等其它输入框。
 * 标记在 {@code ChatScreenMixin} 的 {@code init} 中完成。
 */
public final class RgbChatInput {

    private static final Set<Object> MARKED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private RgbChatInput() {
    }

    public static void mark(Object editBox) {
        MARKED.add(editBox);
    }

    public static boolean isMarked(Object editBox) {
        return MARKED.contains(editBox);
    }
}
