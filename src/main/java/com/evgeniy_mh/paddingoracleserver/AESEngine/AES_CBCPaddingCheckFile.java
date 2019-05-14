package com.evgeniy_mh.paddingoracleserver.AESEngine;

import com.evgeniy_mh.paddingoracleserver.CommonUtils;
import com.evgeniy_mh.paddingoracleserver.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AES_CBCPaddingCheckFile implements Callable<Boolean> {

  private final AES mAES;
  private final File in;
  private final File out;
  private final byte[] key;

  public AES_CBCPaddingCheckFile(File in, File out, byte[] key) {
    mAES = new AES();
    this.in = in;
    this.out = out;
    this.key = key;
  }

  @Override
  public Boolean call() throws Exception {
    //Считывание вектора инициализации из входного файла
    byte[] IV = FileUtils.readBytesFromFile(in, AES.BLOCK_SIZE);

    byte[] tempKey = key;
    //Если длина ключа не кратна длине блока(16 байт), то он дополняется  PKCS7
    if (key.length % AES.BLOCK_SIZE != 0) {
      tempKey = PKCS7.PKCS7(key);
    }
    //mAES - объект класса AES. В него передается ключ, его длина и направление шифрования.
    mAES.makeKey(tempKey, 128, AES.DIR_BOTH);
    boolean error = false;
    try {
      //Открытие файла для записи результата дешифрования
      RandomAccessFile OUTraf = new RandomAccessFile(out, "rw");
      //Установка длины файла
      OUTraf.setLength(in.length() - IV.length);
      //Открытие зашифрованного файла
      RandomAccessFile INraf = new RandomAccessFile(in, "r");

      //Количество блоков открытого текста
      int nBlocks = CommonUtils.countBlocks(in, AES.BLOCK_SIZE);
      //Количество байт, которые будут удалены с конца файла
      int nToDeleteBytes = 0;

      //В буфер  temp будут считываться блоки по 16 байт из зашифрованного файла
      byte[] temp = new byte[AES.BLOCK_SIZE];
      //В буфере prev хранится предыдущий блок зашифрованного сообщения
      byte[] prev = new byte[AES.BLOCK_SIZE];
      //Главный цикл, в котором происходит обработка блоков
      for (int i = 1; i < nBlocks; i++) {
        //Установка указателя для считывания файла
        INraf.seek(i * 16);
        //Считывание блока в temp
        INraf.read(temp, 0, AES.BLOCK_SIZE);

        byte[] k = new byte[AES.BLOCK_SIZE]; // k_i
        byte[] c = new byte[AES.BLOCK_SIZE]; //c_i

        //k_i=Dk(c_i)
        mAES.decrypt(temp, k);

        if (i == 1) { //первая итерация
          for (int j = 0; j < AES.BLOCK_SIZE; j++) {
            //p_1=(IV XOR Dk(k_1))
            c[j] = (byte) (IV[j] ^ k[j]);
          }
          System.arraycopy(temp, 0, prev, 0, AES.BLOCK_SIZE);
        } else {
          for (int j = 0; j < AES.BLOCK_SIZE; j++) {
            c[j] = (byte) (prev[j] ^ k[j]);
          }
        }
        System.arraycopy(temp, 0, prev, 0, AES.BLOCK_SIZE);
        OUTraf.write(c);

        //Если это последняя итерация
        if ((i + 1) == nBlocks) {
          //Проверка дополнения
          //Считывание значения последнего байта из блока
          byte paddingCount = c[AES.BLOCK_SIZE - 1];

          //Проверка дополнения на соответствие стандарту PKCS7
          if (paddingCount > 0 && paddingCount <= 16) {
            for (int p = 0; p < paddingCount; p++) {
              if (c[AES.BLOCK_SIZE - 1 - p] != paddingCount) {
                //Если не все байты дополнения одинаковы, тогда ошибка
                error = true;
                break;
              }
            }
          } else {
            error = true;
          }
          //Если дополнение корректно, то в переменную nToDeleteBytes записывается количество байт дополнения
          if (!error) {
            nToDeleteBytes = c[AES.BLOCK_SIZE - 1];
          }
        }
      }
      //Удаление дополнения из файла результата
      OUTraf.setLength(OUTraf.length() - nToDeleteBytes);
      //Закрытие файловых потоков
      OUTraf.close();
      INraf.close();
    } catch (IOException e) {
      Logger.getLogger(AES_CBCPaddingCheckFile.class.getName()).log(Level.SEVERE, null, e);
      CommonUtils.reportExceptionToMainThread(e, "Exception in decrypt thread!");
    }
    //Возвращение информации о корректности дополнения
    return !error;
  }
}
