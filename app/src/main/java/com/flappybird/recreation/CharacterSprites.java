package com.flappybird.recreation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ColorMatrixColorFilter;

/** Uses the supplied artwork unchanged, trimming transparent padding at load time. */
final class CharacterSprites {
    private CharacterSprites() {}

    static Bitmap[] loadVariants(Resources resources) {
        Bitmap original = load(resources);
        Bitmap[] variants = {original, null, null};
        // Runtime palette swaps preserve alpha, dark outlines and white highlights.
        float[][] palettes = {
            {0,0,1,0,0, 0,1,0,0,0, 1,0,0,0,0, 0,0,0,1,0},
            {1,0,0,0,0, 0,0,1,0,0, 0,1,0,0,0, 0,0,0,1,0}
        };
        for (int i = 1; i < variants.length; i++) {
            variants[i] = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(palettes[i - 1]));
            new Canvas(variants[i]).drawBitmap(original, 0, 0, paint);
        }
        return variants;
    }

    static Bitmap load(Resources resources) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap source = BitmapFactory.decodeResource(resources, R.drawable.nengue_character, options);
        int left = source.getWidth(), top = source.getHeight(), right = -1, bottom = -1;
        int[] row = new int[source.getWidth()];
        for (int y = 0; y < source.getHeight(); y++) {
            source.getPixels(row, 0, row.length, 0, y, row.length, 1);
            for (int x = 0; x < row.length; x++) {
                if (Color.alpha(row[x]) > 128) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left) throw new IllegalStateException("Character artwork is empty");
        Bitmap cropped = Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1);
        // Match the original horizontal footprint without stretching the face.
        Bitmap sprite = Bitmap.createScaledBitmap(cropped, 34,
                Math.max(1, Math.round(34f * cropped.getHeight() / cropped.getWidth())), false);
        if (cropped != source && cropped != sprite) cropped.recycle();
        if (source != sprite) source.recycle();
        return sprite;
    }
}
