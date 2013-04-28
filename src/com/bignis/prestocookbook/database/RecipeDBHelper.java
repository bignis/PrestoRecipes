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
    
    private static final String CREATE_CATEGORIES_TABLE =
    	    "CREATE TABLE Categories" +
    	    "(" +
    	    "Id INTEGER NOT NULL PRIMARY KEY," +
    	    "RecipeId INTEGER NOT NULL," +
    	    "Text TEXT" +
    	    ");";
    
    private static final String DROP_RECIPES_TABLE =
    "DROP TABLE IF EXISTS Recipes";
    
    private static final String DROP_CATEGORIES_TABLE =
    	    "DROP TABLE IF EXISTS Categories";

    public RecipeDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_RECIPES_TABLE);
        db.execSQL(CREATE_CATEGORIES_TABLE);
    }
    
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(DROP_RECIPES_TABLE);
        db.execSQL(DROP_CATEGORIES_TABLE);
        onCreate(db);
    }
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}