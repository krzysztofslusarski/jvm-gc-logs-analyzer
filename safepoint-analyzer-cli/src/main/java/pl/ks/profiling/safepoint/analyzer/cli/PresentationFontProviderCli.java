package pl.ks.profiling.safepoint.analyzer.cli;

import pl.ks.profiling.xchart.commons.PresentationFontProvider;

import java.awt.*;

public class PresentationFontProviderCli implements PresentationFontProvider {
    public static final Font DEFAULT_FONT = new Font("Helvetica Neue", Font.PLAIN, 14);
    public static final Font DEFAULT_BOLD_FONT = new Font("Helvetica Neue", Font.BOLD, 14);

    @Override
    public Font getDefaultFont() {
        return DEFAULT_FONT;
    }

    @Override
    public Font getDefaultBoldFont() {
        return DEFAULT_BOLD_FONT;
    }
}
