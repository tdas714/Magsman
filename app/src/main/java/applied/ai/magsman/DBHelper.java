package applied.ai.magsman;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {

    //information of database
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "bookDB.db";
    public static final String TABLE_NAME = "Book";
    public static final String COLUMN_ID = "BookID";
    public static final String COLUMN_TITLE = "BookTitle";
    public static final String COLUMN_COVER = "BookCover";
    public static final String COLUMN_LASTPAGE = "BookLastpage";
    public static final String COLUMN_BOOKMARK = "BookBookmark";
    public static final String COLUMN_TOTALPAGE = "BookTotalpage";
    public static final String COLUMN_NUMPLAYED = "BookNumplayed";
    //initialize the database

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }
    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override

    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_TITLE + " TEXT, " + COLUMN_COVER + " TEXT, " + COLUMN_LASTPAGE + " INTEGER, " + COLUMN_BOOKMARK + " INTEGER, " + COLUMN_TOTALPAGE + " INTEGER, " + COLUMN_NUMPLAYED + " INTEGER )";
        db.execSQL(CREATE_TABLE);
    }

    @Override

    public void onUpgrade(SQLiteDatabase db, int i, int i1) {}

    public String loadBooks() {
        String result = "";
        String sep = "<Mags>";
        String query = "Select*FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            int result_0 = cursor.getInt(0);
            String result_1 = cursor.getString(1);
            String result_2 = cursor.getString(2);
            int result_3 = cursor.getInt(3);
            int result_4 = cursor.getInt(4);
            int result_5 = cursor.getInt(5);
            int result_6 = cursor.getInt(6);
            result += result_0+sep+result_1+sep+result_2+sep+result_3+sep+result_4+sep+result_5+sep+result_6+System.getProperty("line.separator");
        }
        cursor.close();
        db.close();
        return result;
    }
    public void addBook(Book student) {
        ContentValues values = new ContentValues();
//        values.put(COLUMN_ID, student.getID());
        values.put(COLUMN_TITLE, student.getTitle());
        values.put(COLUMN_COVER, student.getCover());
        values.put(COLUMN_LASTPAGE, student.getLastpage());
        values.put(COLUMN_BOOKMARK, student.getBookmark());
        values.put(COLUMN_TOTALPAGE, student.getTotalpage());
        values.put(COLUMN_NUMPLAYED, student.getNumplayed());
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
    }
    public Book findBook(String title) {
        String query = "Select * FROM " + TABLE_NAME + " WHERE " + COLUMN_TITLE + " = " + "'" + title + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Book book = new Book();
        if (cursor.moveToFirst()) {
            cursor.moveToFirst();
            book.setID(Integer.parseInt(cursor.getString(0)));
            book.setTitle(cursor.getString(1));
            book.setCover(cursor.getString(2));
            book.setLastpage(Integer.parseInt(cursor.getString(3)));
            book.setBookmark(Integer.parseInt(cursor.getString(4)));
            book.setTotalpage(Integer.parseInt(cursor.getString(5)));
            book.setNumplayed(Integer.parseInt(cursor.getString(6)));
            cursor.close();
        } else {
            book = null;
        }
        db.close();
        return book;
    }

    public boolean deleteBook(int ID) {
        boolean result = false;
        String query = "Select*FROM" + TABLE_NAME + "WHERE" + COLUMN_ID + "= '" + String.valueOf(ID) + "'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Book student = new Book();
        if (cursor.moveToFirst()) {
            student.setID(Integer.parseInt(cursor.getString(0)));
            db.delete(TABLE_NAME, COLUMN_ID + "=?",
                    new String[] {
                String.valueOf(student.getID())
            });
            cursor.close();
            result = true;
        }
        db.close();
        return result;
    }
    public boolean updateBook(Book book) {
        int ID = book.getID();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues args = new ContentValues();
        args.put(COLUMN_ID, ID);
        args.put(COLUMN_TITLE, book.getTitle());
        args.put(COLUMN_COVER, book.getCover());
        args.put(COLUMN_LASTPAGE, book.getLastpage());
        args.put(COLUMN_BOOKMARK, book.getBookmark());
        args.put(COLUMN_TOTALPAGE, book.getTotalpage());
        args.put(COLUMN_NUMPLAYED, book.getNumplayed());
        return db.update(TABLE_NAME, args, COLUMN_ID + "=" + ID, null) > 0;
    }
}
