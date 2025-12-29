package com.example.mirror;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.*;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Surface;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ScreenService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startCapture(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    private void startCapture(Intent intent) throws Exception {
        int code = intent.getIntExtra("code", -1);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        MediaProjection projection = mpm.getMediaProjection(code, data);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int dpi = dm.densityDpi;

        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        MediaCodec codec = MediaCodec.createEncoderByType(
                MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = codec.createInputSurface();
        codec.start();

        VirtualDisplay vd = projection.createVirtualDisplay(
                "mirror",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null
        );

        OutputStream out = System.out;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (true) {
            int idx = codec.dequeueOutputBuffer(info, 10_000);
            if (idx >= 0) {
                ByteBuffer buf = codec.getOutputBuffer(idx);
                byte[] dataBuf = new byte[info.size];
                buf.get(dataBuf);
                out.write(dataBuf);
                out.flush();
                codec.releaseOutputBuffer(idx, false);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
