package com.bignis.prestocookbook;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import java.util.ArrayList;

public class Recipe {
	public int Id;
	public String Title;
	public String Category;
	public String Description;
	public String Source;
	public String Yield;
	public ArrayList<String> Ingredients = new ArrayList<String>();
	public ArrayList<String> Steps = new ArrayList<String>();
	public long XmlHash;
	
	public Recipe()
	{
	}

	public static Recipe getFromDatabase(int recipeId, Context context)
	{
		if (context == null) {
			throw new IllegalArgumentException("context");
		}

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
			return null;
			//throw new RuntimeException("Recipe id " + Integer.toString(recipeId) + " not found in database");
		}

		String xml = cursor.getString(cursor.getColumnIndexOrThrow("Xml"));

		if (cursor.moveToNext())
		{
			throw new RuntimeException("got multiple recipes in database query?!?");
		}

		dbHelper.close();

		try {
			Recipe recipe = RecipeParser.ParseFromXmlString(xml);

			recipe.Id = recipeId;  // The only field that's not contained in the XML

			return recipe;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
