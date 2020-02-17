package eu.wallhack.domkirkear.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Session;

import java.io.IOException;
import java.io.InputStream;


public class imageTracking {

    public static AugmentedImageDatabase createImageDatabase(Context context, Session session) {
        AugmentedImageDatabase imageDatabase = new AugmentedImageDatabase(session);
        Bitmap bitmap;
        try (InputStream inputStream = context.getAssets().open("images/qrCode.png")) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int index = imageDatabase.addImage("qrCode", bitmap, 0.089f);
        return imageDatabase;
    }

    public static AugmentedImageDatabase loadImageDatabase(Context context, Session session) {
        AugmentedImageDatabase imageDatabase;
        try {
            InputStream inputStream = context.getAssets().open("images/images.imgdb");
            imageDatabase = AugmentedImageDatabase.deserialize(session, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return imageDatabase;
    }
}
