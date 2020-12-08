package pl.ks.profiling.safepoint.analyzer.standalone;

import java.awt.Font;

class PresentationFontProvider {
    public static final Font DEFAULT_FONT = new Font("Helvetica Neue", Font.PLAIN, 14);
    public static final Font DEFAULT_BOLD_FONT = new Font("Helvetica Neue", Font.BOLD, 14);
    public static final Font DEFAULT_H2_FONT = new Font("Helvetica Neue", Font.BOLD, 18);
    public static final Font DEFAULT_H1_FONT = new Font("Helvetica Neue", Font.BOLD, 22);
    public static final Font MONOSPACE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public static final Font PRESENTATION_DEFAULT_FONT = new Font("Helvetica Neue", Font.PLAIN, 28);
    public static final Font PRESENTATION_DEFAULT_BOLD_FONT = new Font("Helvetica Neue", Font.BOLD, 28);
    public static final Font PRESENTATION_DEFAULT_H2_FONT = new Font("Helvetica Neue", Font.BOLD, 32);
    public static final Font PRESENTATION_DEFAULT_H1_FONT = new Font("Helvetica Neue", Font.BOLD, 34);
    public static final Font PRESENTATION_MONOSPACE_FONT = new Font(Font.MONOSPACED, Font.BOLD, 28);

    private boolean presentationMode;

    public PresentationFontProvider(boolean presentationMode) {
        this.presentationMode = presentationMode;
    }

    public Font getDefaultFont() {
        if (presentationMode) {
            return PRESENTATION_DEFAULT_FONT;
        }
        return DEFAULT_FONT;
    }

    public Font getDefaultBoldFont() {
        if (presentationMode) {
            return PRESENTATION_DEFAULT_BOLD_FONT;
        }
        return DEFAULT_BOLD_FONT;
    }

    public Font getDefaultH2Font() {
        if (presentationMode) {
            return PRESENTATION_DEFAULT_H2_FONT;
        }
        return DEFAULT_H2_FONT;
    }

    public Font getDefaultH1Font() {
        if (presentationMode) {
            return PRESENTATION_DEFAULT_H1_FONT;
        }
        return DEFAULT_H1_FONT;
    }

    public Font getMonospaceFont() {
        if (presentationMode) {
            return PRESENTATION_MONOSPACE_FONT;
        }
        return MONOSPACE_FONT;
    }
}
