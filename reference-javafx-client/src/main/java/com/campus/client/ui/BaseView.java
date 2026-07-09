package com.campus.client.ui;

import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

public abstract class BaseView extends VBox {
    public abstract void onShow();

    public abstract void onHide();

    public void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}