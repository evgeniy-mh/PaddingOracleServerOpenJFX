package com.evgeniy_mh.paddingoracleserver.AESEngine;

public class PKCS7 {

  /**
   * Дополняет массив байт до размера кратного AES.BLOCK_SIZE по стандарту PKCS7
   *
   * @param b-массив байт для которого будет выполнено дополнение PKCS7
   * @return Дополненный массив байт
   */
  public static byte[] PKCS7(byte[] b) {
    int n = countDeltaBlocks(b); //сколько байт нужно добавить и какое у них будет значение
    if (n != 0) {
      byte[] bPadded = new byte[b.length + n];
      for (int i = 0; i < bPadded.length; i++) {
        if (i < b.length) {
          bPadded[i] = b[i];
        } else {
          bPadded[i] = (byte) n;
        }
      }
      return bPadded;
    } else {
      return b;
    }
  }

  /**
   * Подсчет скольких байт не хватает до полного блока
   *
   * @param b Массив байт
   */
  private static int countDeltaBlocks(byte[] b) {
    return AES.BLOCK_SIZE - b.length % AES.BLOCK_SIZE;
  }
}
