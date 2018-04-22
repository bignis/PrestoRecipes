package com.bignis.prestocookbook;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;


public class RecipesLoader {
	
	public static final String DATA_FOLDER_LOCATION = "sdcard/Presto Recipes";  // "sdcard" is what's exposed (not actually an SD Card) via the USB connection to a PC
    public static final String STAGING_FOLDER_LOCATION = "sdcard/Presto Recipes/Staging";  // "sdcard" is what's exposed (not actually an SD Card) via the USB connection to a PC
	
	public static String LoadRecipes(Context context, RecipeLoadType typeOfLoad, RecipeLoadProgressListener progressListener) throws RecipeLoadException
	{
		if (context == null)
		{
			throw new RuntimeException("context cannot be null");
		}
		
		String message = "";

		File folderToLoad;

		switch (typeOfLoad) {
			case StagingFolderOnly:
				folderToLoad = GetStagingFolder();
				break;
			case DataFolderOnlyAndResetDatabase:
				folderToLoad = GetDataFolder();
				break;
			default:
				throw new RuntimeException("RecipeLoadType enum invalid");
		}

		if (typeOfLoad == RecipeLoadType.DataFolderOnlyAndResetDatabase) {
			RecipesLoader.ResetDatabase(context);  // Do this first!
		}

		// Need Maps to associate xml files and images with each other if they have the same name.

		Map<String, File> xmlFilesByName = new HashMap<String, File>();
		Map<String, File> imageFilesByName = new HashMap<String, File>();

		for (File xmlFile : GetXmlFiles(folderToLoad)) {
			xmlFilesByName.put(RecipesLoader.getFileNameWithoutExtension(xmlFile.getName()), xmlFile);
		}

		for (File imageFile : GetImageFiles(folderToLoad)) {
			imageFilesByName.put(RecipesLoader.getFileNameWithoutExtension(imageFile.getName()), imageFile);
		}

		// We'll never load Images by themselves, they'll always be an accompanying Recipe XML file

		if (xmlFilesByName.size() == 0)
		{
			return "No recipes available to load";
		}

		RecipeDBHelper dbHelper = new RecipeDBHelper(context);

		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int loadedCount = 0;

		for (String xmlFileNameWithoutExtension : xmlFilesByName.keySet())
		{
			if (progressListener != null)
			{
				progressListener.progressUpdate("Loading " + (loadedCount + 1) + " of " + xmlFilesByName.size() + " recipes");
			}

			File xmlFile = xmlFilesByName.get(xmlFileNameWithoutExtension);

			String xml = getStringFromFile(xmlFile);

			if (xml == null || xml.length() == 0)
			{
				throw new RecipeLoadException("The file " + xmlFile.getName() + " was empty and cannot be loaded");
			}

			byte[] imageOriginal = null;
			byte[] imageResized = null;

			File imageFile = GetImageFile(folderToLoad, xmlFileNameWithoutExtension);

			if (imageFile != null) {
				try {
					imageOriginal = RecipesLoader.GetImageData(imageFile);
					imageResized = RecipesLoader.GetSizedImageData(imageFile);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}

			Pair<Integer,Long> recipeReplacementInfo = null;

			if (typeOfLoad == RecipeLoadType.StagingFolderOnly) {
				recipeReplacementInfo = RecipesLoader.getRecipeReplacementInfoFromStagingFolder();

				if (recipeReplacementInfo != null) {
					Recipe recipeInDatabase = Recipe.getFromDatabase(recipeReplacementInfo.first, context);

					if (recipeInDatabase == null || recipeInDatabase.XmlHash != recipeReplacementInfo.second) {
						recipeReplacementInfo = null;  // Can't find the recipe in db or it's changed from the one we want to modify - insert this as new
					}
				}
			}

			if (recipeReplacementInfo != null) {

				try {
					Recipe recipe = RecipeParser.ParseFromXmlString(xml);

					SQLiteStatement update = db.compileStatement(
							"UPDATE Recipes " +
									"SET Title = ?, Category = ?, Xml = ?, Image = ?, ImageOriginal = ?, LastUpdated = datetime('now')" +
									"WHERE Id = ?");

					update.bindString(1, recipe.Title);
					update.bindString(2, recipe.Category);
					update.bindString(3, xml);

					if (imageResized != null) {
						update.bindBlob(4, imageResized);
					}
					else {
						update.bindNull(4);
					}

					if (imageOriginal != null) {
						update.bindBlob(5, imageOriginal);
					}
					else {
						update.bindNull(5);
					}
					update.bindLong(6, recipeReplacementInfo.first);
					String debug = update.toString();

					update.execute();
				} catch (Exception ex) {
					db.close();
					dbHelper.close();
					return "Failed to load " + xmlFileNameWithoutExtension + ": " + ex.getMessage();
				}
			}
			else {

				// Brand new, insert.

				try {
					Recipe recipe = RecipeParser.ParseFromXmlString(xml);

					SQLiteStatement insert = db.compileStatement("INSERT INTO Recipes (Title, Category, Xml, Image, ImageOriginal, LastUpdated) VALUES (?, ?, ?, ?, ?, datetime('now'))");
					insert.bindString(1, recipe.Title);
					insert.bindString(2, recipe.Category);
					insert.bindString(3, xml);

					if (imageResized != null) {
						insert.bindBlob(4, imageResized);
					}
					else {
						insert.bindNull(4);
					}

					if (imageOriginal != null) {
						insert.bindBlob(5, imageOriginal);
					}
					else {
						insert.bindNull(5);
					}

					//String debug = insert.toString();
					insert.execute();
				} catch (Exception ex) {
					db.close();
					dbHelper.close();
					return "Failed to load " + xmlFileNameWithoutExtension + ": " + ex.getMessage();
				}
			}

			loadedCount++;

			// Clean up files from Staging if it's loaded successfully
			if (typeOfLoad == RecipeLoadType.StagingFolderOnly) {

				// Delete image, .xml, and '.replacement' marker file
				final String fileNamePrefix = xmlFileNameWithoutExtension + ".";

				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File directory, String fileName) {
						return fileName.startsWith(fileNamePrefix);
					}
				};

				for (File file : GetStagingFolder().listFiles(filter)) {
					file.delete();
				}
			}
		}

		db.close();
		dbHelper.close();

		return xmlFilesByName.size() + " recipes successfuly loaded";
	}

	private static void ResetDatabase(Context context) {
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		dbHelper.onUpgrade(db, 1, 1);  // Triggers drop / recreate
		db.close();
		dbHelper.close();
	}
	
	private static void LoadXmlFiles(File[] xmlFilesThatNeedLoading, Context context, RecipeLoadProgressListener progressListener) throws Exception {
		
		if (xmlFilesThatNeedLoading.length == 0)
		{
			return;
		}
		
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		/*
		 	    "CREATE TABLE Recipes" +
	    "(" +
	    "Id INTEGER NOT NULL PRIMARY KEY," +
	    "Title TEXT NOT NULL," +
	    "Notes TEXT," +
	    "Xml TEXT NOT NULL," +
	    "XmlHash TEXT NOT NULL," +
	    "XmlFileName TEXT," +
	    "Image BLOB," +
	    "ImageHash TEXT," +
	    "ImageFileName TEXT," +
	    "LastUpdated DATETIME NOT NULL," +
	    "UNIQUE (Id)" +
	    ");";
		 */
		
		int xmlFilesActuallyLoaded = 0;
		
		for (File file : xmlFilesThatNeedLoading)
		{
			if (progressListener != null)
			{
				progressListener.progressUpdate("Loading " + (xmlFilesActuallyLoaded + 1) + " of " + xmlFilesThatNeedLoading.length + " recipes");
			}
			
			long fileXmlHash = GetChecksum(file);
			
			String xml = getStringFromFile(file);
			
			if (xml == null || xml.length() == 0)				
			{
				throw new RecipeLoadException("The file " + file.getName() + " was empty and cannot be loaded");
			}
			
			Recipe recipe = RecipeParser.ParseFromXmlString(xml);
			
			{
				String countSql = String.format("SELECT COUNT(*) FROM Recipes WHERE Title = %s AND XmlFileName = %s", 
						DatabaseUtils.sqlEscapeString(recipe.Title),
						DatabaseUtils.sqlEscapeString(file.getName()));
				
			    SQLiteStatement statement = db.compileStatement(countSql);
			    long existingRowCount = statement.simpleQueryForLong();
				
				if (existingRowCount == 1)
				{
					// The Title and File Name haven't changed, so the Content must have changed...
					
					SQLiteStatement update = db.compileStatement(
							"UPDATE Recipes " + 
							"SET Xml = ?, XmlHash = ?, Category = ?, LastUpdated = datetime('now')" +
							"WHERE Title = ? and XmlFileName = ?");
					
					update.bindString(1, xml);
					update.bindLong(2, fileXmlHash);
					update.bindString(3, recipe.Category);
					update.bindString(4, recipe.Title);
					update.bindString(5, file.getName());
					String debug = update.toString();
					update.execute();
							
					continue;
					
				}
			}

			{				
				String countSql = String.format("SELECT COUNT(*) FROM Recipes WHERE Title = %s AND XmlHash = %d", 
						DatabaseUtils.sqlEscapeString(recipe.Title),
						fileXmlHash);
				
			    SQLiteStatement statement = db.compileStatement(countSql);
			    long existingRowCount = statement.simpleQueryForLong();
				
				if (existingRowCount == 1)
				{
					// The Content (via Hash) and Title haven't changed, so the File Name must have changed...
					
					SQLiteStatement update = db.compileStatement(
							"UPDATE Recipes " + 
							"SET XmlFileName = ?, Category = ?, LastUpdated = datetime('now')" +
							"WHERE Title = ? and XmlHash = ?");
					
					update.bindString(1, file.getName());
					update.bindString(2, recipe.Category);
					update.bindString(3, recipe.Title);
					update.bindLong(4, fileXmlHash);
					String debug = update.toString();
					update.execute();
						
					continue;
					
				}
			}
			
			// Brand new, insert.
			
			SQLiteStatement insert = db.compileStatement("INSERT INTO Recipes (Title, Category, Xml, XmlHash, XmlFileName, LastUpdated) VALUES (?, ?, ?, ?, ?, datetime('now'))");
			insert.bindString(1, recipe.Title);
			insert.bindString(2, recipe.Category);
			insert.bindString(3, xml);
			insert.bindLong(4, fileXmlHash);
			insert.bindString(5, file.getName());
			String debug = insert.toString();
			insert.execute();
		}
		
		db.close();
		dbHelper.close();
	}

    public static File[] GetXmlFiles(File folder) {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return RecipesLoader.endsWith(fileName, ".xml", true);
            }
        };

        File[] xmlFiles = folder.listFiles(filter);

        if (xmlFiles == null)
        {
            return new File[0];
        }

        return xmlFiles;
    }

    private static File[] GetImageFiles(File folder) {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return RecipesLoader.IsImageFile(fileName);
            }
        };

        File[] imageFiles = folder.listFiles(filter);

        if (imageFiles == null)
        {
            return new File[0];
        }

        return imageFiles;
    }

    public static boolean IsImageFile(String fileName)
    {
        return RecipesLoader.endsWith(fileName, ".jpg", true)
                || RecipesLoader.endsWith(fileName, ".png", true);
    }

	public static File GetImageFile(File folder, String fileNameWithoutExtension) {
		final String[] imageExtensions = new String[] { ".jpg", ".png", ".gif" };

		File result = null;

		for (String extension : imageExtensions) {
			String path = folder.getPath() + "/" + fileNameWithoutExtension + extension;
			File possibleImage = new File(path);

			if (possibleImage.exists()) {
				if (result != null) {
					throw new RuntimeException("Unexpected: Two images were found that start with " + fileNameWithoutExtension);
				}

				result = possibleImage;
			}
		}

		return result;
	}
	
	public static File GetDataFolder()
	{
		/*  // SD card
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			String foo = "";
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    String foo = "";
		} 
		
		File folder = Environment.getExternalStoragePublicDirectory("Presto Recipes");
		*/
		File folder = new File(DATA_FOLDER_LOCATION);
		
		if (!(folder.exists()))
		{
			folder.mkdirs();
			// throw new RuntimeException("Folder " + folder.toString() + " does not exist");
		}
		
		if (!(folder.isDirectory()))
		{
			throw new RuntimeException("Folder " + folder.toString() + " is not directory");
		}
		
		return folder;
	}

    public static File GetStagingFolder()
    {
		/*  // SD card
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			String foo = "";
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    String foo = "";
		}

		File folder = Environment.getExternalStoragePublicDirectory("Presto Recipes");
		*/
        File folder = new File(STAGING_FOLDER_LOCATION);
		Log.e("mgn", "folder " + folder.exists());
		if (!(folder.exists()))
        {
            boolean success = folder.mkdirs();

			if (!(success)) {
				throw new RuntimeException("Tried and failed to create foldre: " + folder.toString());
			}
        }

        if (!(folder.isDirectory()))
        {
            throw new RuntimeException("Folder " + folder.toString() + " is not directory");
        }

        return folder;
    }
	
	public static File[] GetImageFilesThatNeedLoading(File[] imageFiles, Context context) throws FileNotFoundException, IOException
	{
		if (imageFiles.length == 0)
		{
			return new File[0];
		}
	
		ArrayList<File> filesThatNeedLoading = new ArrayList<File>();
		
		RecipeChecksumInfo[] rcis = ReadRecipesFromDatabase(context);
	
		HashSet<Long> checksumsInDatabase = new HashSet<Long>();
		
		for (RecipeChecksumInfo rci : rcis)
		{
			checksumsInDatabase.add(rci.ImageHash);
		}
		
		for (File imageFile : imageFiles)
		{
			long checksum = GetChecksum(imageFile);
			
			if (checksumsInDatabase.contains(checksum))
			{
				continue;
			}
			
			filesThatNeedLoading.add(imageFile);
		}
		
		return (File[]) filesThatNeedLoading.toArray(new File[0]); //http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
	}

	private static int LoadImageFiles(File[] imageFilesThatNeedLoading, Context context, RecipeLoadProgressListener progressListener) throws Exception {

		if (imageFilesThatNeedLoading.length == 0)
		{
			return 0;
		}

		RecipeDBHelper dbHelper = new RecipeDBHelper(context);

		SQLiteDatabase db = dbHelper.getWritableDatabase();

		/*
		 	    "CREATE TABLE Recipes" +
	    "(" +
	    "Id INTEGER NOT NULL PRIMARY KEY," +
	    "Title TEXT NOT NULL," +
	    "Notes TEXT," +
	    "Xml TEXT NOT NULL," +
	    "XmlHash TEXT NOT NULL," +
	    "XmlFileName TEXT," +
	    "Image BLOB," +
	    "ImageHash TEXT," +
	    "ImageFileName TEXT," +
	    "LastUpdated DATETIME NOT NULL," +
	    "UNIQUE (Id)" +
	    ");";
		 */

		int imagesActuallyLoaded = 0;

		for (File file : imageFilesThatNeedLoading)
		{
			if (progressListener != null)
			{
				progressListener.progressUpdate("Loading " + (imagesActuallyLoaded + 1) + " of " + imageFilesThatNeedLoading.length + " recipe pictures");
			}

			String correspondingXmlFileName = file.getName().substring(0, file.getName().lastIndexOf('.')) + ".xml";

			String countSql = String.format("SELECT COUNT(*) FROM Recipes WHERE XmlFileName = %s",
					DatabaseUtils.sqlEscapeString(correspondingXmlFileName));

		    SQLiteStatement statement = db.compileStatement(countSql);
		    long existingRowCount = statement.simpleQueryForLong();

		    if (existingRowCount != 1)
		    {
		    	// Images are guaranteed to be an "update" since the Recipe XML must have been loaded already

		    	// The only thing weird might be an image (mis-named, say) without a corresponding recipe.  In that case don't load it.
		    	continue;
		    }

		    imagesActuallyLoaded++; // Since there's an existing row, increment the fact we're actually going to load an image

			long fileHash = GetChecksum(file);

			byte[] data = GetSizedImageData(file);

			SQLiteStatement update = db.compileStatement(
					"UPDATE Recipes " +
					"SET Image = ?, ImageHash = ?, LastUpdated = datetime('now')" +
					"WHERE XmlFileName = ?");

			update.bindBlob(1, data);
			update.bindLong(2, fileHash);
			update.bindString(3, correspondingXmlFileName);
			String debug = update.toString();
			update.execute();
		}

		db.close();
		dbHelper.close();

		return imagesActuallyLoaded;
	}

	private static byte[] GetImageData(File file) throws FileNotFoundException, IOException {
		// Read the file
		byte[] data = new byte[(int) file.length()];
		new FileInputStream(file).read(data);

		return data;
	}

	private static byte[] GetSizedImageData(File file) throws FileNotFoundException, IOException
	{
		final int maxAcceptableHeight = 300;
		
		// Read the file
		byte[] data = GetImageData(file);
		
		// Make it a bitmap
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		
		if (bitmap.getHeight() <= maxAcceptableHeight)
		{
			return data;  // Fine as-is
		}
		
		double scaleFactor = (double)maxAcceptableHeight / (double)bitmap.getHeight();
		double newWidth = bitmap.getWidth() * scaleFactor;
		
		Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int) Math.floor(newWidth), maxAcceptableHeight, true);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		
		return stream.toByteArray();
	}
	
	public static File[] GetXmlFilesThatNeedLoading(File[] xmlFiles, Context context) throws FileNotFoundException, IOException
	{
		if (xmlFiles.length == 0)
		{
			return new File[0];
		}
	
		ArrayList<File> filesThatNeedLoading = new ArrayList<File>();
		
		RecipeChecksumInfo[] rcis = ReadRecipesFromDatabase(context);
	
		HashSet<Long> checksumsInDatabase = new HashSet<Long>();
		
		for (RecipeChecksumInfo rci : rcis)
		{
			checksumsInDatabase.add(rci.XmlHash);
		}
		
		for (File xmlFile : xmlFiles)
		{
			long checksum = GetChecksum(xmlFile);
			
			if (checksumsInDatabase.contains(checksum))
			{
				continue;
			}
			
			filesThatNeedLoading.add(xmlFile);
		}
		
		return (File[]) filesThatNeedLoading.toArray(new File[0]); //http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
	}
	
	private static long GetChecksum(File file) throws FileNotFoundException, IOException
	{
		Adler32 adler = new Adler32();
		byte[] data = new byte[(int) file.length()];
		new FileInputStream(file).read(data);
		
		adler.update(data);
		
		return adler.getValue();
	}
	
	private static RecipeChecksumInfo[] ReadRecipesFromDatabase(Context context)
	{
		RecipeDBHelper dbHelper = new RecipeDBHelper(context);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		
		String[] projection = {
			    "Id",
			    "Title",
			    "XmlFileName",
			    "XmlHash",
			    "ImageFileName",
			    "ImageHash"
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
		
		try
		{
			if (!(cursor.moveToFirst()))
			{			
				return new RecipeChecksumInfo[0];
			}
			
			ArrayList<RecipeChecksumInfo> rcis = new ArrayList<RecipeChecksumInfo>();
			
			do {
				RecipeChecksumInfo rci = new RecipeChecksumInfo();
				rcis.add(rci);
				
				rci.Id = cursor.getInt(cursor.getColumnIndexOrThrow("Id"));
				rci.Title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));
				rci.XmlFileName = cursor.getString(cursor.getColumnIndexOrThrow("XmlFileName"));
				rci.XmlHash = cursor.getLong(cursor.getColumnIndexOrThrow("XmlHash"));
				rci.ImageFileName = cursor.getString(cursor.getColumnIndexOrThrow("ImageFileName"));
				rci.ImageHash = cursor.getLong(cursor.getColumnIndexOrThrow("ImageHash"));
	
	        } while (cursor.moveToNext());
			
			return (RecipeChecksumInfo[]) rcis.toArray(new RecipeChecksumInfo[0]); // http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
		}
		finally
		{
			cursor.close();
			db.close();
			dbHelper.close();
		}
	}

	// <id, xmlHash>
	public static Pair<Integer, Long> getRecipeReplacementInfoFromStagingFolder() {
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return RecipesLoader.endsWith(fileName, ".replacement", true);
			}
		};

		File[] filesSignalingRecipeReplacement = RecipesLoader.GetStagingFolder().listFiles(filter);

		if (filesSignalingRecipeReplacement.length > 1) {
			throw new RuntimeException("Unexpected, multiple 'replacement' files in the folder, only 1 is ever expected");
		}

		if (filesSignalingRecipeReplacement.length == 0) {
			return null;
		}

		// Ex: "69-420.replacement" (id-hash.replacement)
		String[] parts = filesSignalingRecipeReplacement[0].getName().split("\\.")[0].split("-");

		int recipeToReplaceId = Integer.parseInt(parts[0]);
		long recipeToReplaceHash = Long.parseLong(parts[1]);

		return new Pair<Integer, Long>(recipeToReplaceId, recipeToReplaceHash);
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    return sb.toString();
	}

	public static String getStringFromFile (File fl) {
		try {
			FileInputStream fin = new FileInputStream(fl);
			String ret = convertStreamToString(fin);
			//Make sure you close all streams.
			fin.close();

			return ret;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String getFileNameWithoutExtension(String filename) {
		int pos = filename.lastIndexOf(".");
		if (pos > 0) {
			return filename.substring(0, pos);
		}

		return filename;  // No dot
	}
	
	  public static boolean endsWith(String str, String suffix, boolean ignoreCase) {
	      if (str == null || suffix == null) {
	          return (str == null && suffix == null);
	      }
	      if (suffix.length() > str.length()) {
	          return false;
	      }
	      int strOffset = str.length() - suffix.length();
	      return str.regionMatches(ignoreCase, strOffset, suffix, 0, suffix.length());
	  }
}


