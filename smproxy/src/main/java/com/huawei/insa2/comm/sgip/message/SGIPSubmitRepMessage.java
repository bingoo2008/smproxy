// Decompiled by DJ v2.9.9.60 Copyright 2000 Atanas Neshkov  Date: 2005-7-16 19:21:01
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   SGIPSubmitRepMessage.java

package com.huawei.insa2.comm.sgip.message;

import java.math.BigInteger;

import com.huawei.insa2.comm.sgip.SGIPConstant;
import com.huawei.insa2.util.TypeConvert;

// Referenced classes of package com.huawei.insa2.comm.sgip.message:
//            SGIPMessage

public class SGIPSubmitRepMessage extends SGIPMessage {

	public SGIPSubmitRepMessage(byte buf[]) throws IllegalArgumentException {
		super.buf = new byte[21];
		if (buf.length != 21) {
			throw new IllegalArgumentException(SGIPConstant.SMC_MESSAGE_ERROR);
		} else {
			System.arraycopy(buf, 0, super.buf, 0, 21);
			super.src_node_Id = TypeConvert.byte2int(super.buf, 0);
			super.time_Stamp = TypeConvert.byte2int(super.buf, 4);
			super.sequence_Id = TypeConvert.byte2int(super.buf, 8);
			return;
		}
	}

	private String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv + " ");
		}
		return stringBuilder.toString();
	}

	public String byte210(byte[] bytes) {
		return new BigInteger(1, bytes).toString(10);
	}

	public String getSubmitSequenceNumber() {
		byte temp[] = new byte[12];
		System.arraycopy(super.buf, 0, temp, 0, 12);
		return byte210(temp);
	}

	public int getResult() {
		int tmpId = super.buf[12];
		return tmpId;
	}

	public String toString() {
		String tmpStr = "SGIP_SUBMIT_REP: ";
		tmpStr = String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(tmpStr))))
				.append("Sequence_Id=").append(getSequenceId())));
		tmpStr = String.valueOf(String.valueOf(
				(new StringBuffer(String.valueOf(String.valueOf(tmpStr)))).append(",Result=").append(getResult())));
		tmpStr = String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(tmpStr))))
				.append(",data =").append(bytesToHexString(this.buf))));
		return tmpStr;
	}

	public int getCommandId() {
		return 0x80000003;
	}
}