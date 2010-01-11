package org.alliance.ui;

import java.io.IOException;
import org.jvnet.substance.api.*;
import org.jvnet.substance.colorscheme.BlendBiColorScheme;
import org.jvnet.substance.colorscheme.LightGrayColorScheme;
import org.jvnet.substance.colorscheme.MetallicColorScheme;
import org.jvnet.substance.colorscheme.OrangeColorScheme;
import org.jvnet.substance.colorscheme.RaspberryColorScheme;
import org.jvnet.substance.colorscheme.SunGlareColorScheme;
import org.jvnet.substance.colorscheme.SunsetColorScheme;
import org.jvnet.substance.painter.border.ClassicBorderPainter;
import org.jvnet.substance.painter.decoration.ArcDecorationPainter;
import org.jvnet.substance.painter.decoration.BrushedMetalDecorationPainter;
import org.jvnet.substance.painter.decoration.DecorationAreaType;
import org.jvnet.substance.painter.gradient.ClassicGradientPainter;
import org.jvnet.substance.painter.highlight.ClassicHighlightPainter;
import org.jvnet.substance.shaper.ClassicButtonShaper;

public class MySkin extends SubstanceSkin {

    private static String NAME = "Alliance";

    public MySkin(UISubsystem ui) {
        SubstanceSkin.ColorSchemes shemes = null;
        try {
            shemes = SubstanceSkin.getColorSchemes(ui.getRl().getResource("gfx/seaglass.colorschemes"));
        } catch (IOException ex) {
        }
        SubstanceColorScheme activeScheme = new MetallicColorScheme().tint(0.45);
        SubstanceColorScheme defaultScheme = new MetallicColorScheme().tint(0.05);
        SubstanceColorScheme disabledScheme = new LightGrayColorScheme().tint(0.05);

        SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
                activeScheme, defaultScheme, disabledScheme);      
        this.registerDecorationAreaSchemeBundle(defaultSchemeBundle,
                DecorationAreaType.NONE);
                
        SubstanceColorScheme activeHeaderScheme = shemes.get("Alliance");
        SubstanceColorScheme defaultHeaderScheme = shemes.get("Alliance");
        SubstanceColorSchemeBundle headerSchemeBundle = new SubstanceColorSchemeBundle(
                activeHeaderScheme, defaultHeaderScheme, defaultHeaderScheme);

        this.registerDecorationAreaSchemeBundle(headerSchemeBundle,
                DecorationAreaType.PRIMARY_TITLE_PANE,
                DecorationAreaType.SECONDARY_TITLE_PANE,
                DecorationAreaType.HEADER, DecorationAreaType.TOOLBAR);

     defaultSchemeBundle.registerColorScheme(new BlendBiColorScheme(
        new OrangeColorScheme(), new SunGlareColorScheme()
            .saturate(-0.1), 0.4),
        ComponentState.DEFAULT);
    defaultSchemeBundle.registerColorScheme(new BlendBiColorScheme(
        new OrangeColorScheme(), new SunsetColorScheme(), 0.3),
        ComponentState.ROLLOVER_SELECTED);
    defaultSchemeBundle.registerColorScheme(new BlendBiColorScheme(
        new RaspberryColorScheme(), new SunsetColorScheme(), 0.6)
        .saturate(0.2), ComponentState.PRESSED_SELECTED);
    defaultSchemeBundle.registerColorScheme(new BlendBiColorScheme(
        new RaspberryColorScheme(), new SunsetColorScheme(), 0.2),
        ComponentState.PRESSED_UNSELECTED);

        this.buttonShaper = new ClassicButtonShaper();
        this.gradientPainter = new ClassicGradientPainter();
        this.borderPainter = new ClassicBorderPainter();

        BrushedMetalDecorationPainter decorationPainter = new BrushedMetalDecorationPainter();
        decorationPainter.setBaseDecorationPainter(new ArcDecorationPainter());
        decorationPainter.setTextureAlpha(0.3f);
        this.decorationPainter = decorationPainter;

        this.highlightPainter = new ClassicHighlightPainter();
        this.borderPainter = new ClassicBorderPainter();
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }
}
