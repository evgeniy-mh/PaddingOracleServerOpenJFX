package com.evgeniy_mh.paddingoracleserver;

import com.evgeniy_mh.paddingoracleserver.AESEngine.AES_CBCPaddingCheckBytes;
import com.evgeniy_mh.paddingoracleserver.AESEngine.AES_CBCPaddingCheckFile;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSocketProcessor implements Runnable {

  private final int PADDING_OK_RESPONSE = 200;
  private final int PADDING_ERROR_RESPONSE = 500;

  private final Socket mClientSocket;
  private final byte[] mKey;

  ClientSocketProcessor(Socket clientSocket, byte[] key) {
    mClientSocket = clientSocket;
    mKey = key;
  }

  @Override
  public void run() {
    try {
      //Получение потока ввода для сокета клиента
      InputStream sin = mClientSocket.getInputStream();
      //Получение потока вывода для сокета клиента
      OutputStream sout = mClientSocket.getOutputStream();

      //Создание обертки над потоком ввода для упрощения получение данных
      DataInputStream in = new DataInputStream(sin);
      //Создание обертки над потоком вывода для упрощения отправки данных
      DataOutputStream out = new DataOutputStream(sout);

      //Считывание первой команды от клиента
      String line = in.readUTF();

      //Если оракул получает комманду на проверку нового файла
      if (line.equals("new file")) {
        //Считывание размера файла
        long fileSize = in.readLong();

        boolean isPaddingCorrect = false;
        //Проверка размера файла
        if (fileSize > 50000) {
          //Если размер файла больше 50000 байт, то файл сохраняется на диск
          //Получение пути для сохранения файла
          File pathnameParentDir = new File(
              MainApp.class.getProtectionDomain().getCodeSource().getLocation().getPath())
              .getParentFile();
          //Создание временного файла
          File tempSavedFile = File.createTempFile("tempSaved", null, pathnameParentDir);

          //Получение файла от клиента
          try (FileOutputStream fos = new FileOutputStream(tempSavedFile)) {
            int t;
            for (int i = 0; i < fileSize; i++) {
              //Получение следующего байта от клиента
              t = sin.read();
              //Запись байта в файл на диске
              fos.write(t);
            }
          }
          //Запуск процедуры проверки дополнения с сохранением результата в переменной
          isPaddingCorrect = checkPadding(tempSavedFile, mKey);
          //Удаление временного файла с диска
          tempSavedFile.delete();
        } else {
          //Иначе, если размер файла меньше 50000 байт, то он хранится в памяти
          //Объявление буфера для хранения файла
          byte[] tempFile = new byte[(int) fileSize];
          //Получение всех байт файла от пользователя
          sin.read(tempFile, 0, (int) fileSize);
          //Запуск процедуры проверки дополнения с сохранением результата в переменной
          isPaddingCorrect = checkPadding(tempFile, mKey);
        }

        if (isPaddingCorrect) {
          //Если дополнение корректно, то клиенту отправляется код 200
          out.writeInt(PADDING_OK_RESPONSE);
        } else {
          //Если дополнение не корректно, то клиенту отправляется код 500
          out.writeInt(PADDING_ERROR_RESPONSE);
        }

        //Закрытие всех потокод ввода и вывода
        out.flush();
        out.close();
        in.close();
        sout.close();
        sin.close();
        //Закрытие сокета клиента
        mClientSocket.close();
      }
    } catch (IOException ex) {
      Logger.getLogger(ServerSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private boolean checkPadding(File file, byte[] key) throws IOException {
    File pathnameParentDir = new File(
        MainApp.class.getProtectionDomain().getCodeSource().getLocation().getPath())
        .getParentFile();
    File tempDecryptedFile = File.createTempFile("tempDecSaved", null, pathnameParentDir);

    Callable c = new AES_CBCPaddingCheckFile(file, tempDecryptedFile, key);
    FutureTask<Boolean> ftask = new FutureTask<>(c);
    Thread thread = new Thread(ftask);
    thread.start();

    boolean isPaddingCorrect = false;
    try {
      isPaddingCorrect = ftask.get();

    } catch (InterruptedException | ExecutionException ex) {
      Logger.getLogger(ServerSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
    }
    tempDecryptedFile.delete();
    return isPaddingCorrect;
  }

  private boolean checkPadding(byte[] file, byte[] key) throws IOException {
    Callable c = new AES_CBCPaddingCheckBytes(file, key);
    FutureTask<Boolean> ftask = new FutureTask<>(c);
    Thread thread = new Thread(ftask);
    thread.start();

    boolean isPaddingCorrect = false;
    try {
      isPaddingCorrect = ftask.get();
    } catch (InterruptedException | ExecutionException ex) {
      Logger.getLogger(ServerSocketProcessor.class.getName()).log(Level.SEVERE, null, ex);
    }

    return isPaddingCorrect;
  }

}
