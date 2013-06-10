package com.bignis.prestocookbook;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class RecipesLoaderTask extends AsyncTask<Void, String, String> implements RecipeLoadProgressListener {
	
	private RecipesListActivity _context;
	private ProgressDialog _progressDialog;
	
	public RecipesLoaderTask(RecipesListActivity context) {
		if (context == null) {
			throw new NullPointerException("context");
		}
		
		this._context = context;
		

	}	

	@Override
    protected String doInBackground(Void... vd) {
    	
        return RecipesLoader.LoadRecipes(_context, this);
    }

	@Override
    protected void onPreExecute()
    {
		this._progressDialog = new ProgressDialog(this._context);
    	this._progressDialog.setTitle("Loading");
    	this._progressDialog.show();

    }
	
	@Override
    protected void onProgressUpdate(String... progressDescriptions) {
    	super.onProgressUpdate(progressDescriptions);
        this._progressDialog.setMessage(progressDescriptions[0]);
    }

    @Override
    protected void onPostExecute(String result) {
    	this._progressDialog.dismiss();
    	Toast.makeText(this._context, result, Toast.LENGTH_SHORT).show();
    	this._context.PopulateRecipes(null); // Reload the recipe list from scratch
    }
    
    public void progressUpdate(String updateDescription) {
    	this.publishProgress(updateDescription);  // Call the built-in method
    }
}
