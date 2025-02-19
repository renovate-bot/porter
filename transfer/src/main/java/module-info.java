module de.xab.porter.transfer {
    requires transitive de.xab.porter.api;
    requires de.xab.porter.common;
    requires java.sql;

    exports de.xab.porter.transfer.connection;
    exports de.xab.porter.transfer.channel;
    exports de.xab.porter.transfer.reader;
    exports de.xab.porter.transfer.writer;

    opens de.xab.porter.transfer.channel to de.xab.porter.common;
}