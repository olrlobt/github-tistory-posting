package olrlobt.githubtistoryposting.utils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class FontUtils {

    private static Font Font_Medium;
    private static Font Font_Size;
    private static Font Font_Bold;

    @PostConstruct
    private void init() {
//		Font_Medium = setFont("/static/font/NotoSansKR-Medium.ttf", 14f);
        Font_Size = setFont("/static/font/SF-Pro-Display-Regular.otf", 14f);
        Font_Medium = setFont("/static/font/segoe-ui.ttf", 14f);
//		Font_Bold = setFont("/static/font/NotoSansKR-Bold.ttf", 14f);
        Font_Bold = setFont("/static/font/segoe-ui.ttf", 14f);
    }

    private Font setFont(String path, float size) {
        try (InputStream is = FontUtils.class.getResourceAsStream(path)) {
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Font load_size(float size) {
        return Font_Size.deriveFont(size);
    }

    public static Font load_b() {
        return Font_Bold.deriveFont(Font.BOLD);
    }

    public static Font load_b(float size) {
        return Font_Bold.deriveFont(size).deriveFont(Font.BOLD);
    }

    public static Font load_m() {
        return Font_Medium;
    }

    public static Font load_m(float size) {
        return Font_Medium.deriveFont(size);
    }
}