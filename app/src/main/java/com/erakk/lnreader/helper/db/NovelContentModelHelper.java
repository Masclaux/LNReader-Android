/*
 * NovelContentModel
 * Nested object : PageModel, lazy loading via NovelsDao
 */
package com.erakk.lnreader.helper.db;

import java.io.File;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.dao.NovelsDao;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

public class NovelContentModelHelper {

	private static final String TAG = NovelContentModelHelper.class.toString();
	private static DBHelper helper = NovelsDao.getInstance().getDBHelper();

	// New column should be appended as the last column
	public static final String DATABASE_CREATE_NOVEL_CONTENT = "create table if not exists " + DBHelper.TABLE_NOVEL_CONTENT + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
			+ DBHelper.COLUMN_CONTENT + " text not null, " // 1
			+ DBHelper.COLUMN_PAGE + " text unique not null, " // 2
			+ DBHelper.COLUMN_LAST_X + " integer, " // 3
			+ DBHelper.COLUMN_LAST_Y + " integer, " // 4
			+ DBHelper.COLUMN_ZOOM + " double, " // 5
			+ DBHelper.COLUMN_LAST_UPDATE + " integer, " // 6
			+ DBHelper.COLUMN_LAST_CHECK + " integer," // 7
		    + DBHelper.COLUMN_CURRENT_PAGE + " integer);"; // 8

	public static NovelContentModel cursorToNovelContent(Cursor cursor) {
		NovelContentModel content = new NovelContentModel();
		content.setId(cursor.getInt(0));
		content.setContent(cursor.getString(1));
		content.setPage(cursor.getString(2));
		content.setLastXScroll(cursor.getInt(3));
		content.setLastYScroll(cursor.getInt(4));
		content.setLastZoom(cursor.getDouble(5));
		content.setLastUpdate(new Date(cursor.getLong(6) * 1000));
		content.setLastCheck(new Date(cursor.getLong(7) * 1000));
        content.setCurrentPage(cursor.getInt(8));
		return content;
	}

	/*
	 * Query Stuff
	 */

	public static NovelContentModel getNovelContent(SQLiteDatabase db, String page) {
		// Log.d(TAG, "Selecting Novel Content: " + page);
		NovelContentModel content = null;

		Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_CONTENT + " where " + DBHelper.COLUMN_PAGE + " = ? ", new String[] { page });
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				content = cursorToNovelContent(cursor);
				// Log.d(TAG, "Found: " + content.getPage() + " id: " + content.getId());
				break;
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		if (content == null) {
			Log.w(TAG, "Not Found Novel Content: " + page);
		}
		return content;
	}

	/*
	 * Insert Stuff
	 */

	public static NovelContentModel insertNovelContent(SQLiteDatabase db, NovelContentModel content, PageModel page, boolean forceUpdateContent) throws Exception {
		ContentValues cv = new ContentValues();
		cv.put(DBHelper.COLUMN_PAGE, content.getPage());
		cv.put(DBHelper.COLUMN_ZOOM, "" + content.getLastZoom());
		cv.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));
        cv.put(DBHelper.COLUMN_CURRENT_PAGE, "" + content.getCurrentPage());

		NovelContentModel temp = getNovelContent(db, content.getPage());
		if (temp == null) {
			cv.put(DBHelper.COLUMN_CONTENT, content.getContent());
			cv.put(DBHelper.COLUMN_LAST_X, "" + content.getLastXScroll());
			cv.put(DBHelper.COLUMN_LAST_Y, "" + content.getLastYScroll());


			// Log.d(TAG, "Inserting Novel Content: " + content.getPage());
			if (content.getLastUpdate() == null)
				cv.put(DBHelper.COLUMN_LAST_UPDATE, 0);
			else
				cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));
			long id = helper.insertOrThrow(db, DBHelper.TABLE_NOVEL_CONTENT, null, cv);
			Log.i(TAG, "Novel Content Inserted, New id: " + id);
		} else {
			if (content.isUpdatingFromInternet()) {
				cv.put(DBHelper.COLUMN_LAST_X, "" + temp.getLastXScroll());
				cv.put(DBHelper.COLUMN_LAST_Y, "" + temp.getLastYScroll());
			} else {
				cv.put(DBHelper.COLUMN_LAST_X, "" + content.getLastXScroll());
				cv.put(DBHelper.COLUMN_LAST_Y, "" + content.getLastYScroll());
			}

			// skip updating existing content if the page/chapter already deleted.
			if (!page.isMissing() || forceUpdateContent) {
				Log.d(TAG, "Updating content for : " + content.getPage());
				cv.put(DBHelper.COLUMN_CONTENT, content.getContent());
			}

			// Log.d(TAG, "Updating Novel Content: " + content.getPage() + " id: " + temp.getId());
			if (content.getLastUpdate() == null)
				cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (temp.getLastUpdate().getTime() / 1000));
			else
				cv.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (content.getLastUpdate().getTime() / 1000));
			int result = helper.update(db, DBHelper.TABLE_NOVEL_CONTENT, cv, DBHelper.COLUMN_ID + " = ? ", new String[] { "" + temp.getId() });
			Log.i(TAG, "Novel Content:" + content.getPage() + " Updated, Affected Row: " + result);
		}

		// update the pageModel
		if (page == null) {
			page = content.getPageModel();
		}
		if (page != null) {
			page.setDownloaded(true);
			page = PageModelHelper.insertOrUpdatePageModel(db, page, false);
		}

		content = getNovelContent(db, content.getPage());
		return content;
	}

	/*
	 * Delete Stuff
	 */

	public static boolean deleteNovelContent(SQLiteDatabase db, NovelContentModel content) {
		if (content != null && content.getId() > 0) {
			int result = helper.delete(db, DBHelper.TABLE_NOVEL_CONTENT, DBHelper.COLUMN_ID + " = ?", new String[] { "" + content.getId() });
			Log.w(TAG, "NovelContent Deleted: " + result);
			return result > 0 ? true : false;
		}
		return false;
	}

	public static int deleteNovelContent(SQLiteDatabase db, PageModel ref) {
		if (ref != null && !Util.isStringNullOrEmpty(ref.getPage())) {

			if (ref.isExternal()) {
				String wacName = Util.getSavedWacName(ref.getPage());
				if (wacName != null) {
					File f = new File(wacName);
					boolean isDeleted = f.delete();
					if (isDeleted) {
						ref.setDownloaded(false);
						Log.w(TAG, "External NovelContent Deleted: " + wacName);
						return 1;
					}
				}
			}
			else {
				int result = helper.delete(db, DBHelper.TABLE_NOVEL_CONTENT, DBHelper.COLUMN_PAGE + " = ?", new String[] { "" + ref.getPage() });
				Log.w(TAG, "NovelContent Deleted: " + result);
				return result;
			}
		}
		return 0;
	}
}
