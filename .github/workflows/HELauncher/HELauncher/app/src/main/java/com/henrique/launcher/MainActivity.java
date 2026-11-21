package com.henrique.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * HE Launcher - tela inicial do projetor do Eng. Henrique Elias.
 *
 * Topo esquerdo: hora, data e boas-vindas.
 * Topo direito: Wi-Fi e Bluetooth (abrem as configurações).
 * Fileira 1: apps favoritos.
 * Fileira 2: ajustes (brilho, volume, imagem, trapézio, configurações, todos os apps).
 */
public class MainActivity extends Activity {

    private static final int ACCENT = 0xFFE62D28;
    private static final int TILE_BG = 0xCC1C1C22;

    /** Apps da primeira fileira, na ordem em que aparecem. Os que não estiverem instalados são pulados. */
    private static final String[] FAVORITOS = {
            "com.netflix.mediaclient",
            "com.google.android.youtube.tv",
            "com.amazon.avod.thirdpartyclient",
            "com.android.chrome",
            "com.esaba.downloader",
            "com.android.vending"
    };

    private static final int ON_GREEN = 0xFF5CDB7A;
    private static final int ON_BLUE = 0xFF4F9BFF;
    private static final int OFF_GREY = 0xFF9A9A9A;

    private TextView wifiChip;
    private TextView btChip;
    private Drawable wifiIcon;
    private Drawable btIcon;
    private LinearLayout favRow;
    private boolean firstFocusDone = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statusLoop = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 10_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillFavorites();
        handler.removeCallbacks(statusLoop);
        handler.post(statusLoop);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(statusLoop);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // É a tela inicial: o botão voltar não faz nada aqui.
    }

    // ------------------------------------------------------------------ montagem da tela

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundResource(R.drawable.fundo);
        root.setClipChildren(false);

        // ---- topo esquerdo: hora, data e boas-vindas
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        TextClock hora = new TextClock(this);
        hora.setFormat24Hour("HH:mm");
        hora.setFormat12Hour("HH:mm");
        style(hora, 42, true, Color.WHITE);

        TextClock data = new TextClock(this);
        data.setFormat24Hour("EEEE, d 'de' MMMM 'de' yyyy");
        data.setFormat12Hour("EEEE, d 'de' MMMM 'de' yyyy");
        style(data, 15, false, 0xFFE0E0E0);

        TextView bemVindo = new TextView(this);
        bemVindo.setText("BEM-VINDO,");
        style(bemVindo, 13, false, 0xFFF0F0F0);
        bemVindo.setLetterSpacing(0.15f);

        TextView nome = new TextView(this);
        nome.setText("Eng. Henrique Elias");
        style(nome, 30, true, Color.WHITE);

        View traco = new View(this);
        traco.setBackgroundColor(ACCENT);

        left.addView(hora);
        left.addView(data);
        LinearLayout.LayoutParams bemParams = wrap();
        bemParams.topMargin = dp(18);
        left.addView(bemVindo, bemParams);
        left.addView(nome);
        LinearLayout.LayoutParams tracoParams = new LinearLayout.LayoutParams(dp(56), dp(3));
        tracoParams.topMargin = dp(6);
        left.addView(traco, tracoParams);

        FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        leftParams.leftMargin = dp(40);
        leftParams.topMargin = dp(22);
        root.addView(left, leftParams);

        // ---- topo direito: Wi-Fi e Bluetooth
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setClipChildren(false);
        wifiIcon = icon(R.drawable.ic_wifi, 16);
        btIcon = icon(R.drawable.ic_bluetooth, 16);
        wifiChip = chip(wifiIcon, v -> openSettings(Settings.ACTION_WIFI_SETTINGS));
        btChip = chip(btIcon, v -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS));
        LinearLayout.LayoutParams chipParams = wrap();
        chipParams.leftMargin = dp(10);
        right.addView(wifiChip, chipParams);
        right.addView(btChip, chipParams);

        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        rightParams.rightMargin = dp(40);
        rightParams.topMargin = dp(26);
        root.addView(right, rightParams);

        // ---- fileiras embaixo
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setClipChildren(false);

        bottom.addView(label("Favoritos"));
        favRow = row();
        bottom.addView(scroller(favRow));

        bottom.addView(label("Ajustes"));
        LinearLayout adjRow = row();
        adjRow.addView(settingTile(R.drawable.ic_brilho, "Brilho", v -> brightnessDialog()));
        adjRow.addView(settingTile(R.drawable.ic_volume, "Volume", v -> volumeDialog()));
        adjRow.addView(settingTile(R.drawable.ic_imagem, "Imagem", v -> openPicture()));
        adjRow.addView(settingTile(R.drawable.ic_trapezio, "Trapézio", v -> launchPackage("com.softwinner.tcorrection")));
        adjRow.addView(settingTile(R.drawable.ic_config, "Configurações", v -> openSettings(Settings.ACTION_SETTINGS)));
        adjRow.addView(settingTile(R.drawable.ic_apps, "Todos os apps", v -> allAppsDialog()));
        bottom.addView(scroller(adjRow));

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        bottomParams.leftMargin = dp(34);
        bottomParams.bottomMargin = dp(16);
        root.addView(bottom, bottomParams);

        return root;
    }

    private void fillFavorites() {
        favRow.removeAllViews();
        for (String pkg : FAVORITOS) {
            View tile = appTile(pkg);
            if (tile != null) favRow.addView(tile);
        }
        if (!firstFocusDone && favRow.getChildCount() > 0) {
            firstFocusDone = true;
            favRow.post(() -> favRow.getChildAt(0).requestFocus());
        }
    }

    // ------------------------------------------------------------------ peças visuais

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setClipChildren(false);
        r.setClipToPadding(false);
        r.setPadding(dp(6), dp(8), dp(40), dp(10));
        return r;
    }

    private HorizontalScrollView scroller(LinearLayout content) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false);
        hs.setClipToPadding(false);
        hs.addView(content);
        return hs;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        style(t, 14, true, 0xFFF2F2F2);
        t.setPadding(dp(6), dp(4), 0, 0);
        return t;
    }

    private FrameLayout baseTile(int widthDp, int heightDp) {
        FrameLayout tile = new FrameLayout(this);
        tile.setFocusable(true);
        tile.setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(TILE_BG);
        bg.setStroke(dp(2), Color.TRANSPARENT);
        tile.setBackground(bg);
        tile.setClipToOutline(true);
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            bg.setStroke(dp(2), hasFocus ? Color.WHITE : Color.TRANSPARENT);
            float s = hasFocus ? 1.08f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(120).start();
            v.setElevation(hasFocus ? dp(8) : 0);
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp));
        p.rightMargin = dp(14);
        tile.setLayoutParams(p);
        return tile;
    }

    private View appTile(String pkg) {
        PackageManager pm = getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        if (!ai.enabled) return null;
        Intent launch = launchIntent(pkg);
        if (launch == null) return null;

        FrameLayout tile = baseTile(160, 90);

        Drawable banner = null;
        try {
            if (launch.getComponent() != null) banner = pm.getActivityBanner(launch.getComponent());
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (banner == null) banner = pm.getApplicationBanner(ai);

        ImageView iv = new ImageView(this);
        if (banner != null) {
            iv.setImageDrawable(banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            tile.addView(iv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            iv.setImageDrawable(pm.getApplicationIcon(ai));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(44), dp(44),
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            ip.topMargin = dp(12);
            tile.addView(iv, ip);

            TextView name = new TextView(this);
            name.setText(pm.getApplicationLabel(ai));
            style(name, 12, false, Color.WHITE);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);
            tp.bottomMargin = dp(8);
            tile.addView(name, tp);
        }
        tile.setOnClickListener(v -> safeStart(launch));
        return tile;
    }

    private View settingTile(int iconRes, String text, View.OnClickListener onClick) {
        FrameLayout tile = baseTile(118, 72);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);

        ImageView ic = new ImageView(this);
        ic.setImageResource(iconRes);
        ic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams icParams = new LinearLayout.LayoutParams(dp(26), dp(26));
        icParams.gravity = Gravity.CENTER_HORIZONTAL;

        TextView tx = new TextView(this);
        tx.setText(text);
        style(tx, 12, false, 0xFFEDEDED);
        tx.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams txParams = wrap();
        txParams.topMargin = dp(6);
        txParams.gravity = Gravity.CENTER_HORIZONTAL;

        col.addView(ic, icParams);
        col.addView(tx, txParams);
        tile.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tile.setOnClickListener(onClick);
        return tile;
    }

    private Drawable icon(int res, int sizeDp) {
        Drawable d = getDrawable(res).mutate();
        d.setBounds(0, 0, dp(sizeDp), dp(sizeDp));
        return d;
    }

    private TextView chip(Drawable leftIcon, View.OnClickListener onClick) {
        TextView c = new TextView(this);
        style(c, 13, false, Color.WHITE);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setCompoundDrawablesRelative(leftIcon, null, null, null);
        c.setCompoundDrawablePadding(dp(8));
        c.setPadding(dp(14), dp(7), dp(14), dp(7));
        c.setFocusable(true);
        c.setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0x99000000);
        bg.setStroke(dp(2), Color.TRANSPARENT);
        c.setBackground(bg);
        c.setOnFocusChangeListener((v, hasFocus) ->
                bg.setStroke(dp(2), hasFocus ? Color.WHITE : Color.TRANSPARENT));
        c.setOnClickListener(onClick);
        return c;
    }

    private void style(TextView t, float sp, boolean bold, int color) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setShadowLayer(dp(4), 0, dp(1), 0xAA000000);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    // ------------------------------------------------------------------ Wi-Fi e Bluetooth

    private void refreshStatus() {
        boolean wifiOn = false;
        boolean wifiConnected = false;
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiOn = wm != null && wm.isWifiEnabled();
            ConnectivityManager cm = getSystemService(ConnectivityManager.class);
            Network n = cm != null ? cm.getActiveNetwork() : null;
            NetworkCapabilities nc = n != null ? cm.getNetworkCapabilities(n) : null;
            wifiConnected = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception ignored) {
        }
        String wifiState = wifiConnected ? "conectado" : (wifiOn ? "ligado" : "desligado");
        wifiChip.setText("Wi-Fi " + wifiState);
        wifiIcon.setTint(wifiConnected ? ON_GREEN : (wifiOn ? Color.WHITE : OFF_GREY));

        boolean btOn = false;
        try {
            @SuppressWarnings("deprecation")
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            btOn = adapter != null && adapter.isEnabled();
        } catch (Exception ignored) {
        }
        btChip.setText("Bluetooth " + (btOn ? "ligado" : "desligado"));
        btIcon.setTint(btOn ? ON_BLUE : OFF_GREY);
    }

    // ------------------------------------------------------------------ ações

    private void safeStart(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Não foi possível abrir.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings(String action) {
        try {
            startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException e) {
            safeStart(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private Intent launchIntent(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.setPackage(pkg);
            List<ResolveInfo> found = pm.queryIntentActivities(q, 0);
            if (!found.isEmpty()) {
                i = new Intent(Intent.ACTION_MAIN);
                i.setClassName(pkg, found.get(0).activityInfo.name);
            }
        }
        return i;
    }

    private void launchPackage(String pkg) {
        Intent i = launchIntent(pkg);
        if (i != null) safeStart(i);
        else Toast.makeText(this, "App não encontrado: " + pkg, Toast.LENGTH_SHORT).show();
    }

    /** Menu de imagem: tenta o de tela do Android e, se não existir, as configurações gerais. */
    private void openPicture() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        } catch (ActivityNotFoundException ignored) {
        }
        Intent droid = launchIntent("com.droidlogic.tv.settings");
        if (droid != null) {
            safeStart(droid);
        } else {
            openSettings(Settings.ACTION_SETTINGS);
        }
    }

    private void brightnessDialog() {
        ContentResolver cr = getContentResolver();
        sliderDialog("Brilho", 255,
                () -> Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128),
                value -> {
                    if (Settings.System.canWrite(this)) {
                        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, Math.max(10, value));
                    } else {
                        Toast.makeText(this, "Falta liberar a permissão de brilho (comando appops).", Toast.LENGTH_LONG).show();
                        try {
                            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        } catch (ActivityNotFoundException ignored) {
                        }
                    }
                });
    }

    private void volumeDialog() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        sliderDialog("Volume", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                () -> am.getStreamVolume(AudioManager.STREAM_MUSIC),
                value -> am.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0));
    }

    /** Janela com uma barra: setas esquerda/direita do controle ajustam o valor. */
    private void sliderDialog(String title, int max, IntSupplier getter, IntConsumer setter) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextView value = new TextView(this);
        style(value, 18, true, Color.WHITE);
        value.setGravity(Gravity.CENTER);

        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        int current = Math.min(max, Math.max(0, getter.getAsInt()));
        bar.setProgress(current);
        value.setText(percent(current, max));
        bar.setKeyProgressIncrement(Math.max(1, max / 20));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                value.setText(percent(progress, max));
                if (fromUser) setter.accept(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        box.addView(value);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(dp(360), ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.topMargin = dp(12);
        box.addView(bar, barParams);

        TextView hint = new TextView(this);
        hint.setText("Use as setas ◀ ▶ do controle");
        style(hint, 12, false, 0xFFBBBBBB);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = wrap();
        hintParams.topMargin = dp(8);
        hintParams.gravity = Gravity.CENTER_HORIZONTAL;
        box.addView(hint, hintParams);

        AlertDialog d = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("OK", null)
                .create();
        d.setOnShowListener(x -> bar.requestFocus());
        d.show();
    }

    private String percent(int v, int max) {
        return max <= 0 ? "0%" : Math.round(v * 100f / max) + "%";
    }

    /** Lista de todos os apps instalados que podem ser abertos. */
    private void allAppsDialog() {
        PackageManager pm = getPackageManager();
        List<String> labels = new ArrayList<>();
        List<Intent> intents = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());

        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        List<ResolveInfo> all = new ArrayList<>();
        for (String cat : categories) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(cat);
            all.addAll(pm.queryIntentActivities(q, 0));
        }
        all.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        for (ResolveInfo ri : all) {
            String pkg = ri.activityInfo.packageName;
            if (!seen.add(pkg)) continue;
            Intent i = launchIntent(pkg);
            if (i == null) continue;
            labels.add(ri.loadLabel(pm).toString());
            intents.add(i);
        }

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Todos os apps")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> safeStart(intents.get(which)))
                .show();
    }
}
