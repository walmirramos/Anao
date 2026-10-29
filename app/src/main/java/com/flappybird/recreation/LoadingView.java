package com.flappybird.recreation;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Clean branded loading, visible until the actual game resources are ready. */
final class LoadingView extends View {
    private final Paint pixels=new Paint(), text=WorldArt.textPaint(14);
    private boolean failed;

    LoadingView(Context context) {
        super(context);
        BrandArt.load(getResources());
        pixels.setFilterBitmap(false);
        setContentDescription("Anão de Firma. Linha de montagem. Carregando o jogo.");
    }
    void showError() {
        failed=true;
        setContentDescription("Não foi possível carregar. Toque para tentar novamente.");
        invalidate();
    }
    @Override protected void onDraw(Canvas c) {
        c.drawColor(0xff0c1629);
        if(getWidth()==0 || getHeight()==0)return;
        float s=Math.min(getWidth()/288f,getHeight()/512f);
        c.save();c.translate((getWidth()-288*s)/2,(getHeight()-512*s)/2);c.scale(s,s);
        BrandArt.logo(c,68,pixels);
        text.setColor(0xd0121c2e);c.drawRect(22,306,266,369,text);
        text.setTextSize(failed ? 12 : 17);text.setColor(0xffffe7a2);
        c.drawText(failed ? "TOQUE PARA TENTAR NOVAMENTE" : "CARREGANDO...",144,335,text);
        text.setColor(0xff354358);c.drawRect(43,350,245,357,text);
        if(!failed) {
            int shift=(int)(SystemClock.uptimeMillis()/70%20);
            text.setColor(0xffffd16b);
            c.save();c.clipRect(43,350,245,357);
            for(int x=23+shift;x<249;x+=26)c.drawRect(x,350,x+15,357,text);
            c.restore();
        }
        text.setTextSize(11);text.setColor(0xffe8d6aa);c.drawText("DUBBOX 2026",144,501,text);
        c.restore();
        if(!failed && isAttachedToWindow())postInvalidateDelayed(50);
    }
}
