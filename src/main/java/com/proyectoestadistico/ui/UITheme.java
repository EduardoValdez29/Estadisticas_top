package com.proyectoestadistico.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class UITheme {

    public final Color bg0 = new Color(0x0B1020);
    public final Color bg1 = new Color(0x0F1630);
    public final Color bg2 = new Color(0x141F3D);
    public final Color border = new Color(0x23345A);

    public final Color text = new Color(0xE6F1FF);
    public final Color muted = new Color(0x8AA0C7);

    public final Color neonCyan = new Color(0x00E5FF);
    public final Color neonMagenta = new Color(0xFF2D95);
    public final Color neonGreen = new Color(0x39FF88);
    public final Color neonPurple = new Color(0x7C4DFF);

    public final Color danger = new Color(0xFF4D4D);

    private UITheme() {
    }

    public static UITheme apply() {
        UITheme t = new UITheme();

        UIManager.put("Panel.background", t.bg0);
        UIManager.put("Label.foreground", t.text);
        Font base = UIManager.getFont("Label.font");
        if (base != null) {
            UIManager.put("Label.font", base.deriveFont(Font.PLAIN, 12f));
            UIManager.put("Button.font", base.deriveFont(Font.BOLD, 12f));
            UIManager.put("TabbedPane.font", base.deriveFont(Font.BOLD, 12f));
            UIManager.put("Table.font", base.deriveFont(Font.PLAIN, 12f));
            UIManager.put("TableHeader.font", base.deriveFont(Font.BOLD, 12f));
        }
        UIManager.put("TextField.background", t.bg1);
        UIManager.put("TextField.foreground", t.text);
        UIManager.put("TextField.caretForeground", t.text);

        UIManager.put("ScrollPane.background", t.bg0);
        UIManager.put("Viewport.background", t.bg0);

        UIManager.put("TabbedPane.background", t.bg0);
        UIManager.put("TabbedPane.foreground", t.text);
        UIManager.put("TabbedPane.selected", t.bg1);
        UIManager.put("TabbedPane.contentAreaColor", t.bg0);
        UIManager.put("TabbedPane.focus", t.bg0);
        UIManager.put("TabbedPane.borderHightlightColor", t.border);
        UIManager.put("TabbedPane.darkShadow", t.border.darker());
        UIManager.put("TabbedPane.shadow", t.border);
        UIManager.put("TabbedPane.light", t.border.brighter());

        UIManager.put("Table.background", t.bg1);
        UIManager.put("Table.foreground", t.text);
        UIManager.put("Table.selectionBackground", t.neonPurple.darker());
        UIManager.put("Table.selectionForeground", t.text);
        UIManager.put("Table.gridColor", t.border);

        UIManager.put("TableHeader.background", t.bg2);
        UIManager.put("TableHeader.foreground", t.text);

        UIManager.put("ToolTip.background", t.bg2);
        UIManager.put("ToolTip.foreground", t.text);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(t.border));
        UIManager.put("ToolTip.font", UIManager.getFont("Label.font"));

        Font dialogFont = UIManager.getFont("Label.font");
        if (dialogFont != null) {
            UIManager.put("OptionPane.messageFont", dialogFont.deriveFont(Font.PLAIN, 12f));
            UIManager.put("OptionPane.buttonFont", dialogFont.deriveFont(Font.BOLD, 12f));
        }
        UIManager.put("OptionPane.background", t.bg0);
        UIManager.put("OptionPane.messageForeground", t.text);
        UIManager.put("OptionPane.border", BorderFactory.createLineBorder(t.border));

        UIManager.put("Button.background", t.bg2);
        UIManager.put("Button.foreground", t.text);
        UIManager.put("Button.select", t.bg1);
        UIManager.put("Button.focus", t.border);
        UIManager.put("Button.border", t.roundedBorder(t.border));

        UIManager.put("Label.font", dialogFont);

        UIManager.put("OptionPane.informationIcon", new NeonCircleIcon(t.neonCyan, t.bg0));
        UIManager.put("OptionPane.questionIcon", new NeonCircleIcon(t.neonPurple, t.bg0));
        UIManager.put("OptionPane.errorIcon", new NeonCircleIcon(t.danger, t.bg0));
        UIManager.put("OptionPane.warningIcon", new NeonCircleIcon(t.neonMagenta, t.bg0));

        return t;
    }

    public Border roundedBorder(Color c) {
        return BorderFactory.createCompoundBorder(
                new RoundedLineBorder(c, 10, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        );
    }

    public Border roundedContainerBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundedLineBorder(border, 12, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        );
    }

    public Font titleFont(Component c) {
        return c.getFont().deriveFont(Font.BOLD, 18f);
    }

    public Font subtitleFont(Component c) {
        return c.getFont().deriveFont(Font.PLAIN, 11f);
    }

    static final class RoundedLineBorder implements Border {
        private final Color color;
        private final int arc;
        private final int thickness;

        RoundedLineBorder(Color color, int arc, int thickness) {
            this.color = color;
            this.arc = arc;
            this.thickness = thickness;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(thickness));
                int offs = thickness / 2;
                g2.drawRoundRect(x + offs, y + offs, width - thickness, height - thickness, arc, arc);
            } finally {
                g2.dispose();
            }
        }
    }

    static final class NeonCircleIcon implements Icon {
        private final Color color;
        private final Color background;
        private final int size = 24;

        NeonCircleIcon(Color color, Color background) {
            this.color = color;
            this.background = background;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(background);
                g2.fillRect(x, y, size, size);

                int rOuter = size - 4;
                int rInner = size - 10;
                int cx = x + size / 2;
                int cy = y + size / 2;

                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
                g2.fillOval(cx - rOuter / 2, cy - rOuter / 2, rOuter, rOuter);

                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - rInner / 2, cy - rInner / 2, rInner, rInner);

                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                FontMetrics fm = g2.getFontMetrics();
                String ch = "!";
                if (color.equals(background)) ch = "";
                if (color.getGreen() > color.getRed() && color.getGreen() > color.getBlue()) {
                    ch = "i";
                } else if (color.getBlue() > color.getRed() && color.getBlue() > color.getGreen()) {
                    ch = "?";
                } else if (color.equals(new Color(0xFF4D4D))) {
                    ch = "!";
                } else {
                    ch = "!";
                }
                int tx = cx - fm.stringWidth(ch) / 2;
                int ty = cy + fm.getAscent() / 2 - 2;
                g2.drawString(ch, tx, ty);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}

