package com.bignis.prestocookbook;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class RecipesLoaderTask extends AsyncTask<Void, String, String> implements RecipeLoadProgressListener {
	
	private Context _context;
	private ProgressDialog _progressDialog;
	
	public RecipesLoaderTask(Context context) {
		if (context == null) {
			throw new NullPointerException("context");
		}
		
		this._context = context;
		
		this._progressDialog = new ProgressDialog(context);
    	this._progressDialog.setTitle("Loading");
    	this._progressDialog.show();
	}	

	@Override
    protected String doInBackground(Void... vd) {
    	
        return RecipesLoader.LoadRecipes(_context, this);
    }

    protected void onProgressUpdate(String progressDescription) {
        this._progressDialog.setMessage(progressDescription);
    }

    protected void onPostExecute(String result) {
    	Toast.makeText(this._context, result, Toast.LENGTH_SHORT).show();
    }
    
    public void progressUpdate(String updateDescription) {
    	this.publishProgress(updateDescription);  // Call the built-in method
    }
}
