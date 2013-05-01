package com.bignis.prestocookbook;
import android.content.*;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


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
		
		if (convertView != null)
		{
			// Never reuse, this might kill performance
			//return convertView;
		}
		
		LinearLayout layout = new LinearLayout(_context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		RecipeImage recipeImage = getRecipeImage(position);
		
		TextView textView = new TextView(this._context);
		textView.setText(recipeImage.title);
		layout.addView(textView);
		
		ImageView imageView = new ImageView(this._context);
		Drawable drawable = recipeImage.drawable;
		Gallery.LayoutParams params = new Gallery.LayoutParams(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
		imageView.setLayoutParams(params);
		
		imageView.setImageDrawable(drawable);
		
		layout.addView(imageView);
		
		return layout;
		
		/*
		LinearLayout layout = new LinearLayout(_context);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		ImageView imageView = null;
		
		if (convertView == null)
		{
			imageView = new ImageView(this._context);
			
			RecipeImage recipeImage = getRecipeImage(position);
			
			Drawable drawable = recipeImage.drawable;
			Gallery.LayoutParams params = new Gallery.LayoutParams(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			//imageView.setLayoutParams(new Gallery.LayoutParams(115, 100));
			imageView.setLayoutParams(params);
			
			imageView.setImageResource(ImageIds[position]);
		}
		else
		{
			imageView = (ImageView)convertView;
		}
		
		return imageView;
		*/
		
	}

	private RecipeImage getRecipeImage(int position)
	{
		RecipeImage ri = new RecipeImage();
		ri.drawable = this._context.getResources().getDrawable(ImageIds[position]);
		
		switch (position)
		{
		case 0:
			ri.title = "Curry";
			break;
		case 1:
			ri.title = "Delicious Fruit";
			break;
		case 2:
			ri.title = "Salmon from the River";
			break;
		case 3:
			ri.title = "Chicken Soul Food";
			break;
		case 4:
			ri.title = "Mom's Famous Spaghetti";
			break;
		}
		
		return ri;
	}
	
	private class RecipeImage
	{
		public Drawable drawable;
		public String title;
	}
}
