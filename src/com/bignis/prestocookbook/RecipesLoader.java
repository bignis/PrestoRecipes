package com.bignis.prestocookbook;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.Adler32;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

public class RecipesLoader {
	public static String LoadRecipes(Context context)
	{
		if (context == null)
		{
			throw new RuntimeException("context cannot be null");
		}
		
		String message = "";
		
		{
			File[] xmlFiles = GetXmlFiles();
			
			File[] xmlFilesThatNeedLoading = null;
			
			try {
				xmlFilesThatNeedLoading = GetXmlFilesThatNeedLoading(xmlFiles, context);
			
				LoadXmlFiles(xmlFilesThatNeedLoading, context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				return "Nothing was loaded due to an error";
			}
			
			message += Integer.toString(xmlFilesThatNeedLoading.length) + " recipes done loading";
		}
	
		
		{
			File[] imageFiles = GetImageFiles();
			
			File[] imageFilesThatNeedLoading = null;
			
			try {
				imageFilesThatNeedLoading = GetImageFilesThatNeedLoading(imageFiles, context);
			
				message += "\n" + Integer.toString(imageFilesThatNeedLoading.length) + " images detected to load";
				
				int imagesActuallyLoadedCount = LoadImageFiles(imageFilesThatNeedLoading, context);
				
				message += "\n" + Integer.toString(imagesActuallyLoadedCount) + " images successfully loaded";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
				return "Nothing was loaded due to an error";
			}
		}
		
		return message;
	}
	
	private static void LoadXmlFiles(File[] xmlFilesThatNeedLoading, Context context) throws Exception {
		
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
		
		for (File file : xmlFilesThatNeedLoading)
		{
			long fileXmlHash = GetChecksum(file);
			
			String xml = getStringFromFile(file);
			
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
							"SET Xml = '?', XmlHash = ?, LastUpdated = datetime('now')" +
							"WHERE Title = '?' and XmlFileName = '?'");
					
					update.bindString(1, xml);
					update.bindLong(2, fileXmlHash);
					update.bindString(3, recipe.Title);
					update.bindString(4, file.getName());
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
							"SET XmlFileName = '?', LastUpdated = datetime('now')" +
							"WHERE Title = '?' and XmlHash = '?'");
					
					update.bindString(1, file.getName());
					update.bindString(2, recipe.Title);
					update.bindLong(3, fileXmlHash);
					String debug = update.toString();
					update.execute();
						
					continue;
					
				}
			}
			
			// Brand new, insert.
			
			SQLiteStatement insert = db.compileStatement("INSERT INTO Recipes (Title, Xml, XmlHash, XmlFileName, LastUpdated) VALUES (?, ?, ?, ?, datetime('now'))");
			insert.bindString(1, recipe.Title);
			insert.bindString(2, xml);
			insert.bindLong(3, fileXmlHash);
			insert.bindString(4, file.getName());
			String debug = insert.toString();
			insert.execute();
		}
		
		dbHelper.close();
	}

	public static File[] GetXmlFiles()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			String foo = "";
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    String foo = "";
		} 
		
		File folder = Environment.getExternalStoragePublicDirectory("Presto Recipes");
		
		//File folder = new File("/Presto Recipes");
		
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String fileName) {
		        return fileName.endsWith(".xml");
		    }
		};
		
		File[] xmlFiles = folder.listFiles(filter);
		
		return xmlFiles;
	}
	
	public static File[] GetImageFiles()
	{
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			String foo = "";
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    String foo = "";
		} 
		
		File folder = Environment.getExternalStoragePublicDirectory("Presto Recipes");
		
		//File folder = new File("/Presto Recipes");
		
		FilenameFilter filter = new FilenameFilter() {
		    public boolean accept(File directory, String fileName) {
		        return fileName.endsWith(".jpg");
		    }
		};
		
		File[] imageFiles = folder.listFiles(filter);
		
		return imageFiles;
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
	
	private static int LoadImageFiles(File[] imageFilesThatNeedLoading, Context context) throws Exception {
		
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
		
		dbHelper.close();
		
		return imagesActuallyLoaded;
	}
	
	private static byte[] GetSizedImageData(File file) throws FileNotFoundException, IOException
	{
		final int maxAcceptableHeight = 300;
		
		// Read the file
		byte[] data = new byte[(int) file.length()];
		new FileInputStream(file).read(data);
		
		// Make it a bitmap
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
		
		if (bitmap.getHeight() <= maxAcceptableHeight)
		{
			return data;  // Fine as-is
		}
		
		double scaleFactor = (double)maxAcceptableHeight / (double)bitmap.getHeight();
		double newWidth = bitmap.getWidth() * scaleFactor;
		
		Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int)Math.floor(newWidth), maxAcceptableHeight, true);
		
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
		
		dbHelper.close();
		
		return (RecipeChecksumInfo[]) rcis.toArray(new RecipeChecksumInfo[0]); // http://docs.oracle.com/javase/1.4.2/docs/api/java/util/Collection.html#toArray%28java.lang.Object%5B%5D%29
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

	public static String getStringFromFile (File fl) throws Exception {
	    FileInputStream fin = new FileInputStream(fl);
	    String ret = convertStreamToString(fin);
	    //Make sure you close all streams.
	    fin.close();        
	    return ret;
	}
}


