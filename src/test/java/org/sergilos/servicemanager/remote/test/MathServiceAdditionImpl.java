package org.sergilos.servicemanager.remote.test;

import org.apache.thrift.TException;

public class MathServiceAdditionImpl implements MathTestServiceAddition.Iface {

	@Override
	public int testingSum(int val1, int val2) throws TException {
		return val1 + val2;
	}
}
