package com.evgeniy_mh.paddingoracleserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtils {

  public static void saveFile(File file, byte[] fileBytes) {
    if (file != null && fileBytes != null) {
      try {
        try (FileOutputStream fos = new FileOutputStream(file)) {
          fos.write(fileBytes);
        }
      } catch (IOException ex) {
        CommonUtils.reportExceptionToMainThread(ex, "Exception in saveFile");
        Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  public static byte[] readBytesFromFile(File file, int bytesToRead) {
    return readBytesFromFile(file, 0, bytesToRead);
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
      Logger.getLogger(MainApp.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }
}
