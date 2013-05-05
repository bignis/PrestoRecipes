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
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;

public class RecipesListActivity extends Activity implements OnQueryTextListener {

	public final static String RECIPE_ID = "com.bignis.PrestoCookbook.RECIPE_ID";
	public final static String RECIPE_XML_FILENAME = "com.bignis.PrestoCookbook.RECIPE_XML_FILENAME";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recipes_list);
		
		//You can enable type-to-search in your activity by calling setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL) during your activity's onCreate() method.
		//this.setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);		
		
		this.PopulateRecipes(null); // null = Get all recipes
	}
	
	private void PopulateRecipes(String searchQuery)
	{
		RecipeForList[] recipes = this.GetRecipesForList(searchQuery);  
		
		GridView gridView = (GridView)this.findViewById(R.id.gridView1);
		View noRecipesInDatabaseMessage = this.findViewById(R.id.noRecipesInDatabaseMessage);
		View noRecipesFoundFromSearchMessage = this.findViewById(R.id.noRecipesFoundFromSearchMessage);
		
		if (recipes == null || recipes.length == 0)
		{
			if (searchQuery == null)
			{
				// No Recipes in DB
				noRecipesInDatabaseMessage.setVisibility(View.VISIBLE);
			}
			else
			{
				// No search results
				noRecipesFoundFromSearchMessage.setVisibility(View.VISIBLE);
			}
			
			gridView.setAdapter(null);
			return;
		}
		
		int[] recipeIds = this.GetRecipeIds(recipes);
		
		noRecipesInDatabaseMessage.setVisibility(View.GONE);
		noRecipesFoundFromSearchMessage.setVisibility(View.GONE);
		
		gridView.setAdapter(new ImageAdapter(this, recipeIds));
		gridView.setOnItemClickListener(new StupidClickHandler(this));
	}
	
	private class StupidClickHandler implements OnItemClickListener 
	{

		private Activity _activity;
		
		public StupidClickHandler(Activity activity)
		{
			this._activity = activity;
		}
		
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		{
			Object tag = v.getTag();
			
			if (tag == null)
			{
				throw new RuntimeException("Null tag");
			}
			
			int recipeId = (Integer)tag;
			
			Intent intent = new Intent(this._activity, MainActivity.class);
			intent.putExtra(RECIPE_ID, recipeId);
			this._activity.startActivity(intent);
			
		}
	
	}
	
	
	public boolean onQueryTextChange(String newText) {
		PopulateRecipes(newText);
		//Toast.makeText(this, newText, Toast.LENGTH_SHORT).show();
        return false;
    }
 
    public boolean onQueryTextSubmit(String query) {
    	PopulateRecipes(query);
    	//Toast.makeText(this, "Submit: " + query, Toast.LENGTH_SHORT).show();
        return false;
    }
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_recipes_list, menu);
		
		//http://stackoverflow.com/questions/11276043/how-to-add-a-searchwidget-to-the-actionbar
		MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menu_load_recipes:
	    	String message = RecipesLoader.LoadRecipes(this.getApplicationContext());
	    	Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	    	this.PopulateRecipes(null);  // Reload the recipe list from scratch
	        return true;
	    case R.id.menu_reset_database:
	    {
	    	RecipeDBHelper dbHelper = new RecipeDBHelper(this);
	    	/*
	    	new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle("All your data will be lost")
	        .setMessage("Are you sure you want to reset?")
	        .setPositiveButton("Delete it all!", new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {

	                //Stop the activity
	                YourClass.this.finish();    
	            }

	        })
	        .setNegativeButton(R.string.no, null)
	        .show();
	    	*/
	    	dbHelper.onUpgrade(dbHelper.getWritableDatabase(), 1, 1);  // Triggers drop / recreate
	    	Toast.makeText(this, "Database has been reset", Toast.LENGTH_SHORT).show();
	    	this.PopulateRecipes(null); // Reload the recipe list from scratch
	    	return true;
	    }
	    	
	    
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	private RecipeForList[] GetRecipesForList(String searchQuery)
	{
		Context context = this.getApplicationContext();
		
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String[] projection = {
			    "Id",
			    "Title"
			    };
		
		// Should really use FTS3 to make searches faster
		//http://blog.andresteingress.com/2011/09/30/android-quick-tip-using-sqlite-fts-tables/
		
		String query = "SELECT Id, Title FROM Recipes";
		String whereClause = null;  // all rows
		String[] whereArgs = null;
		
		if (searchQuery != null && searchQuery.length() != 0)
		{
			whereClause = "Title LIKE '%?%'";
			whereArgs = new String[] { searchQuery };
			query += " WHERE Title LIKE '%" + searchQuery + "%'"; // Eh, ignoring sql injection because I can't figure out how to swap out param 
					
		}
		/*
		Cursor cursor = db.query(
			    "Recipes",  // The table to query
			    projection,                               // The columns to return
			    whereClause,                                // The columns for the WHERE clause
			    whereArgs,                            // The values for the WHERE clause
			    null,                                     // don't group the rows
			    null,                                     // don't filter by row groups
			    "Title"                                 // The sort order
			    );		
		*/
		Cursor cursor = db.rawQuery(query + " ORDER BY Title", null);
		
		
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
