package com.evgeniy_mh.paddingoracleserver.AESEngine;

import com.evgeniy_mh.paddingoracleserver.CommonUtils;
import java.util.concurrent.Callable;

public class AES_CBCPaddingCheckBytes implements Callable<Boolean> {

  private final AES mAES;
  private final byte[] in;
  private final byte[] key;

  public AES_CBCPaddingCheckBytes(byte[] in, byte[] key) {
    mAES = new AES();
    this.in = in;
    this.key = key;
  }

  @Override
  public Boolean call() throws Exception {
    byte[] IV = new byte[AES.BLOCK_SIZE];
    System.arraycopy(in, 0, IV, 0, AES.BLOCK_SIZE);

    byte[] tempKey = key;
    if (key.length % AES.BLOCK_SIZE != 0) {
      tempKey = PKCS7.PKCS7(key);
    }
    mAES.makeKey(tempKey, 128, AES.DIR_BOTH);
    boolean error = false;

    int nBlocks = CommonUtils.countBlocks(in, AES.BLOCK_SIZE); //сколько блоков шифро текста

    byte[] temp = new byte[AES.BLOCK_SIZE];
    byte[] prev = new byte[AES.BLOCK_SIZE];

    for (int i = 1; i < nBlocks; i++) {
      System.arraycopy(in, i * 16, temp, 0, AES.BLOCK_SIZE);
      byte[] k = new byte[AES.BLOCK_SIZE]; // k_i
      byte[] c = new byte[AES.BLOCK_SIZE]; //c_i

      mAES.decrypt(temp, k);

      if (i == 1) { //первая итерация
        for (int j = 0; j < AES.BLOCK_SIZE; j++) {
          c[j] = (byte) (IV[j] ^ k[j]);
        }
        System.arraycopy(temp, 0, prev, 0, AES.BLOCK_SIZE);
      } else {
        for (int j = 0; j < AES.BLOCK_SIZE; j++) {
          c[j] = (byte) (prev[j] ^ k[j]);
        }
      }

      System.arraycopy(temp, 0, prev, 0, AES.BLOCK_SIZE);
      if ((i + 1) == nBlocks) {
        //проверка дополнения
        byte paddingCount = c[AES.BLOCK_SIZE - 1];

        if (paddingCount > 0 && paddingCount <= 16) {
          for (int p = 0; p < paddingCount; p++) {
            if (c[AES.BLOCK_SIZE - 1 - p] != paddingCount) {
              error = true;
              break;
            }
          }
        } else {
          error = true;
        }
      }
    }
    return !error;
  }
}
