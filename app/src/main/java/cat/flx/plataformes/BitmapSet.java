package cat.flx.plataformes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class BitmapSet {

    private SparseArray<Bitmap> bitmaps;       // all the bitmaps will be held here

   Bitmap getBitmap(int index) { return bitmaps.get(index); }

    BitmapSet(Context context) {
        bitmaps = new SparseArray<>();
        loadBitmaps(context, R.raw.bonk, R.raw.bonkinfo);
        loadBitmaps(context, R.raw.heart_sprite, R.raw.heart_info);
    }

    private void loadBitmaps(Context context, int pngResource, int txtResource) {
        // Load the sprites and tiles from res/raw/bonk.png
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        Bitmap bitmapsBMP = BitmapFactory.decodeResource(context.getResources(), pngResource, opts);
        // Prepping the transformations for image rotation
        Matrix rot1 = new Matrix();     // no-rotation
        Matrix rot2 = new Matrix();
        rot2.setScale(-1, 1);    // flip horizontal

        // Load the sprite's and tile's definition file
        InputStream in = context.getResources().openRawResource(txtResource);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        // bitMapIds(context, bitmapsBMP, in, R.raw.bonk, R.raw.bonkinfo);
        try {
            // Search the maximum ID from the file. Needed to know the size of the array
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String parts[] = line.split(":");
                if (parts.length != 7) continue;
                int id = Integer.parseInt(parts[0]);
                count = (id > count) ? id : count;
            }
            // Reset the stream to re-read the file
            in.reset();
            while ((line = reader.readLine()) != null) {
                String parts[] = line.split(":");
                if (parts.length != 7) continue;    // empty lines are skipped
                int id = Integer.parseInt(parts[0]);
                int x =  Integer.parseInt(parts[1]);
                int y =  Integer.parseInt(parts[2]);
                int w =  Integer.parseInt(parts[3]);
                int h =  Integer.parseInt(parts[4]);
                int r =  Integer.parseInt(parts[5]);
                Matrix m = (r == 1) ? rot2 : rot1;
                // Get the portion of the original Bitmap and store it in the array
                Bitmap bitmap = Bitmap.createBitmap(bitmapsBMP, x, y, w, h, m, true);
                bitmaps.put(id, bitmap);
            }
            reader.close();
        }
        catch (Exception ignored) { }
        // Release the resources of the original Bitmap. It's needed no more in the app
        bitmapsBMP.recycle();
    }
}
