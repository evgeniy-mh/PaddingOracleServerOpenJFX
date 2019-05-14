package com.evgeniy_mh.paddingoracleserver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FXMLController {

  private MainApp mainApp;
  private Stage stage;
  private ServerSocketProcessor processor = null;
  private FileChooser fileChooser = new FileChooser();
  private File secretKeyFile;

  AtomicLong requestCount = new AtomicLong(0);

  @FXML
  Button startServerButton;
  @FXML
  Button stopServerButton;
  @FXML
  TextField secretKeyTextField;
  @FXML
  Label serverStatusLabel;
  @FXML
  Label requestCountLabel;
  @FXML
  Button openSecretKeyFile;

  public void setMainApp(MainApp mainApp) {
    this.mainApp = mainApp;
  }

  public void initialize() {
    stopServerButton.setDisable(true);

    startServerButton.setOnAction(event -> {
      byte[] key = getSecretKey();
      if (key == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка ключа шифрования AES");
        alert.setHeaderText("Вы не ввели ключ или ключ больше 128 бит.");
        alert.showAndWait();
      } else {
        processor = new ServerSocketProcessor(key, requestCount);
        Thread server = new Thread(processor);
        server.setDaemon(true);
        server.start();
        stopServerButton.setDisable(false);
        startServerButton.setDisable(true);
      }
    });

    stopServerButton.setOnAction(event -> {
      if (processor != null && processor.isRunning()) {
        processor.stop();
        startServerButton.setDisable(false);
        stopServerButton.setDisable(true);
      }

    });

    secretKeyTextField.setOnMouseClicked(event -> {
      if (secretKeyFile != null) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Использовать поле ввода ключа?");
        alert.setHeaderText("Вы желаете ввести ключ самостоятельно?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
          secretKeyFile = null;
          secretKeyTextField.clear();
          updateSecretKeyInfo();
        }
      }
    });

    try {
      fileChooser.setInitialDirectory(new File(
          MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
          .getParentFile());
    } catch (URISyntaxException ex) {
      Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
    }
    openSecretKeyFile.setOnAction(event -> {
      secretKeyFile = openFile("Выберите файл с ключом");
      updateSecretKeyInfo();
    });

    final LongProperty lastUpdate = new SimpleLongProperty();
    final long minUpdateInterval = 0;
    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (now - lastUpdate.get() > minUpdateInterval) {
          updateServerInfo();
          lastUpdate.set(now);
        }
      }
    };
    timer.start();
  }

  private byte[] getSecretKey() {
    if (secretKeyFile == null) {
      byte[] key = secretKeyTextField.getText().getBytes(StandardCharsets.UTF_8);
      if (key.length == 0 || key.length > 128) {
        return null;
      } else {
        return key;
      }
    } else {
      return readBytesFromFile(secretKeyFile, 0, 128);
    }
  }

  private void updateServerInfo() {
    if (processor != null && processor.isRunning()) {
      serverStatusLabel.setText("Запущен");
    } else {
      serverStatusLabel.setText("Остановлен");
    }
    requestCountLabel.setText(String.valueOf(requestCount.get()));
  }

  private void updateSecretKeyInfo() {
    if (secretKeyFile == null) {
      secretKeyTextField.setEditable(true);
    } else {
      secretKeyTextField.setText(secretKeyFile.getAbsolutePath());
      secretKeyTextField.setEditable(false);
    }
  }

  private File openFile(String dialogTitle) {
    fileChooser.setTitle(dialogTitle);
    File file = fileChooser.showOpenDialog(stage);
    return file;
  }

  /**
   * Считывание необходимого количества байт из файла
   *
   * @param f Файл для считывания
   * @param from Начальная позиция для считывания из файла(Номер байта)
   * @param to Конечная позиция для считывания из файла(Номер байта)
   * @return Массив байт которые были считаны из файла
   */
  public static byte[] readBytesFromFile(File f, int from, int to) {
    try {
      byte[] res;
      try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
        raf.seek(from);
        res = new byte[to - from];
        raf.read(res, 0, to - from);
      }
      return res;
    } catch (IOException ex) {
      CommonUtils.reportExceptionToMainThread(ex, "Exception in readBytesFromFile");
      return null;
    }
  }

  public static void showExceptionToUser(Throwable e, String message) {
    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
    errorAlert.setTitle("Exception!");
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    errorAlert.setContentText(message + "\n" + sw.toString());
    errorAlert.showAndWait();
  }
}
