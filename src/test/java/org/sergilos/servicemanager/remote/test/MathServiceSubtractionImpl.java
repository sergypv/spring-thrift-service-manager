package org.sergilos.servicemanager.remote.test;

import org.apache.thrift.TException;

public class MathServiceSubtractionImpl implements MathTestServiceSubtraction.Iface {
	@Override
	public int testingSubtract(int val1, int val2) throws TException {
		return val1 - val2;
	}
}
