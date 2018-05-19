package com.bignis.prestocookbook;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.bignis.prestocookbook.database.RecipeDBHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipCreator {
    public static File createZipForRecipeFiles(Context context, int recipeId) throws Exception {

        RecipeDBHelper dbHelper = new RecipeDBHelper(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT Title, Xml, ImageOriginal FROM Recipes WHERE Id = " + Integer.valueOf(recipeId).toString();

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (!(cursor.moveToFirst())) {
                return null;
            }

            String title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));

            String sanitizedTitle = getSanitizedTitle(title);

            String xml = cursor.getString(cursor.getColumnIndexOrThrow("Xml"));

            byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow("ImageOriginal"));

            File tempFolder = context.getCacheDir();
            //File tempFolder = RecipesLoader.GetStagingFolder();

            File zipFile = new File(tempFolder.getPath() + "/" + sanitizedTitle + ".presto");
            zipFile.setReadable(true, false);  // mimic world_readable

            FileOutputStream fos = new FileOutputStream(zipFile);

            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            try {
                ZipEntry xmlEntry = new ZipEntry(sanitizedTitle + ".xml");
                zos.putNextEntry(xmlEntry);
                zos.write(xml.getBytes());
                zos.closeEntry();

                ////////
                if (image != null && image.length != 0) {

                    ZipEntry imageEntry = new ZipEntry(sanitizedTitle + ".jpg");  // Unsafe to assume .jpg, so someday inspect the byte stream for the proper extension
                    zos.putNextEntry(imageEntry);
                    zos.write(image);
                    zos.closeEntry();
                }

            } finally {
                zos.close();
            }

            return zipFile;
        }
        finally
        {
            cursor.close();
            db.close();
            dbHelper.close();
        }
    }

    public static void createZipBackupForAllRecipeFiles(Context context, OutputStream outputStream) throws Exception {
        RecipeDBHelper dbHelper = new RecipeDBHelper(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT Title, Xml, ImageOriginal FROM Recipes";

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (!(cursor.moveToFirst())) {
                return;
            }

            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outputStream));

            try {

                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));

                    Log.i("ZipCreator", "Backing up " + title);

                    String sanitizedTitle = getSanitizedTitle(title);

                    String xml = cursor.getString(cursor.getColumnIndexOrThrow("Xml"));

                    byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow("ImageOriginal"));

                    ZipEntry xmlEntry = new ZipEntry(sanitizedTitle + ".xml");
                    zos.putNextEntry(xmlEntry);
                    zos.write(xml.getBytes());
                    zos.closeEntry();

                    ////////
                    if (image != null && image.length != 0) {

                        ZipEntry imageEntry = new ZipEntry(sanitizedTitle + ".jpg");  // Unsafe to assume .jpg, so someday inspect the byte stream for the proper extension
                        zos.putNextEntry(imageEntry);
                        zos.write(image);
                        zos.closeEntry();
                    }
                } while (cursor.moveToNext());
            } finally {
                zos.close();
                outputStream.close();
            }
        }
        finally
        {
            cursor.close();
            db.close();
            dbHelper.close();
        }
    }

    @Deprecated
    public static File createZipBackupForAllRecipeFiles(Context context) throws Exception {

        RecipeDBHelper dbHelper = new RecipeDBHelper(context);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT Title, Xml, ImageOriginal FROM Recipes";

        Cursor cursor = db.rawQuery(query, null);

        try {
            if (!(cursor.moveToFirst())) {
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
            String currentDate = sdf.format(new Date());

            /* trying something new in 2018
            // Saving to downloads folder & showing up is impossible unfortunately
            // http://stackoverflow.com/questions/35117898/updating-android-downloads-folder-without-using-a-downloadmanager

            String saveFolderPath = RecipesLoader.GetDataFolder().getPath() + "/backups";
            File saveFolder = new File(saveFolderPath);

            if (!(saveFolder.exists()))
            {
                saveFolder.mkdirs();
                // throw new RuntimeException("Folder " + folder.toString() + " does not exist");
            }

            if (!(saveFolder.isDirectory()))
            {
                throw new RuntimeException("Folder " + saveFolder.toString() + " is not directory");
            }

            File zipFile = new File(saveFolderPath + "/AllRecipes-" + currentDate + ".presto");
            zipFile.setReadable(true, false);  // mimic world_readable
            */

            // https://developer.android.com/training/data-storage/files#java
            /*
            File zipFile = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_DOWNLOADS), "/AllRecipes-" + currentDate + ".presto");
                    */
            File zipFile = new File(context.getFilesDir(), "/AllRecipes-" + currentDate + ".presto");

            if (zipFile.exists()) {
                zipFile.delete();
            }

            if (!zipFile.getParentFile().mkdirs()) {  // .getParentFile() tries to create the folder (otherwise it'll create a folder ending with .presto)
                Log.e("ZipCreator", "Directory failed to create mgn");
            }

            Log.i("ZipCreator", "Preparing zip file to save to " + zipFile.getAbsolutePath());

            FileOutputStream fos = new FileOutputStream(zipFile);

            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

            try {

                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("Title"));

                    Log.i("ZipCreator", "Backing up " + title);

                    String sanitizedTitle = getSanitizedTitle(title);

                    String xml = cursor.getString(cursor.getColumnIndexOrThrow("Xml"));

                    byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow("ImageOriginal"));

                    ZipEntry xmlEntry = new ZipEntry(sanitizedTitle + ".xml");
                    zos.putNextEntry(xmlEntry);
                    zos.write(xml.getBytes());
                    zos.closeEntry();

                    ////////
                    if (image != null && image.length != 0) {

                        ZipEntry imageEntry = new ZipEntry(sanitizedTitle + ".jpg");  // Unsafe to assume .jpg, so someday inspect the byte stream for the proper extension
                        zos.putNextEntry(imageEntry);
                        zos.write(image);
                        zos.closeEntry();
                    }
                } while (cursor.moveToNext());
            } finally {
                zos.close();
            }

            return zipFile;
        }
        finally
        {
            cursor.close();
            db.close();
            dbHelper.close();
        }
    }

    private static String getSanitizedTitle(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\._]+", "_");
    }
}
