package stew5.ui.swing;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_MASK;

import java.awt.*;
import java.util.concurrent.*;

import javax.swing.*;

final class Utilities {

    private Utilities() {
    } // forbidden

    static ImageIcon getImageIcon(String name) {
        try {
            return new ImageIcon(Utilities.class.getResource("icon/" + name));
        } catch (RuntimeException ex) {
            return new ImageIcon();
        }
    }

    static KeyStroke getKeyStroke(String s) {
        KeyStroke ks = KeyStroke.getKeyStroke(s);
        if (ks != null && s.matches("(?i).*Ctrl.*")) {
            return convertShortcutMask(ks, getMenuShortcutKeyMask());
        }
        return ks;
    }

    static KeyStroke convertShortcutMask(KeyStroke ks, int shortcutMask) {
        final int mod = ks.getModifiers();
        if ((mod & (CTRL_DOWN_MASK | CTRL_MASK)) != 0) {
            final int newmod = mod & ~(CTRL_DOWN_MASK | CTRL_MASK) | shortcutMask;
            return KeyStroke.getKeyStroke(ks.getKeyCode(), newmod);
        }
        return ks;
    }

    static int getMenuShortcutKeyMask() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

    static void sleep(long interval) {
        try {
            TimeUnit.MILLISECONDS.sleep(interval);
        } catch (InterruptedException ex) {
            // expects no interruption
            assert false : ex.toString();
        }
    }

}
