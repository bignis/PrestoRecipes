package com.bignis.prestocookbook;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.widget.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        Intent intent = this.getIntent();
        int recipeId = intent.getIntExtra(RecipesListActivity.RECIPE_ID, -69);

        if (recipeId <= 0)
        {
        	throw new RuntimeException("RecipeId not found on the intent");
        }
        
        try
        {
	        Recipe recipe = getRecipe(recipeId);
	        
	        displayRecipe(recipe);
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
			    null,                                // The columns for the WHERE clause
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
    	
    	LinearLayout contentView = (LinearLayout)this.findViewById(R.id.recipeContentLayout);
    	//style="@android:style/TextAppearance.Medium"
    	
    	
    	
    	float ingredientsSize = this.getResources().getDimension(R.dimen.IngredientsSize);
    	float stepsSize = this.getResources().getDimension(R.dimen.StepsSize);
    	
    	// Body
    	
    	TextView title = new TextView(this);
    	contentView.addView(title);
    	title.setText(recipe.Title);
    	//title.setTextSize(this.getResources().getDimension(R.dimen.TitleSize));
    	title.setTextAppearance(this, android.R.style.TextAppearance_Large);
    	
    	for (int i = 0; i < recipe.Ingredients.size(); ++i)
    	{
    		TextView ingredient = new TextView(this);
        	contentView.addView(ingredient);
        	ingredient.setText(recipe.Ingredients.get(i));
        	//ingredient.setTextSize(ingredientsSize);
        	ingredient.setTextAppearance(this, android.R.style.TextAppearance_Medium);
    	}
    	
    	for (int i = 0; i < recipe.Steps.size(); ++i)
    	{
    		TextView step = new TextView(this);
        	contentView.addView(step);
        	step.setText(recipe.Steps.get(i));
        	//step.setTextSize(stepsSize);
        	step.setTextAppearance(this, android.R.style.TextAppearance_Medium);
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
}