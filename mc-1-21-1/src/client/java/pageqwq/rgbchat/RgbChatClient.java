package pageqwq.rgbchat;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RgbChatClient implements ClientModInitializer {
    public static final String MOD_ID = "rgbchat";
    public static final Logger LOGGER = LoggerFactory.getLogger("RGB Chat Reborn");

    @Override
    public void onInitializeClient() {
        RgbConfigIO.load();
        LOGGER.info("RGB Chat Reborn initialized");
    }
}
