package com.evgeniy_mh.paddingoracleserver;

import java.io.File;
import javafx.application.Platform;

public class CommonUtils {

  /**
   * Подсчет количества целых блоков
   *
   * @param f Файл с данными
   * @param blockSize Размер блока
   * @return Количество блоков
   */
  public static int countBlocks(File f, int blockSize) {
    return (int) (f.length() / blockSize);
  }

  /**
   * Подсчет количества целых блоков
   *
   * @param b Массив с данными
   * @param blockSize Размер блока
   * @return Количество блоков
   */
  public static int countBlocks(byte[] b, int blockSize) {
    return (int) (b.length / blockSize);
  }

  /**
   * Отправка сообщения о исключении в Application Thread
   *
   * @param message Дополнительное сообщение для пользователя
   */
  public static void reportExceptionToMainThread(final Throwable t, final String message) {
    Platform.runLater(() -> {
      FXMLController.showExceptionToUser(t, message);
    });
  }
}
