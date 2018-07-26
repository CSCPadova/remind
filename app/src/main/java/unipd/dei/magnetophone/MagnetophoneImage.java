package unipd.dei.magnetophone;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.widget.ImageView;

public class MagnetophoneImage
{
	//Variabili per i valori di default di width e height dell'imageView
	protected static final int DEFAULT_WIDTH = 200;
	protected static final int DEFAULT_HEIGHT = 200;
	
	/**
	 * Metodo che si occupa di caricare un immagine all'interno di una imageView
	 * @param picturePath
	 * @param imageView
	 */
	public static void scaleImage(String picturePath, ImageView imageView)
	{
		//Cerco di rimanere dentro i limiti della imageView
		Options opt = new Options();
		opt.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(picturePath, opt);

		int width = imageView.getWidth() <= 0 ? DEFAULT_WIDTH : imageView.getWidth();
		int height = imageView.getHeight() <= 0 ? DEFAULT_HEIGHT : imageView.getHeight();

		int b_width = opt.outWidth;
		int b_height = opt.outHeight;

		int scale_x = b_width / width;
		int scale_y = b_height / height;

		opt.inSampleSize = Math.min(scale_x, scale_y);

		opt.inJustDecodeBounds = false;

		File temp = new File(picturePath);
		
		if(temp.exists())	//Se il file esiste
		{
			//Scalo l'immagine adatta alla image view
			Bitmap bmp = BitmapFactory.decodeFile(picturePath, opt);
			imageView.setImageBitmap(bmp);
		}
		else	// altrimenti immagine di default
			imageView.setImageResource(R.drawable.defaultimage);
	}
}
