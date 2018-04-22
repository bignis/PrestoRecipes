package com.bignis.prestocookbook;
import java.util.ArrayList;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;


public class ImageAdapter extends BaseAdapter implements SpinnerAdapter {

	private Context _context;
	private int[] _recipeIds;
	
	public ImageAdapter(Context context, int[] recipeIds)
	{
		if (context == null)
		{
			throw new RuntimeException("context was null");
		}
		
		if (recipeIds == null || recipeIds.length == 0)
		{
			throw new RuntimeException("recipeIds was null or empty");
		}
		
		this._context = context;
		this._recipeIds = recipeIds;
	}
	
	@Override
	public int getCount() {
		return this._recipeIds.length;
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
			// Never reuse, but this might kill performance...
			//return convertView;
		}
		
		final int textWidth = 300;
		final int textHeightPixels = 70;

		//http://stackoverflow.com/questions/5255184/android-and-setting-width-and-height-programmatically-in-dp-units
		final int textHeightSP = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textHeightPixels, arg2.getResources().getDisplayMetrics());
		
		RelativeLayout layout = new RelativeLayout(_context);

		int backgroundColor = position % 2 == 0 ? 0xFFA6A3FF : 0xFFD5D4FF;
		layout.setBackgroundColor(backgroundColor);

		int recipeId = this._recipeIds[position];
		layout.setTag(recipeId); // For onclick
		
		RecipeImage recipeImage = getRecipeImage(recipeId);
		
		// Not every recipe has an image
		if (recipeImage.drawable != null)
		{
			//layout.setBackgroundDrawable(recipeImage.drawable);
	//		layout.setLayoutParams(new RelativeLayout.LayoutParams(recipeImage.drawable.getIntrinsicWidth(), recipeImage.drawable.getIntrinsicHeight()));


			ImageView imageView = new ImageView(this._context);
			//noinspection ResourceType
			imageView.setId(1);  // just picked a number
			Drawable drawable = recipeImage.drawable;
			LayoutParams params = new LayoutParams(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			imageView.setLayoutParams(params);

			imageView.setImageDrawable(drawable);

			layout.addView(imageView);

		}

		TextView textView = new TextView(this._context);
		textView.setText(recipeImage.title);
		textView.setTextColor(0xFFFFFFFF);
		textView.setWidth(textWidth);
		textView.setHeight(textHeightSP);
		textView.setPadding(20, 20, 20, 20);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		textView.setGravity(Gravity.CENTER_VERTICAL);
		textView.setBackgroundColor(0x66000000);  // FF at the beginning means fully opaque
		

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		
		textView.setLayoutParams(params);

		
		layout.addView(textView);
		
		return layout;
		
	}

	private RecipeImage getRecipeImage(int recipeId)
	{		
		RecipeDBHelper dbHelper = new RecipeDBHelper(this._context);		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		SQLiteStatement select = db.compileStatement(
				"SELECT Title, Image FROM Recipes " + 
				"WHERE Id = ?");
		
		select.bindLong(1, recipeId);

		String[] projection = {
			    "Title",
			    "Image"
			    };
		
		Cursor cursor = db.query(
			    "Recipes",  // The table to query
			    projection,                               // The columns to return
			    "Id = " + new Integer(recipeId).toString(),                                // The columns for the WHERE clause
			    null,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    "Title"                              // The sort order
			    );		
		
		if (!(cursor.moveToFirst()))
		{
			throw new RuntimeException("No results for ID query");
		}
		
		RecipeImage ri = new RecipeImage();
		ri.title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));
		
		byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("Image"));
		
		if (imageBytes != null)
		{		
			Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
			
			ri.drawable = new BitmapDrawable(this._context.getResources(), bitmap);
		}
		
		if (cursor.moveToNext())
		{
			throw new RuntimeException("More than 1 result came back?!?");
		}

		cursor.close();

		dbHelper.close();
		
		return ri;
	}
	
	private class RecipeImage
	{
		public Drawable drawable;
		public String title;
	}
}
