package com.bignis.prestocookbook;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class RecipesListActivity extends Activity {

	public final static String RECIPE_ID = "com.bignis.PrestoCookbook.RECIPE_ID";
	public final static String RECIPE_XML_FILENAME = "com.bignis.PrestoCookbook.RECIPE_XML_FILENAME";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recipes_list);
		
		LinearLayout linearLayout =  (LinearLayout)this.findViewById(R.id.linearLayoutMgn);
		
		RecipeForList[] recipes = this.GetRecipesForList();
		
		int[] recipeIds = this.GetRecipeIds(recipes);
		
		GridView gridView = (GridView)this.findViewById(R.id.gridView1);
		gridView.setAdapter(new ImageAdapter(this, recipeIds));
		
		if (recipes == null || recipes.length == 0)
		{
			TextView text = new TextView(this);
			text.setText("No xml files found");
			linearLayout.addView(text);
			return;
		}
		
		for (RecipeForList recipe : recipes)
		{
			Button button = new Button(this);
			button.setText(recipe.Title);
			linearLayout.addView(button);
			button.setOnClickListener(new StupidClickHandler(this, recipe.Id));
		}
		
	}
	
	private class StupidClickHandler implements OnClickListener
	{
		private int _recipeId;
		private Activity _activity;
		
		public StupidClickHandler(Activity activity, int recipeId)
		{
			this._activity = activity;
			this._recipeId = recipeId;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(this._activity, MainActivity.class);
    		intent.putExtra(RECIPE_ID, this._recipeId);
    		this._activity.startActivity(intent);
		}
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_recipes_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menu_load_recipes:
	    	String message = RecipesLoader.LoadRecipes(this.getApplicationContext());
	    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private RecipeForList[] GetRecipesForList()
	{
		Context context = this.getApplicationContext();
		
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String[] projection = {
			    "Id",
			    "Title"
			    };
		
		Cursor cursor = db.query(
			    "Recipes",  // The table to query
			    projection,                               // The columns to return
			    null,                                // The columns for the WHERE clause
			    null,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    "Title"                                 // The sort order
			    );		
		
		if (!(cursor.moveToFirst()))
		{
			return new RecipeForList[0];
		}
		
		ArrayList<RecipeForList> rfls = new ArrayList<RecipeForList>();
		
		do {
			RecipeForList rfl = new RecipeForList();
			rfls.add(rfl);
			
			rfl.Id = cursor.getInt(cursor.getColumnIndexOrThrow("Id"));
			rfl.Title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));
        } while (cursor.moveToNext());
		
		dbHelper.close();
		
		return (RecipeForList[]) rfls.toArray(new RecipeForList[0]); // http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
	}
	
	private int[] GetRecipeIds(RecipeForList[] rfls)
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		for (RecipeForList rfl : rfls)
		{
			list.add(rfl.Id);
		}
		
		return convertIntegers(list);
	}
	
	public static int[] convertIntegers(List<Integer> integers)
	{
	    int[] ret = new int[integers.size()];
	    Iterator<Integer> iterator = integers.iterator();
	    for (int i = 0; i < ret.length; i++)
	    {
	        ret[i] = iterator.next().intValue();
	    }
	    return ret;
	}
	
	private class RecipeForList
	{
		public int Id;
		public String Title;
	}
}
