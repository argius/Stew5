package stew5.ui.swing;

import java.awt.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.UIDefaults.ActiveValue;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

final class FontControlLookAndFeel extends BasicLookAndFeel {

    private static final LookAndFeel BASE = UIManager.getLookAndFeel();

    private final String fontFamily;
    private final int fontStyleMask;
    private final double sizeRate;

    FontControlLookAndFeel(String fontFamily, int fontStyleMask, double sizeRate) {
        this.fontFamily = fontFamily;
        this.fontStyleMask = fontStyleMask;
        this.sizeRate = sizeRate;
    }

    static void change(String fontFamily, int fontStyleMask, double sizeRate) {
        try {
            UIManager.setLookAndFeel(new FontControlLookAndFeel(fontFamily, fontStyleMask, sizeRate));
        } catch (UnsupportedLookAndFeelException ex) {
            throw new IllegalStateException(ex.toString());
        }
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = BASE.getDefaults();
        for (Entry<?, Object> entry : defaults.entrySet()) {
            Object key = entry.getKey();
            if (String.valueOf(key).endsWith("font")) {
                Object value = entry.getValue();
                if (value instanceof UIDefaults.ActiveValue) {
                    entry.setValue(new FontControlActiveValue((ActiveValue)value,
                                                              fontFamily,
                                                              fontStyleMask,
                                                              sizeRate));
                }
            }
        }
        return defaults;
    }

    @Override
    public String getDescription() {
        return BASE.getDescription();
    }

    @Override
    public String getID() {
        return BASE.getID();
    }

    @Override
    public String getName() {
        return BASE.getName();
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return BASE.isNativeLookAndFeel();
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return BASE.isSupportedLookAndFeel();
    }

    private static final class FontControlActiveValue implements ActiveValue {

        private final ActiveValue base;
        private final String fontFamily;
        private final int fontStyleMask;
        private final double sizeRate;

        FontControlActiveValue(ActiveValue base,
                               String fontFamily,
                               int fontStyleMask,
                               double sizeRate) {
            this.base = base;
            this.fontFamily = fontFamily;
            this.fontStyleMask = fontStyleMask;
            this.sizeRate = sizeRate;
        }

        @Override
        public Object createValue(UIDefaults table) {
            Object o = base.createValue(table);
            if (o instanceof Font) {
                Font font = (Font)o;
                final int style = font.getStyle() & fontStyleMask;
                final int size = (int)(font.getSize() * sizeRate);
                return new FontUIResource(fontFamily, style, size);
            }
            return o;
        }

    }

}
