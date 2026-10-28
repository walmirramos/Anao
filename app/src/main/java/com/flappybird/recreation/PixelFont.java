package com.flappybird.recreation;

import android.content.Context;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

final class PixelFont {
    private static volatile Typeface bold;
    private PixelFont() {}
    static synchronized void init(Context context) {
        if(bold==null) bold=ResourcesCompat.getFont(context,R.font.flappy_bold);
    }
    static Typeface current() {
        return bold!=null ? bold : Typeface.create("monospace",Typeface.BOLD);
    }
}
