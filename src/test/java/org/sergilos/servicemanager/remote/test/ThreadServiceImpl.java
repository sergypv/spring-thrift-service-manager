package org.sergilos.servicemanager.remote.test;

import org.apache.thrift.TException;

public class ThreadServiceImpl implements ThreadTestService.Iface {
	@Override
	public void testingWait(int time) throws TException {
		try {
			this.wait(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
