package database;

public class SupabaseDBCreator extends DBCreator {
    @Override
    public DBStrategy createDatabase() {
        return new DBService();
    }
}
