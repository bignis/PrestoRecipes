package com.bignis.prestocookbook.database;

import android.content.Context;
import android.database.sqlite.*;

public class RecipeDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PrestoRecipes.db";
        
    private static final String CREATE_RECIPES_TABLE =
	    "CREATE TABLE Recipes" +
	    "(" +
	    "Id INTEGER NOT NULL PRIMARY KEY," +
	    "Title TEXT NOT NULL collate nocase," +
	    "Notes TEXT collate nocase," +
	    "Xml TEXT NOT NULL," +
	    "XmlHash TEXT NOT NULL," +
	    "XmlFileName TEXT collate nocase," +
	    "Image BLOB," +
	    "ImageHash TEXT," +
	    "ImageFileName TEXT collate nocase," +
	    "Category TEXT collate nocase," +
	    "LastUpdated DATETIME NOT NULL," +
	    "UNIQUE (Id)," +
	    "UNIQUE (XmlFileName)" +  // XmlFileName is the identifier to figure out what Recipe an Image is associated with when loading data.
	    ");";

    
    private static final String DROP_RECIPES_TABLE =
    "DROP TABLE IF EXISTS Recipes";
    
    public RecipeDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_RECIPES_TABLE);
    }
    
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(DROP_RECIPES_TABLE);
        onCreate(db);
    }
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
