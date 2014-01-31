package com.bignis.prestocookbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoadRecipesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_recipes);

        //showFakeData();
        promptToLoadStagedRecipes(getStagedRecipes());
        //showStuff();
    }

    private void showFakeData()
    {
        StagedRecipe[] data = new StagedRecipe[3];
        data[0] = new StagedRecipe();
        data[0].AlreadyExists = true;
        data[0].ImageFileName = "foo.jpg";
        data[0].RecipeTitle = "Big Mac";

        data[1] = new StagedRecipe();
        data[1].AlreadyExists = true;
        data[1].ImageFileName = "foo.jpg";
        data[1].RecipeTitle = "Big Mac2";

        data[2] = new StagedRecipe();
        data[2].AlreadyExists = true;
        data[2].ImageFileName = "foo.jpg";
        data[2].RecipeTitle = "Big Mac3";

        promptToLoadStagedRecipes(data);
    }

    private StagedRecipe[] getStagedRecipes()
    {
        File[] xmlFiles = RecipesLoader.GetXmlFilesFromStagingFolder();
        File[] imageFiles = RecipesLoader.GetImageFilesFromStagingFolder();

        HashMap<String, File> xmlFileSet = new HashMap<String, File>();
        HashMap<String, File> imageFileSet = new HashMap<String, File>();

        for (File file : xmlFiles)
        {
            String shortName = file.getName().substring(0, file.getName().length() - 4);

            xmlFileSet.put(shortName, file);
        }

        for (File file : imageFiles)
        {
            String shortName = file.getName().substring(0, file.getName().length() - 4);

            imageFileSet.put(shortName, file);
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

                try
                {
                    Recipe recipe = RecipeParser.ParseFromXmlFile(file.getPath());
                    item.RecipeTitle = recipe.Title;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    continue;
                }

                item.AlreadyExists = new File(RecipesLoader.GetDataFolder() + "/" + file.getName()).exists();

                list.add(item);

                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        // Any "orphan" image files?
        {
            Iterator it = imageFileSet.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry)it.next();

                if (xmlFileSet.containsKey(pair.getKey())) // Already covered
                {
                    continue;
                }

                File file = (File)pair.getValue();

                StagedRecipe item = new StagedRecipe();

                item.ImageFileName = file.getName();
                item.AlreadyExists = new File(RecipesLoader.GetDataFolder() + "/" + file.getName()).exists();

                list.add(item);

                it.remove(); // avoids a ConcurrentModificationException
            }
        }

        return (StagedRecipe[])list.toArray(new StagedRecipe[0]);
    }

    private void promptToLoadStagedRecipes(StagedRecipe[] recipes)
    {
        final ListView listView = (ListView) findViewById(R.id.listview);

        //final ArrayAdapter<StagedRecipe> adapter = new ArrayAdapter<StagedRecipe>(this, android.R.layout.simple_list_item_1, recipes);
        final StagedRecipesArrayAdapter adapter = new StagedRecipesArrayAdapter(this, recipes);

        listView.setAdapter(adapter);
    }


    private void showStuff()
    {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (!(Intent.ACTION_VIEW.equals(action)))
        {
            throw new RuntimeException("Action " + action + " doesn't match expected VIEW action");
        }

        if (!("application/zip".equals(type)))
        {
            throw new RuntimeException("File type " + type + " doesn't match expected Zip file");
        }

        Uri str = intent.getData();

        File zip = new File(intent.getData().getPath());

        /*
        String[] fileNamesInZip;

        try
        {
            fileNamesInZip = readFileNames(zip);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
*/

        try
        {
            writeRecipeFilesToStagingFolder(zip);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

/*
        String names = "";

        for (int i = 0; i < fileNamesInZip.length; ++i)
        {
            names += fileNamesInZip[i] + ", ";
        }
*/
        FrameLayout view = (FrameLayout)this.findViewById(R.id.container);

        TextView text = new TextView(this);
        text.setText("Mike: " + "done");
        view.addView(text);


        /*
        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }
        */
    }

    private String[] readFileNames(File zipFile) throws IOException
    {
        InputStream inputStream = new FileInputStream(zipFile);

        ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(inputStream));

        ZipEntry zipEntry;

        ArrayList<String> fileNames = new ArrayList<String>();

        while ((zipEntry = zipStream.getNextEntry()) != null)
        {
            fileNames.add(zipEntry.getName());

            zipStream.closeEntry();
        }

        zipStream.close();

        return (String[])fileNames.toArray(new String[0]);
    }

    private void writeRecipeFilesToStagingFolder(File zipFile) throws IOException
    {
        // Clear out other stuff

        File stagingFolder = RecipesLoader.GetStagingFolder();

        //LoadRecipesActivity.deleteFilesFromFolder(stagingFolder);

        // Extract zip file

        InputStream inputStream = new FileInputStream(zipFile);

        ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(inputStream));

        ZipEntry zipEntry;

        while ((zipEntry = zipStream.getNextEntry()) != null)
        {
            String fileName = zipEntry.getName();

            // Skip non-recipe related files
            if (!(RecipesLoader.IsImageFile(fileName) ||
                RecipesLoader.endsWith(fileName, ".xml", true)))
            {
                continue;
            }

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
        // does something very interesting
        String x= ";";
    }

    public void cancelClick(View v) {
        // does something very interesting
        String x= ";";
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.load_recipes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public class StagedRecipe
    {
        public String RecipeTitle;
        public String ImageFileName; // Null if no image.
        public Boolean AlreadyExists;
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

            if (data.AlreadyExists)
            {
                displayText += " (will overwrite)";
            }

            textView.setText(displayText);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);  // Scalable pixels

            return textView;
        }
    }
}
