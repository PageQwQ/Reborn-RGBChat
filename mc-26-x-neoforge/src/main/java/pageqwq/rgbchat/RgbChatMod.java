package pageqwq.rgbchat;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RGB Chat entry point. Client-only; on dedicated servers this does nothing. */
@Mod("rgbchat")
public class RgbChatMod {
    public static final Logger LOGGER = LoggerFactory.getLogger("rgbchat");

    public RgbChatMod() {
        if (FMLEnvironment.getDist().isClient()) {
            RgbConfigIO.load();
        }
    }
}