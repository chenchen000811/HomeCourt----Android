package com.tencent.yolov8ncnn;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class CircleDao_Impl implements CircleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Circle> __insertionAdapterOfCircle;

  private final EntityDeletionOrUpdateAdapter<Circle> __deletionAdapterOfCircle;

  private final EntityDeletionOrUpdateAdapter<Circle> __updateAdapterOfCircle;

  public CircleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCircle = new EntityInsertionAdapter<Circle>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `Circle` (`id`,`result_id`,`x`,`y`,`radius`,`color`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Circle entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getResultId());
        statement.bindDouble(3, entity.getX());
        statement.bindDouble(4, entity.getY());
        statement.bindDouble(5, entity.getRadius());
        statement.bindLong(6, entity.getColor());
      }
    };
    this.__deletionAdapterOfCircle = new EntityDeletionOrUpdateAdapter<Circle>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Circle` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Circle entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCircle = new EntityDeletionOrUpdateAdapter<Circle>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Circle` SET `id` = ?,`result_id` = ?,`x` = ?,`y` = ?,`radius` = ?,`color` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Circle entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getResultId());
        statement.bindDouble(3, entity.getX());
        statement.bindDouble(4, entity.getY());
        statement.bindDouble(5, entity.getRadius());
        statement.bindLong(6, entity.getColor());
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public void addCircle(final Circle circle) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCircle.insert(circle);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteCircle(final Circle circle) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfCircle.handle(circle);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateCircle(final Circle circle) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfCircle.handle(circle);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Circle> getCirclesForResult(final int resultId) {
    final String _sql = "select* from Circle where result_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, resultId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfResultId = CursorUtil.getColumnIndexOrThrow(_cursor, "result_id");
      final int _cursorIndexOfX = CursorUtil.getColumnIndexOrThrow(_cursor, "x");
      final int _cursorIndexOfY = CursorUtil.getColumnIndexOrThrow(_cursor, "y");
      final int _cursorIndexOfRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "radius");
      final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
      final List<Circle> _result = new ArrayList<Circle>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Circle _item;
        final int _tmpResultId;
        _tmpResultId = _cursor.getInt(_cursorIndexOfResultId);
        final float _tmpX;
        _tmpX = _cursor.getFloat(_cursorIndexOfX);
        final float _tmpY;
        _tmpY = _cursor.getFloat(_cursorIndexOfY);
        final float _tmpRadius;
        _tmpRadius = _cursor.getFloat(_cursorIndexOfRadius);
        final int _tmpColor;
        _tmpColor = _cursor.getInt(_cursorIndexOfColor);
        _item = new Circle(_tmpResultId,_tmpX,_tmpY,_tmpRadius,_tmpColor);
        final int _tmpId;
        _tmpId = _cursor.getInt(_cursorIndexOfId);
        _item.setId(_tmpId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
