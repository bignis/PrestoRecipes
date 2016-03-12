package com.bignis.prestocookbook;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.InputType;
import android.text.TextUtils;
import android.transition.Visibility;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
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



		float titleSize = this.getResources().getDimension(R.dimen.TitleSize);
    	float ingredientsSize = this.getResources().getDimension(R.dimen.IngredientsSize);
    	float stepsSize = this.getResources().getDimension(R.dimen.StepsSize);

    	// Body
    	
    	TextView titleTextView = (TextView)this.findViewById(R.id.titleTextView);
    	titleTextView.setText(recipe.Title);
		titleTextView.setTextSize(titleSize);

		//http://www.formatdata.com/recipeml/spec/recipeml-spec.html
		//In the rendition of a RecipeML document, a description traditionaly appears following the title and before the body.
		if (recipe.Description != null && recipe.Description.trim().length() != 0) {
			((TextView)this.findViewById(R.id.descriptionTextView)).setText(recipe.Description);
		}
		else
		{
			this.findViewById(R.id.descriptionTextView).setVisibility(View.GONE);
		}

		if (recipe.Yield != null && recipe.Yield.trim().length() != 0) {
			((TextView)this.findViewById(R.id.yieldTextView)).setText(recipe.Yield);
		}
		else
		{
			this.findViewById(R.id.yieldTextView).setVisibility(View.GONE);
		}

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

		//http://www.formatdata.com/recipeml/spec/recipeml-spec.html
		//The source element contains credit or source information for the recipe. If displayed as part of the rendered output of the RecipeML document (see rendering), the content normally appears as a footnote.

		if (recipe.Source != null && recipe.Source.trim().length() != 0) {
			((TextView)this.findViewById(R.id.sourceTextView)).setText("Source: " + recipe.Source);
		}
		else
		{
			this.findViewById(R.id.sourceTextView).setVisibility(View.GONE);
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_display_recipe, menu);
        return true;
    }

	private static Uri getUriForRecipe(Recipe recipe) {
		android.net.Uri.Builder builder = new android.net.Uri.Builder();
		builder.scheme("http");
		builder.authority("presto.bignis.com");

		builder.appendQueryParameter("title", recipe.Title);
		builder.appendQueryParameter("category", recipe.Category);
		builder.appendQueryParameter("source", recipe.Source);
		builder.appendQueryParameter("yield", recipe.Yield);
		builder.appendQueryParameter("ingredients", TextUtils.join("||", recipe.Ingredients));
		builder.appendQueryParameter("steps", TextUtils.join("||", recipe.Steps));

		return builder.build();
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

				startActivity(Intent.createChooser(intent, "Choose email program"));
			}
			case R.id.menu_edit_recipe:
			{
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(getUriForRecipe(_recipe));
				startActivity(Intent.createChooser(intent, "Choose Web Browser"));
			}
			case R.id.menu_delete_recipe:
			{

				new AlertDialog.Builder(this)
						.setMessage("Are you sure you want to delete this recipe?")
						.setCancelable(true)
						.setPositiveButton("Delete This Recipe", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// http://stackoverflow.com/a/5447120/5198
								Context context = DisplayRecipeActivity.this.getApplicationContext();

								RecipeDBHelper dbHelper = new RecipeDBHelper(context);

								SQLiteDatabase db = dbHelper.getWritableDatabase();

								SQLiteStatement delete = db.compileStatement("DELETE FROM Recipes WHERE ID = ?");
								delete.bindLong(1, DisplayRecipeActivity.this._recipeId);

								int rowsAffected = delete.executeUpdateDelete();

								db.close();
								dbHelper.close();

								Toast.makeText(DisplayRecipeActivity.this, "Recipe Deleted", Toast.LENGTH_SHORT).show();

								// Go back to the recipes list
								Intent intent = new Intent(DisplayRecipeActivity.this, RecipesListActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								startActivity(intent);
							}
						})
						.setNegativeButton("Cancel", null)
						.show();
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
