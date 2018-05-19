package com.bignis.prestocookbook;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SearchView.OnQueryTextListener;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import android.support.v7.widget.Toolbar;

public class RecipesListActivity extends AppCompatActivity implements OnQueryTextListener {

	public final static String RECIPE_ID = "com.bignis.PrestoCookbook.RECIPE_ID";
	public final static String ALL_RECIPES_CATEGORY = "All Recipes";

	private final int UNIQUE_REQUEST_CODE_FOR_SAVE_FILE = 952;

	private String currentSearchQuery;
	private String currentCategorySelection;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());

		setContentView(R.layout.activity_recipes_list);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		this.setSupportActionBar(myToolbar);  // Get this from extending AppCompatActivity

		if (this.getIntent().hasExtra("RepopulateRecipesWhenShown")) {
			this.PopulateRecipes();  // After new recipes are loaded
		}

		// Spinner.OnItemSelected will call PopulateRecipes, so this one can be removed
		//this.PopulateRecipes(); //  Get all recipes
	}
	
	public void PopulateRecipes()
	{
		String searchQuery = this.currentSearchQuery;
		String categorySelection = this.currentCategorySelection;
		
		RecipeForList[] recipes = this.GetRecipesForList(searchQuery, categorySelection);  
		 
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

			Intent intent = new Intent(this._activity, DisplayRecipeActivity.class);
			intent.putExtra(RECIPE_ID, recipeId);
			this._activity.startActivity(intent);
			
		}
	
	}
	
	
	public boolean onQueryTextChange(String newText) {

		if ("version".equalsIgnoreCase(newText)) {
			new AlertDialog.Builder(this)
					.setTitle("Version of Presto Recipes")
					.setMessage("Version " + this.getString(R.string.current_version))
					.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// Nothing to do, alert will just close.
						}
					})
					.setIcon(android.R.drawable.ic_dialog_info)
					.show();

			return false;
		}

		if ("reset".equalsIgnoreCase(newText)) {
			new AlertDialog.Builder(this)
					.setTitle("Reset All Recipes")
					.setMessage("Are you sure you want to delete all recipes?")
					.setPositiveButton("Delete Everything", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							resetDatabase();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();

			return false;
		}

		if ("backup".equalsIgnoreCase(newText)) {
			new AlertDialog.Builder(this)
					.setTitle("Download a Backup Of All Recipes")
					.setMessage("Are you sure you want to download a backup copy of all your recipes?")
					.setPositiveButton("Download a Backup", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							downloadBackupOfAllRecipes();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// do nothing
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();

			return false;
		}

		if ("intcrash".equalsIgnoreCase(newText)) {
			throw new RuntimeException("MGN intentional crash to see if GA is working");
		}

		this.currentSearchQuery = newText;
		
		PopulateRecipes();
		//Toast.makeText(this, newText, Toast.LENGTH_SHORT).show();
        return false;
    }
 
    public boolean onQueryTextSubmit(String query) {
    	
    	this.currentSearchQuery = query;
    	
    	PopulateRecipes();
    	//Toast.makeText(this, "Submit: " + query, Toast.LENGTH_SHORT).show();
        return false;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_recipes_list, menu);

		//http://stackoverflow.com/questions/11276043/how-to-add-a-searchwidget-to-the-actionbar
		MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);


        this.populateCategorySpinner(menu);
             
		return true;
	}
	
	private void populateCategorySpinner(Menu menu)
	{		
        MenuItem spinnerMenuItem = menu.findItem(R.id.action_category);
		
		//String[] availableCategories = new String[] { "Main Dish", "Side Dish", "Dessert"};
        String[] availableCategories = GetAvailableCategoriesFromRecipes();
        
        // Just "All Categories" item or nothing at all... for some reason... guard...
        if (availableCategories.length <= 1)
        {
			// If items *were* to be shown, this activity relies on teh OnItemSelected() method (on initial population) to call
			// PopulateRecipes().  If the spinner isn't showing up, we need to call PopulateRecipes anyway to have consistent behavior
			// Use case: 2 recipes are loaded but neither have a category.
			this.PopulateRecipes();

        	spinnerMenuItem.setVisible(false);
        	return;
        }
        
        Spinner spinner = (Spinner)spinnerMenuItem.getActionView();

        //int spinnerLayoutType = android.R.layout.simple_list_item_1;
        int spinnerLayoutType = android.R.layout.simple_spinner_dropdown_item;
        ArrayAdapter<CharSequence> categoriesAdapter = new ArrayAdapter<CharSequence>(this, spinnerLayoutType, availableCategories);
        spinner.setAdapter(categoriesAdapter);
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 
            	CharSequence selected = (String)adapterView.getItemAtPosition(i);
            	
            	RecipesListActivity.this.currentCategorySelection = selected.toString();
            	
            	RecipesListActivity.this.PopulateRecipes();
            	
            	//Context context = RecipesListActivity.this; // mgn, hmmmmm
            	//Toast.makeText(context, selected, Toast.LENGTH_LONG).show();
            } 

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            } 
        }); 
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
			case R.id.menu_load_recipes:
			{
				/* Old behavior
				new RecipesLoaderTask(this).execute();
				return true;
				*/

				// Tell the user that the can use a full computer if they want...
				AlertDialog alertDialog = new AlertDialog.Builder(this).create();
				alertDialog.setTitle("Reminder");
				alertDialog.setMessage("If you want to use a full computer to load recipes, visit \n\"http://presto.bignis.com\"\n on your computer - you can email yourself recipes to load to your Android device.\n\nYou will now be taken to \"http://presto.bignis.com\".");
				alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();

								android.net.Uri.Builder builder = new android.net.Uri.Builder();
								builder.scheme("http");
								builder.authority("presto.bignis.com");

								builder.appendQueryParameter("fromApp", "true");

								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setData(builder.build());
								startActivity(Intent.createChooser(intent, "Choose Web Browser"));

							}
						});
				alertDialog.show();

				return true;
			}
	    
			default:
				return super.onOptionsItemSelected(item);
	    }
	}

	private void resetDatabase() {

		Handler.Callback postExecuteCallback = new Handler.Callback() {
			@Override
			public boolean handleMessage(Message message) {

				RecipesLoader.ResetDatabase(RecipesListActivity.this);

				Toast.makeText(RecipesListActivity.this, "Database has been reset", Toast.LENGTH_SHORT).show();  // Must do this from the main thread

				// Reset selections
				RecipesListActivity.this.currentSearchQuery = null;
				RecipesListActivity.this.currentCategorySelection = null;

				RecipesListActivity.this.PopulateRecipes();

				return true;
			}
		};

		new RecipesLoaderTask(this, new Handler(postExecuteCallback)).execute();
	}

	private void downloadBackupOfAllRecipes() {
		try {

			SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
			String currentDate = sdf.format(new Date());
			String proposedFileName = "AllRecipes-" + currentDate + ".presto";

			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_TITLE, proposedFileName);
			startActivityForResult(intent, UNIQUE_REQUEST_CODE_FOR_SAVE_FILE);
		}
		catch (Exception e) {
			e.printStackTrace();
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Error backing up");
			alertDialog.setMessage(e.getMessage());
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alertDialog.show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == UNIQUE_REQUEST_CODE_FOR_SAVE_FILE) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();

				try {
					OutputStream stream = this.getContentResolver().openOutputStream(uri);

					ZipCreator.createZipBackupForAllRecipeFiles(this, stream);
				}
				catch (Exception e) {
					e.printStackTrace();
					AlertDialog alertDialog = new AlertDialog.Builder(this).create();
					alertDialog.setTitle("Error saving backup file");
					alertDialog.setMessage(e.getMessage());
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
					alertDialog.show();
				}
			}
		}
	}
	
	private String[] GetAvailableCategoriesFromRecipes()
	{
		Context context = this.getApplicationContext();
		
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String query = "SELECT DISTINCT Category FROM Recipes WHERE length(Category) > 0 ORDER BY Category";
		
		Cursor cursor = db.rawQuery(query, null);
		
		try
		{
			if (!(cursor.moveToFirst()))
			{
				return new String[0];
			}
			
			ArrayList<String> categories = new ArrayList<String>();
			
			categories.add(ALL_RECIPES_CATEGORY);
			
			do {
				categories.add(cursor.getString(cursor.getColumnIndexOrThrow("Category")));
	        } while (cursor.moveToNext());
			
			return (String[]) categories.toArray(new String[0]); // http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
		}
		finally
		{
			cursor.close();
			db.close();
			dbHelper.close();
		}
	}
	
	private RecipeForList[] GetRecipesForList(String searchQuery, String categorySelection)
	{
		Context context = this.getApplicationContext();
		
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		// Should really use FTS3 to make searches faster
		//http://blog.andresteingress.com/2011/09/30/android-quick-tip-using-sqlite-fts-tables/
		
		String query = "SELECT Id, Title FROM Recipes";
		ArrayList<String> whereClauses = new ArrayList<String>();  // will build it up
		
		if (searchQuery != null && searchQuery.length() != 0)
		{
			whereClauses.add("Title LIKE '%" + searchQuery + "%'");  // sqlEscapeString includes the apostrophes around strings so I can't use that here.  SQL Injection vulnerability :)
			
			//query += " WHERE Title LIKE '%" + searchQuery + "%'";  			
		}
		
		if (categorySelection != null && !(categorySelection.equals(ALL_RECIPES_CATEGORY)))
		{
			whereClauses.add("Category = " + DatabaseUtils.sqlEscapeString(categorySelection));  // sqlEscapeString includes the apostrophes around strings
		}

		if (!(whereClauses.isEmpty()))
		{
			query += " WHERE";
			
			boolean needAnd = false;
			
			for (String clause : whereClauses)
			{
				if (needAnd)
				{
					query += " AND";
				}
				
				query += " " + clause;
				
				needAnd = true;
			}
		}
		
		Cursor cursor = db.rawQuery(query + " ORDER BY Title", null);
		
		try
		{
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
			
	
			
			return (RecipeForList[]) rfls.toArray(new RecipeForList[0]); // http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
		}
		finally
		{
			cursor.close();
			db.close();
			dbHelper.close();
		}
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
