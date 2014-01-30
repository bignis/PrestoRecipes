package com.bignis.prestocookbook;

import com.bignis.prestocookbook.database.RecipeDBHelper;
import android.view.ViewGroup.LayoutParams;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.widget.*;

public class DisplayRecipeActivity extends Activity {

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
	        Recipe recipe = getRecipe(recipeId);
	        
	        this.setTitle(recipe.Title);
	        
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
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
}
