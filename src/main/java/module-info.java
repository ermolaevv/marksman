module com.example.marksman {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.logging;
    requires java.sql;
    requires java.naming;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.postgresql.jdbc;

    exports com.example.marksman.model;
    opens com.example.marksman.model to javafx.fxml, org.hibernate.orm.core;
    exports com.example.marksman.server;
    opens com.example.marksman.server to javafx.fxml;
    exports com.example.marksman.client;
    opens com.example.marksman.client to javafx.fxml;
    exports com.example.marksman.service;
    opens com.example.marksman.service to org.hibernate.orm.core;
}