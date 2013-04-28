package com.bignis.prestocookbook;
import android.content.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;


public class ImageAdapter extends BaseAdapter implements SpinnerAdapter {

	private Context _context;
	
	public Integer[] ImageIds =
		{
			R.drawable.curry,
			R.drawable.fruit,
			R.drawable.salmon,
			R.drawable.soulfood,
			R.drawable.spagetti
		};
	
	public ImageAdapter(Context context)
	{
		this._context = context;
	}
	
	@Override
	public int getCount() {
		return this.ImageIds.length;
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup arg2) {
		ImageView imageView = null;
		
		if (convertView == null)
		{
			imageView = new ImageView(this._context);
			imageView.setLayoutParams(new Gallery.LayoutParams(115, 100));
		}
		else
		{
			imageView = (ImageView)convertView;
		}
		
		imageView.setImageResource(ImageIds[position]);
		
		return imageView;
	}

}
