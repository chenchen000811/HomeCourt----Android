package com.tencent.yolov8ncnn;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ResultDatabase_Impl extends ResultDatabase {
  private volatile ResultDAO _resultDAO;

  private volatile CircleDao _circleDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Result` (`result_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT, `made` INTEGER NOT NULL, `attempt` INTEGER NOT NULL, `percent` REAL NOT NULL, `mode` INTEGER NOT NULL, `circles` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `Circle` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `result_id` INTEGER NOT NULL, `x` REAL NOT NULL, `y` REAL NOT NULL, `radius` REAL NOT NULL, `color` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f6700d8266341b0b9e719f2483fcf5d0')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `Result`");
        db.execSQL("DROP TABLE IF EXISTS `Circle`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsResult = new HashMap<String, TableInfo.Column>(7);
        _columnsResult.put("result_id", new TableInfo.Column("result_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("date", new TableInfo.Column("date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("made", new TableInfo.Column("made", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("attempt", new TableInfo.Column("attempt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("percent", new TableInfo.Column("percent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("mode", new TableInfo.Column("mode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsResult.put("circles", new TableInfo.Column("circles", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysResult = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesResult = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoResult = new TableInfo("Result", _columnsResult, _foreignKeysResult, _indicesResult);
        final TableInfo _existingResult = TableInfo.read(db, "Result");
        if (!_infoResult.equals(_existingResult)) {
          return new RoomOpenHelper.ValidationResult(false, "Result(com.tencent.yolov8ncnn.Result).\n"
                  + " Expected:\n" + _infoResult + "\n"
                  + " Found:\n" + _existingResult);
        }
        final HashMap<String, TableInfo.Column> _columnsCircle = new HashMap<String, TableInfo.Column>(6);
        _columnsCircle.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCircle.put("result_id", new TableInfo.Column("result_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCircle.put("x", new TableInfo.Column("x", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCircle.put("y", new TableInfo.Column("y", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCircle.put("radius", new TableInfo.Column("radius", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCircle.put("color", new TableInfo.Column("color", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCircle = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCircle = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCircle = new TableInfo("Circle", _columnsCircle, _foreignKeysCircle, _indicesCircle);
        final TableInfo _existingCircle = TableInfo.read(db, "Circle");
        if (!_infoCircle.equals(_existingCircle)) {
          return new RoomOpenHelper.ValidationResult(false, "Circle(com.tencent.yolov8ncnn.Circle).\n"
                  + " Expected:\n" + _infoCircle + "\n"
                  + " Found:\n" + _existingCircle);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f6700d8266341b0b9e719f2483fcf5d0", "fc3fee1ffad76127f3f43ba43b0f4ac0");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "Result","Circle");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `Result`");
      _db.execSQL("DELETE FROM `Circle`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ResultDAO.class, ResultDAO_Impl.getRequiredConverters());
    _typeConvertersMap.put(CircleDao.class, CircleDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ResultDAO getResultDAO() {
    if (_resultDAO != null) {
      return _resultDAO;
    } else {
      synchronized(this) {
        if(_resultDAO == null) {
          _resultDAO = new ResultDAO_Impl(this);
        }
        return _resultDAO;
      }
    }
  }

  @Override
  public CircleDao circleDao() {
    if (_circleDao != null) {
      return _circleDao;
    } else {
      synchronized(this) {
        if(_circleDao == null) {
          _circleDao = new CircleDao_Impl(this);
        }
        return _circleDao;
      }
    }
  }
}
