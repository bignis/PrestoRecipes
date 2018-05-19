package com.bignis.prestocookbook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class RecipesLoaderTask extends AsyncTask<Void, String, String> implements RecipeLoadProgressListener {
	
	private Activity _context;
	private ProgressDialog _progressDialog;
	private boolean _exceptionOccurredDuringRecipeLoading;
	private Handler _onPostExecuteCallback;
	
	public RecipesLoaderTask(Activity context, Handler onPostExecuteCallback) {
		if (context == null) {
			throw new NullPointerException("context");
		}
		
		this._context = context;
		this._onPostExecuteCallback = onPostExecuteCallback;
	}	

	@Override
    protected String doInBackground(Void... vd) {
		
		_exceptionOccurredDuringRecipeLoading = false;
		
    	try
    	{
    		return RecipesLoader.LoadRecipesFromStagingFolder(_context, this);
    	}
    	catch (RecipeLoadException ex)
    	{
    		_exceptionOccurredDuringRecipeLoading = true;
    		
    		return ex.getMessage();
    	}
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
    	
    	/*
    	Toast toast = new Toast(this._context);
    	toast.setDuration(_exceptionOccurredDuringRecipeLoading ? 60 * 1000 : 3 * 1000);
    	TextView message = new TextView(this._context);
    	message.setText(result);
    	message.setBackgroundColor(Color.BLACK);
    	message.setTextColor(Color.WHITE);
    	toast.setView(message);
    	toast.show();    
    	*/
    	Toast.makeText(this._context, result, _exceptionOccurredDuringRecipeLoading ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();

		if (_onPostExecuteCallback != null) {
			_onPostExecuteCallback.sendEmptyMessage(0);
		}

    	//this._context.PopulateRecipes(); // Reload the recipe list from scratch
    }
    
    public void progressUpdate(String updateDescription) {
    	this.publishProgress(updateDescription);  // Call the built-in method
    }
}
