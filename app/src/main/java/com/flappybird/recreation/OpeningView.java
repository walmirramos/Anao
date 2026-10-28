package com.flappybird.recreation;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** Shared logo and city/viatura artwork with a single clear call to action. */
public final class OpeningView extends View {
    private final Bitmap character;
    private final Paint pixels=new Paint(), text=WorldArt.textPaint(14);
    public OpeningView(Context context, AttributeSet attributes) {
        super(context,attributes);
        BrandArt.load(getResources());character=CharacterSprites.load(getResources());
        pixels.setFilterBitmap(false);setFocusable(true);
        setContentDescription("Corre Nengue. A polícia vem aí. Dubbox 2026. Toque para correr.");
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if(getWidth()==0 || getHeight()==0)return;
        c.drawColor(0xff0c1629);
        float s=Math.min(getWidth()/288f,getHeight()/512f);
        c.save();c.translate((getWidth()-288*s)/2,(getHeight()-512*s)/2);c.scale(s,s);
        BrandArt.background(c,pixels);BrandArt.logo(c,43,pixels);
        c.drawBitmap(character,null,new Rect(105,188,187,264),pixels);
        text.setColor(0xe0121c2e);c.drawRect(22,421,266,481,text);
        text.setColor(0xffffd16b);c.drawRect(22,421,266,424,text);
        text.setTextSize(19);text.setColor(0xffffe7a2);c.drawText("TOQUE PARA CORRER",144,449,text);
        text.setTextSize(11);text.setColor(0xffb9c6d5);c.drawText("A POLÍCIA VEM AÍ",144,468,text);
        text.setColor(0xffe8d6aa);c.drawText("DUBBOX 2026",144,501,text);
        c.restore();
    }
}
