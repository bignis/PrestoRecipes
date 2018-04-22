package com.bignis.prestocookbook;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class LoadRecipesActivity extends Activity {

    private Uri uriFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_load_recipes);

        //showFakeData();

        extractZipFromIntentIntoStagingFolder();

        StagedRecipe[] stagedRecipes = getStagedRecipes();
        Arrays.sort(stagedRecipes);

        displayListOfStagedRecipes(stagedRecipes);
    }

    private StagedRecipe[] getStagedRecipes()
    {
        File[] xmlFiles = RecipesLoader.GetXmlFiles(RecipesLoader.GetStagingFolder());

        HashMap<String, File> xmlFileSet = new HashMap<String, File>();

        for (File file : xmlFiles)
        {
            String shortName = RecipesLoader.getFileNameWithoutExtension(file.getName());

            xmlFileSet.put(shortName, file);
        }

        // Now create the data

        ArrayList<StagedRecipe> list = new ArrayList<StagedRecipe>();

        // Do the XML files first
        {
            Iterator it = xmlFileSet.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry)it.next();

                File file = (File)pair.getValue();

                StagedRecipe item = new StagedRecipe();

                Recipe recipe;

                try
                {
                    recipe = RecipeParser.ParseFromXmlFile(file.getPath());
                    item.RecipeTitle = recipe.Title;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    this.showMessage("Error: " + e.toString());
                    continue;
                }

                Pair<Integer,Long> recipeReplacementInfo = RecipesLoader.getRecipeReplacementInfoFromStagingFolder();

                if (recipeReplacementInfo != null) {

                    if (list.size() != 0) {
                        throw new RuntimeException("Unexpected, multiple recipes present when there is a 'replacement' file in the folder");
                    }

                    Recipe recipeInDatabase = Recipe.getFromDatabase(recipeReplacementInfo.first, this);

                    if (recipeInDatabase != null &&
                            recipeInDatabase.XmlHash == recipeReplacementInfo.second) {
                        item.ReplacesRecipeWithId = recipeReplacementInfo.first;
                        item.ReplacesRecipeWithTitle = recipeInDatabase.Title;
                    }
                }

                list.add(item);
            }
        }

        return (StagedRecipe[])list.toArray(new StagedRecipe[0]);
    }

    private void displayListOfStagedRecipes(StagedRecipe[] recipes)
    {
        final ListView listView = (ListView) findViewById(R.id.listview);

        //final ArrayAdapter<StagedRecipe> adapter = new ArrayAdapter<StagedRecipe>(this, android.R.layout.simple_list_item_1, recipes);
        final StagedRecipesArrayAdapter adapter = new StagedRecipesArrayAdapter(this, recipes);

        listView.setAdapter(adapter);

        if (recipes.length == 0) {
            this.showMessage("No recipes are ready to load.");
        }
    }

    private void showMessage(String textToDisplay)
    {
        LinearLayout view = (LinearLayout)this.findViewById(R.id.container);

        TextView text = new TextView(this);
        text.setText(textToDisplay);
        view.addView(text);
    }

    private void ensurePermissionsGranted() {

        // Necessary because not having permissions to GMail and/or external storage causes crashes
        // https://developer.android.com/training/secure-file-sharing/request-file.html
        // source: https://fabric.io/michael-nistlers-projects/android/apps/com.bignis.prestorecipesunderground/issues/5747544affcdc042506c20c8

        boolean haveStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

//        if (!(haveStoragePermission)) {
       /*     ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO_MGN);
*/
            // TODO someday, react to permissions granted/denied
  //      }
    }

    private void extractZipFromIntentIntoStagingFolder()
    {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (!(Intent.ACTION_VIEW.equals(action)))
        {
            throw new RuntimeException("Action " + action + " doesn't match expected VIEW action");
        }

        if (!(intent.getScheme().equals("content")) &&// gmail open file link - https://code.google.com/p/codenameone/issues/detail?id=772
            !(intent.getScheme().equals("file")))  // Outlook.com email opens it as "file"
        {
            this.showMessage("Intent scheme didn't match - " + intent.getScheme());
            return;
        }

        // First clear the staging folder, don't want other junk from "before" polluting it if we want to just load the from the Presto .zip given to us
        deleteFilesFromFolder(RecipesLoader.GetStagingFolder());

        this.uriFromIntent = intent.getData();

        try
        {

            InputStream attachment = this.getContentResolver().openInputStream(this.uriFromIntent);

            ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(attachment));

            extractRecipeFilesToStagingFolder(zipStream);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            this.showMessage("Error: " + e.toString());
            return;
        }
    }
/*
    static boolean isValid(final File file) {
        ZipFile zipfile = null;
        try {
            zipfile = new ZipFile(file);
            return true;
        } catch (ZipException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (zipfile != null) {
                    zipfile.close();
                    zipfile = null;
                }
            } catch (IOException e) {
            }
        }
    }
*/
    private void extractRecipeFilesToStagingFolder(ZipInputStream zipStream) throws IOException
    {
        // Delete the existing (if any) contents of the staging folder

        File stagingFolder = RecipesLoader.GetStagingFolder();

        LoadRecipesActivity.deleteFilesFromFolder(stagingFolder);

        // Extract zip file
        ZipEntry zipEntry;

        while ((zipEntry = zipStream.getNextEntry()) != null)
        {
            String fileName = zipEntry.getName();

            String outputFilePath = stagingFolder.getPath() + "/" + fileName;

            FileOutputStream fout = new FileOutputStream(outputFilePath);

            byte[] buffer = new byte[1024];
            int count;

            while ((count = zipStream.read(buffer)) != -1)
            {
                fout.write(buffer, 0, count);
            }

            fout.close();

            zipStream.closeEntry();
        }

        zipStream.close();
    }

    private static void deleteFilesFromFolder(File folder) {
        File[] Files = folder.listFiles();
        if(Files != null) {
            int j;
            for(j = 0; j < Files.length; j++) {
                Files[j].delete();
            }
        }
    }

    public void loadRecipesClick(View v) {

        Handler.Callback postExecuteCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {

                Intent intent = new Intent(LoadRecipesActivity.this, RecipesListActivity.class);
                intent.putExtra("RepopulateRecipesWhenShown", true);
                LoadRecipesActivity.this.startActivity(intent);

                return true;
            }
        };

        new RecipesLoaderTask(this, RecipeLoadType.StagingFolderOnly, new Handler(postExecuteCallback)).execute(); // Load recipes
    }

    public String getFileNameFromUri(Uri uri) {

        // If I open the file from Chrome, it give me a URI of content://downloads/my_downloads/1586 and doesn't find a name
        // If I open the same downloaded file from Downloads, it gives me a URI of content://downloads/all_downloads/1586 (same id) and it works
        // HACK
        //uri = Uri.parse(uri.toString().replace("my_downloads", "all_downloads"));
        // Caused by: java.lang.SecurityException: Permission Denial: reading com.android.providers.downloads.DownloadProvider uri content://downloads/all_downloads/1587 from pid=29247, uid=10137 requires android.permission.ACCESS_ALL_DOWNLOADS, or grantUriPermission()

        String[] projection = {OpenableColumns.DISPLAY_NAME};

        // http://stackoverflow.com/a/25005243
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void cancelClick(View v) {

        // Delete things since we didn't choose to load
        deleteFilesFromFolder(RecipesLoader.GetStagingFolder());

        Intent intent = new Intent(this, RecipesListActivity.class);
        this.startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.load_recipes, menu);
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_load_recipes, container, false);
            return rootView;
        }
    }

    public class StagedRecipe implements Comparable<StagedRecipe>
    {
        public String RecipeTitle;
        public String ImageFileName; // Null if no image.
//@Deprecated
//public boolean AlreadyExists;
        public String ReplacesRecipeWithTitle;
        public int ReplacesRecipeWithId;

        @Override
        public int compareTo(StagedRecipe stagedRecipe) {
            String my = this.RecipeTitle != null ? this.RecipeTitle : this.ImageFileName;
            String other = stagedRecipe.RecipeTitle != null ? stagedRecipe.RecipeTitle : stagedRecipe.ImageFileName;
            return my.compareTo(other);
        }
    }

    //public class StagedRecipesArrayAdapter extends ArrayAdapter<StagedRecipe> {
    public class StagedRecipesArrayAdapter extends BaseAdapter {
        private final Context context;
        private final StagedRecipe[] values;

        public StagedRecipesArrayAdapter(Context context, StagedRecipe[] values) {
            super();
            this.context = context;
            this.values = values;
        }

        @Override
        public int getCount() {
            return this.values.length;
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            /*
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
*/
            StagedRecipe data = this.values[position];

            TextView textView = new TextView(this.context);

            String displayText = "";

            if (data.RecipeTitle != null)
            {
                displayText += data.RecipeTitle;
            }
            else
            {
                displayText += data.ImageFileName;
            }

            if (data.ReplacesRecipeWithTitle != null)
            {
                if (data.ReplacesRecipeWithId != 0) {
                    displayText += " (will replace the existing recipe named '" + data.ReplacesRecipeWithTitle + "')";
                }
                else {
                    displayText += " (will replace existing recipe)";
                }
            }

            textView.setText(displayText);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);  // Scalable pixels

            return textView;
        }
    }
}

