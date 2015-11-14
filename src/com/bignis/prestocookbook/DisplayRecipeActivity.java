package com.bignis.prestocookbook;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.widget.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DisplayRecipeActivity extends Activity {

	private Recipe _recipe;
	private int _recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_recipe);
        
        
        
        Intent intent = this.getIntent();
        int recipeId = intent.getIntExtra(RecipesListActivity.RECIPE_ID, -69);

        if (recipeId <= 0)
        {
        	throw new RuntimeException("RecipeId not found on the intent");
        }
        
        try
        {
	        this._recipe = getRecipe(recipeId);
			this._recipeId = recipeId;
	        
	        this.setTitle(this._recipe.Title);
	        
	        displayRecipe(this._recipe);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
    }
    
    private Recipe getRecipe(int recipeId) throws Exception
    {
    	Context context = this.getApplicationContext();
    	
    	RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String[] projection = {
			    "Xml"
			    };
		
		Cursor cursor = db.query(
				"Recipes",  // The table to query
				projection,                               // The columns to return
				"ID = " + recipeId,                                // The columns for the WHERE clause
				null,                            // The values for the WHERE clause
				null,                                     // don't group the rows
				null,                                     // don't filter by row groups
				null                                 // The sort order
		);
		
		if (!(cursor.moveToFirst()))
		{
			throw new RuntimeException("Recipe id " + Integer.toString(recipeId) + " not found in database");
		}
		
		String xml = cursor.getString(cursor.getColumnIndexOrThrow("Xml"));
		
		if (cursor.moveToNext())
		{
			throw new RuntimeException("got multiple recipes in database query?!?");
		}
		
		dbHelper.close();
		
		return RecipeParser.ParseFromXmlString(xml);	
    }
    
    private void displayRecipe(Recipe recipe)
    {   
    	if (recipe == null)
    	{
    		throw new RuntimeException("recipe cannot be null");
    	}

    	/*TextView recipeTextView = (TextView)this.findViewById(R.id.textView1);
    	
    	if (recipeTextView == null)
    	{
    		//throw new RuntimeException("Couldn't find recipeTextView mgn");
    	}*/
    	
    	//recipeTextView.setText(recipe.Title);
    	
    	LinearLayout ingredientsLayout = (LinearLayout)this.findViewById(R.id.ingredientsContentLayout);
    	LinearLayout stepsLayout = (LinearLayout)this.findViewById(R.id.stepsContentLayout);
    	//style="@android:style/TextAppearance.Medium"
    	
    	
    	
    	float ingredientsSize = this.getResources().getDimension(R.dimen.IngredientsSize);
    	float stepsSize = this.getResources().getDimension(R.dimen.StepsSize);
    	
    	// Body
    	
    	TextView titleTextView = (TextView)this.findViewById(R.id.titleTextView);
    	titleTextView.setText(recipe.Title);
    	
    	for (int i = 0; i < recipe.Ingredients.size(); ++i)
    	{
    		TextView ingredient = new TextView(this);
    		ingredientsLayout.addView(ingredient);
    		String ingredientText = recipe.Ingredients.get(i);
        	ingredient.setText(ingredientText);
        	ingredient.setTextSize(ingredientsSize);
        	//ingredient.setTextAppearance(this, android.R.style.TextAppearance_Medium);
    	}
    	
    	LinearLayout.LayoutParams stepLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	stepLayoutParams.bottomMargin = 10;  // Space between steps
    	
    	
    	for (int i = 0; i < recipe.Steps.size(); ++i)
    	{
    		TextView step = new TextView(this);
    		stepsLayout.addView(step);
        	step.setText(Integer.valueOf(i+1).toString() + ") " + recipe.Steps.get(i));
        	
        	step.setLayoutParams(stepLayoutParams);
        	
        	step.setTextSize(stepsSize);
        	//step.setTextAppearance(this, android.R.style.TextAppearance_Medium);
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_display_recipe, menu);
        return true;
    }

	private static String getEmailBodyForRecipe(Recipe recipe) {

		StringBuilder builder = new StringBuilder();

		builder.append(recipe.Title);
		builder.append("\n\n");

		for (int i = 0; i < recipe.Ingredients.size(); ++i)
		{
			builder.append("- " + recipe.Ingredients.get(i));
			builder.append("\n");
		}

		builder.append("\n");

		for (int i = 0; i < recipe.Steps.size(); ++i)
		{
			builder.append(new Integer(i+1).toString() + ") " + recipe.Steps.get(i));
			builder.append("\n");
		}

		return builder.toString();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_email_recipe:
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("plain/text");
				//intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"some@email.address"});
				intent.putExtra(Intent.EXTRA_SUBJECT, this._recipe.Title + " from Presto Recipes");
				intent.putExtra(Intent.EXTRA_TEXT, getEmailBodyForRecipe(this._recipe));

				File zipFile = null;

				try {
					zipFile = createZipForRecipeFiles(this._recipe);
				}
				catch (Exception e) {
					e.printStackTrace();
				}

				if (zipFile != null) {

					Uri uri = Uri.fromFile(zipFile);
					intent.putExtra(Intent.EXTRA_STREAM, uri);
				}

				startActivity(Intent.createChooser(intent, ""));
			}


			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private File createZipForRecipeFiles(Recipe recipe)  throws Exception {

		Context context = this.getApplicationContext();

		RecipeDBHelper dbHelper = new RecipeDBHelper(context);

		SQLiteDatabase db = dbHelper.getReadableDatabase();

		String query = "SELECT XmlFileName, ImageFileName FROM Recipes WHERE Id = " + new Integer(this._recipeId).toString();

		Cursor cursor = db.rawQuery(query, null);

		String xmlFileName = null;
		String imageFileName = null;

		try
		{
			if (!(cursor.moveToFirst()))
			{
				return null;
			}

			xmlFileName = cursor.getString(cursor.getColumnIndexOrThrow("XmlFileName"));
			imageFileName = cursor.getString(cursor.getColumnIndexOrThrow("ImageFileName"));
		}
		finally
		{
			cursor.close();
			db.close();
			dbHelper.close();
		}

		return createZipForRecipeFiles(xmlFileName, imageFileName);
	}

	private File createZipForRecipeFiles(String xmlFileName, String imageFileName) throws Exception {

		File folder = RecipesLoader.GetDataFolder();

		if (xmlFileName == null) {
			return null;
		}

		Uri xmlFileUri = Uri.fromFile(new File(xmlFileName));

		String zipFileName = xmlFileUri.getLastPathSegment();
		zipFileName = zipFileName.substring(0, zipFileName.length() - 4);  // Remove .xml (probably unsafe to assume this, but meh)

		File zipFile = new File(folder.getPath() + "/" + zipFileName + ".presto");
		zipFile.setReadable(true, false);  // mimic world_readable


		FileOutputStream fos = new FileOutputStream(zipFile);

		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			BufferedInputStream origin;
			FileInputStream fi;

			int BUFFER = 2048;
			byte data[] = new byte[BUFFER];
			int count;

			ZipEntry xmlEntry = new ZipEntry(xmlFileName);
			zos.putNextEntry(xmlEntry);

			fi = new FileInputStream(folder + "/" + xmlFileName);
			origin = new BufferedInputStream(fi, BUFFER);

			while ((count = origin.read(data, 0, BUFFER)) != -1) {
				zos.write(data, 0, count);
			}

			zos.closeEntry();
			origin.close();
			fi.close();

			////////
			if (imageFileName != null) {

				ZipEntry imageEntry = new ZipEntry(xmlFileName);
				zos.putNextEntry(imageEntry);

				fi = new FileInputStream(folder + "/" + imageFileName);
				origin = new BufferedInputStream(fi, BUFFER);

				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					zos.write(data, 0, count);
				}

				zos.closeEntry();
				origin.close();
				fi.close();
			}
		} finally {
			zos.close();
		}

		long length = zipFile.length();

		return zipFile;
	}

}
