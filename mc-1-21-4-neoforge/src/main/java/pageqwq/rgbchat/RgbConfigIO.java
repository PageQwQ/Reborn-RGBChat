package pageqwq.rgbchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads and saves {@code config/rgbchat.json}. */
public final class RgbConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger("rgbchat");

    private RgbConfigIO() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("rgbchat.json");
    }

    public static void load() {
        Path file = file();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                RgbConfig config = GSON.fromJson(reader, RgbConfig.class);
                RgbConfig.set(config);
                return;
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.warn("Failed to read {}, using defaults", file, e);
                RgbConfig.set(new RgbConfig());
                return;
            }
        }
        RgbConfig.set(new RgbConfig());
        save();
    }

    public static void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(RgbConfig.get(), writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write {}", file, e);
        }
    }
}
