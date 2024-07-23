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
public final class ResultDAO_Impl implements ResultDAO {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Result> __insertionAdapterOfResult;

  private final EntityDeletionOrUpdateAdapter<Result> __deletionAdapterOfResult;

  private final EntityDeletionOrUpdateAdapter<Result> __updateAdapterOfResult;

  public ResultDAO_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfResult = new EntityInsertionAdapter<Result>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `Result` (`result_id`,`date`,`made`,`attempt`,`percent`,`mode`,`circles`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Result entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getMade());
        statement.bindLong(4, entity.getAttempt());
        statement.bindDouble(5, entity.getPercent());
        statement.bindLong(6, entity.getMode());
        final String _tmp = Converter.fromCircleList(entity.getCircles());
        if (_tmp == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp);
        }
      }
    };
    this.__deletionAdapterOfResult = new EntityDeletionOrUpdateAdapter<Result>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `Result` WHERE `result_id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Result entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfResult = new EntityDeletionOrUpdateAdapter<Result>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `Result` SET `result_id` = ?,`date` = ?,`made` = ?,`attempt` = ?,`percent` = ?,`mode` = ?,`circles` = ? WHERE `result_id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final Result entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDate() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDate());
        }
        statement.bindLong(3, entity.getMade());
        statement.bindLong(4, entity.getAttempt());
        statement.bindDouble(5, entity.getPercent());
        statement.bindLong(6, entity.getMode());
        final String _tmp = Converter.fromCircleList(entity.getCircles());
        if (_tmp == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp);
        }
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public void addResult(final Result result) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfResult.insert(result);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deletedResult(final Result result) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfResult.handle(result);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updatedResult(final Result result) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfResult.handle(result);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public List<Result> getAllResult() {
    final String _sql = "select * from result";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "result_id");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfMade = CursorUtil.getColumnIndexOrThrow(_cursor, "made");
      final int _cursorIndexOfAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "attempt");
      final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
      final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
      final int _cursorIndexOfCircles = CursorUtil.getColumnIndexOrThrow(_cursor, "circles");
      final List<Result> _result = new ArrayList<Result>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final Result _item;
        final int _tmpMade;
        _tmpMade = _cursor.getInt(_cursorIndexOfMade);
        final int _tmpAttempt;
        _tmpAttempt = _cursor.getInt(_cursorIndexOfAttempt);
        final float _tmpPercent;
        _tmpPercent = _cursor.getFloat(_cursorIndexOfPercent);
        final List<Circle> _tmpCircles;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfCircles)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfCircles);
        }
        _tmpCircles = Converter.toCircleList(_tmp);
        _item = new Result(_tmpMade,_tmpAttempt,_tmpPercent,_tmpCircles);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _item.date = null;
        } else {
          _item.date = _cursor.getString(_cursorIndexOfDate);
        }
        final int _tmpMode;
        _tmpMode = _cursor.getInt(_cursorIndexOfMode);
        _item.setMode(_tmpMode);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Result getResult(final int result_id) {
    final String _sql = "select * from result where result_id==?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, result_id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "result_id");
      final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
      final int _cursorIndexOfMade = CursorUtil.getColumnIndexOrThrow(_cursor, "made");
      final int _cursorIndexOfAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "attempt");
      final int _cursorIndexOfPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "percent");
      final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
      final int _cursorIndexOfCircles = CursorUtil.getColumnIndexOrThrow(_cursor, "circles");
      final Result _result;
      if (_cursor.moveToFirst()) {
        final int _tmpMade;
        _tmpMade = _cursor.getInt(_cursorIndexOfMade);
        final int _tmpAttempt;
        _tmpAttempt = _cursor.getInt(_cursorIndexOfAttempt);
        final float _tmpPercent;
        _tmpPercent = _cursor.getFloat(_cursorIndexOfPercent);
        final List<Circle> _tmpCircles;
        final String _tmp;
        if (_cursor.isNull(_cursorIndexOfCircles)) {
          _tmp = null;
        } else {
          _tmp = _cursor.getString(_cursorIndexOfCircles);
        }
        _tmpCircles = Converter.toCircleList(_tmp);
        _result = new Result(_tmpMade,_tmpAttempt,_tmpPercent,_tmpCircles);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfDate)) {
          _result.date = null;
        } else {
          _result.date = _cursor.getString(_cursorIndexOfDate);
        }
        final int _tmpMode;
        _tmpMode = _cursor.getInt(_cursorIndexOfMode);
        _result.setMode(_tmpMode);
      } else {
        _result = null;
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
