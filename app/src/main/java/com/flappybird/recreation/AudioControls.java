package com.flappybird.recreation;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.google.android.material.materialswitch.MaterialSwitch;

final class AudioControls {
    private final MaterialSwitch music, siren;
    private final SeekBar musicVolume, sirenVolume;
    private boolean loading;

    AudioControls(Activity activity, LinearLayout parent, Runnable changed) {
        music = toggle(activity, parent, "Música de fundo 8-bit");
        musicVolume = volume(activity, parent, "Volume da música", changed);
        siren = toggle(activity, parent, "Sirene de polícia 8-bit");
        sirenVolume = volume(activity, parent, "Volume da sirene", changed);
        music.setOnCheckedChangeListener((view, checked) -> { if (!loading) changed.run(); });
        siren.setOnCheckedChangeListener((view, checked) -> { if (!loading) changed.run(); });
    }

    private MaterialSwitch toggle(Activity activity, LinearLayout parent, String title) {
        MaterialSwitch view = new MaterialSwitch(activity);
        view.setTypeface(PixelFont.current());
        view.setText(title); view.setTextColor(Color.WHITE); view.setTextSize(16);
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private SeekBar volume(Activity activity, LinearLayout parent, String title, Runnable changed) {
        TextView label = new TextView(activity); label.setTextColor(Color.LTGRAY);label.setTypeface(PixelFont.current());
        label.setText(title + ": 0%");
        parent.addView(label);
        SeekBar bar = new SeekBar(activity); bar.setMax(100); bar.setContentDescription(title);
        parent.addView(bar, new LinearLayout.LayoutParams(-1, (int)(48 * activity.getResources().getDisplayMetrics().density)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int value, boolean user) { label.setText(title + ": " + value + "%"); }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) { if (!loading) changed.run(); }
        });
        return bar;
    }

    void load(SharedPreferences source) {
        loading = true;
        music.setChecked(source.getBoolean(RetroAudio.MUSIC, true));
        siren.setChecked(source.getBoolean(RetroAudio.SIREN, true));
        musicVolume.setProgress(source.getInt(RetroAudio.MUSIC_VOLUME, 35));
        sirenVolume.setProgress(source.getInt(RetroAudio.SIREN_VOLUME, 18));
        loading = false;
    }

    void save(SharedPreferences.Editor editor) {
        editor.putBoolean(RetroAudio.MUSIC, music.isChecked());
        editor.putBoolean(RetroAudio.SIREN, siren.isChecked());
        editor.putInt(RetroAudio.MUSIC_VOLUME, musicVolume.getProgress());
        editor.putInt(RetroAudio.SIREN_VOLUME, sirenVolume.getProgress());
    }
}
