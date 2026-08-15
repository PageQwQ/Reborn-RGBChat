package pageqwq.rgbchat;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RGB Chat entry point. Mixins are client-only; dedicated servers just load config. */
@Mod("rgbchat")
public class RgbChatMod {
    public static final Logger LOGGER = LoggerFactory.getLogger("rgbchat");

    public RgbChatMod() {
        RgbConfigIO.load();
    }
}
