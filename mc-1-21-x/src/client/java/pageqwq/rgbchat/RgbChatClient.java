package pageqwq.rgbchat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RgbChatClient implements ClientModInitializer {
    public static final String MOD_ID = "rgbchat";
    public static final Logger LOGGER = LoggerFactory.getLogger("RGB Chat Reborn");

    @Override
    public void onInitializeClient() {
        RgbCompat.setModernUiLoaded(FabricLoader.getInstance().isModLoaded("modernui"));
        RgbConfigIO.load();
        LOGGER.info("RGB Chat Reborn initialized");
    }
}
