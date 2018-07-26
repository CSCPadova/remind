package unipd.dei.magnetophone;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Preference per visualizzare un'immagine, usata in particolare per visualizzare l'immagine di una song
 *
 */
public class ImageViewPreference extends Preference 
{
	private ImageView imageView;
	private String picturePath; 
	
	public ImageViewPreference(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		this.setWidgetLayoutResource(R.layout.image_layout);
	}

	/**
	 * Metodo chiamato ogni volta che viene creata la imageView (Anche solo per ricrearla! esempio:
	 * se viene coperta da qualcosa e poi riappare, questo metodo viene richiamato)
	 */
	@Override
	protected void onBindView(View view) 
	{
		super.onBindView(view);
		imageView = (ImageView) view.findViewById(R.id.imageView1);
		imageView.setFocusable(false);
		imageView.setAdjustViewBounds(true);
		imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		
		if(picturePath != null)
			setImage(picturePath);
	}
	
	/**
	 * Metodo per cambiare immagine alla preference
	 * @param path
	 */
	public void setImage(String path)
	{
		picturePath = path;
		MagnetophoneImage.scaleImage(path, imageView);
	}
	
	/**
	 * Metodo per settare la path della immagine da visualizzare
	 * @param path
	 */
	public void setPicturePath(String path)
	{
		picturePath = path;
	}
	
	/**
	 * Definisco la view e la sua grandezza
	 */
	@Override
	public View getView(final View convertView, final ViewGroup parent)
	{
		final View v = super.getView(convertView, parent);
		final int width = LayoutParams.MATCH_PARENT;
		final int height = MagnetophoneImage.DEFAULT_HEIGHT;
		final LayoutParams params = new LayoutParams(width,height);
		v.setLayoutParams(params);
		return v;
	}
}