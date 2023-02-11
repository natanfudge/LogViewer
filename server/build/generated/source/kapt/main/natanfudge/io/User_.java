
package natanfudge.io;

import io.objectbox.EntityInfo;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.internal.CursorFactory;
import io.objectbox.internal.IdGetter;
import natanfudge.io.UserCursor.Factory;

// THIS CODE IS GENERATED BY ObjectBox, DO NOT EDIT.

/**
 * Properties for entity "User". Can be used for QueryBuilder and for referencing DB names.
 */
public final class User_ implements EntityInfo<User> {

    // Leading underscores for static constants to avoid naming conflicts with property names

    public static final String __ENTITY_NAME = "User";

    public static final int __ENTITY_ID = 1;

    public static final Class<User> __ENTITY_CLASS = User.class;

    public static final String __DB_NAME = "User";

    public static final CursorFactory<User> __CURSOR_FACTORY = new Factory();

    @Internal
    static final UserIdGetter __ID_GETTER = new UserIdGetter();

    public final static User_ __INSTANCE = new User_();

    public final static io.objectbox.Property<User> id =
        new io.objectbox.Property<>(__INSTANCE, 0, 1, long.class, "id", true, "id");

    public final static io.objectbox.Property<User> name =
        new io.objectbox.Property<>(__INSTANCE, 1, 2, String.class, "name");

    @SuppressWarnings("unchecked")
    public final static io.objectbox.Property<User>[] __ALL_PROPERTIES = new io.objectbox.Property[]{
        id,
        name
    };

    public final static io.objectbox.Property<User> __ID_PROPERTY = id;

    @Override
    public String getEntityName() {
        return __ENTITY_NAME;
    }

    @Override
    public int getEntityId() {
        return __ENTITY_ID;
    }

    @Override
    public Class<User> getEntityClass() {
        return __ENTITY_CLASS;
    }

    @Override
    public String getDbName() {
        return __DB_NAME;
    }

    @Override
    public io.objectbox.Property<User>[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public io.objectbox.Property<User> getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public IdGetter<User> getIdGetter() {
        return __ID_GETTER;
    }

    @Override
    public CursorFactory<User> getCursorFactory() {
        return __CURSOR_FACTORY;
    }

    @Internal
    static final class UserIdGetter implements IdGetter<User> {
        @Override
        public long getId(User object) {
            return object.getId();
        }
    }

}